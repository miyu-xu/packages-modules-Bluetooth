use std::future::ready;
use std::ops::{Deref, DerefMut};

use async_stream::stream;
use futures::stream::{Stream, StreamExt};

use super::database::{self, Database};
use super::gatt;
use super::uuid::Uuid;

struct Client {
    inner: gatt::Client,
    database: Database,
}

impl Client {
    fn new(inner: gatt::Client) -> Self {
        Self { inner, database: Database::empty() }
    }

    async fn exchange_mtu(&mut self, rx_mtu: u16) {
        self.inner.exchange_mtu(rx_mtu).await
    }

    fn discover_all_primary_services(&mut self) -> impl Stream<Item = gatt::Service> + '_ {
        self.database
            .primary_services
            .cache_complete_request(self.inner.discover_all_primary_services().map(Result::unwrap))
    }

    fn discover_primary_service_by_service_uuid(
        &mut self,
        uuid: Uuid,
    ) -> impl Stream<Item = gatt::Service> + '_ {
        let know_all_services = &mut self.database.know_all_services;
        // The cache for discover_primary_service_by_service_uuid is considered complete
        // if either:
        // - A complete discover_all_primary_services is cached (self.primary_services.is_complete).
        // - A complete discover_primary_service_by_service_uuid for this uuid is cached
        //   (know_all_services contains the searched uuid).
        let cache_is_complete =
            self.database.primary_services.is_complete || know_all_services.contains(&uuid);
        let mark_cache_complete = move |_| know_all_services.push(uuid);

        self.database
            .primary_services
            .cache_request(
                self.inner.discover_primary_service_by_service_uuid(uuid).map(Result::unwrap),
                cache_is_complete,
                mark_cache_complete,
            )
            .filter(move |service| ready(service.uuid == uuid))
    }

    fn discover_all_characteristics_of_a_service(
        &mut self,
        service: gatt::Service,
    ) -> impl Stream<Item = gatt::Characteristic> + '_ {
        let cached_service = self
            .database
            .primary_services
            .iter_mut()
            .find(|database::Service { inner, .. }| inner == &service)
            .expect("TODO: warn and do request");

        cached_service.characteristics.cache_complete_request(
            self.inner.discover_all_characteristics_of_a_service(service).map(Result::unwrap),
        )
    }

    fn discover_all_characteristic_descriptors(
        &mut self,
        characteristic: gatt::Characteristic,
    ) -> impl Stream<Item = gatt::Descriptor> + '_ {
        let cached_characteristic = self
            .database
            .primary_services
            .iter_mut()
            .flat_map(|service| service.characteristics.iter_mut())
            .find(|database::Characteristic { inner, .. }| inner == &characteristic)
            .expect("TODO: warn and do request");

        cached_characteristic.descriptors.cache_complete_request(
            self.inner.discover_all_characteristic_descriptors(characteristic).map(Result::unwrap),
        )
    }
}

pub struct Cache<T> {
    inner: Vec<T>,
    is_complete: bool,
}

impl<T> Cache<T> {
    pub fn empty() -> Self {
        Self { inner: vec![], is_complete: false }
    }
}

impl<T, const N: usize> From<[T; N]> for Cache<T> {
    fn from(value: [T; N]) -> Self {
        Self { inner: value.into(), is_complete: false }
    }
}

impl<T, U> Cache<U>
where
    T: Clone + PartialEq + 'static,
    U: From<T> + Deref<Target = T>,
{
    fn cache_request<'a>(
        &'a mut self,
        request: impl Stream<Item = T> + 'a,
        is_complete: bool,
        mark_complete: impl FnOnce(&'a mut Self) + 'a,
    ) -> impl Stream<Item = T> + 'a {
        stream!({
            // Return all cached entries.
            for value in self.iter() {
                yield value.deref().clone();
            }

            // If cache is not complete, return all and put in cache all
            // entries not already present.
            if !is_complete {
                for await value in request {
                    let in_cache = self.iter().any(|item| item.deref() == &value);

                    if !in_cache {
                        self.push(value.clone().into());
                        yield value;
                    }
                }
                mark_complete(self)
            }
        })
    }

    fn cache_complete_request<'a>(
        &'a mut self,
        request: impl Stream<Item = T> + 'a,
    ) -> impl Stream<Item = T> + 'a {
        self.cache_request(request, self.is_complete, |cache| cache.is_complete = true)
    }
}

impl<T> Deref for Cache<T> {
    type Target = Vec<T>;

    fn deref(&self) -> &Self::Target {
        &self.inner
    }
}

impl<T> DerefMut for Cache<T> {
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.inner
    }
}

#[cfg(test)]
mod tests {
    use std::pin::pin;

    use futures::stream::StreamExt;

    use super::gatt::tests as gatt_tests;

    #[futures_test::test]
    async fn complete_discover_all_primary_services_is_cached() {
        let gatt = super::gatt::Client::new(gatt_tests::new_discover_all_primary_services_bearer());
        let mut client = super::Client::new(gatt);

        let services = client.discover_all_primary_services().collect::<Vec<_>>().await;
        let complete = client.inner.bearer.is_complete();
        client.inner.bearer.reset();
        let cached_services = client.discover_all_primary_services().collect::<Vec<_>>().await;

        assert_eq!(services, cached_services);
        assert_eq!(client.inner.bearer.into_current_step(), 0, "Second discovery is cached");
        assert_eq!(complete, true);
    }

    #[futures_test::test]
    async fn incomplete_discover_all_primary_services_is_cached() {
        let gatt = super::gatt::Client::new(gatt_tests::new_discover_all_primary_services_bearer());
        let mut client = super::Client::new(gatt);

        let first_service = pin!(client.discover_all_primary_services()).next().await;
        let complete = client.inner.bearer.is_complete();
        client.inner.bearer.reset();
        let first_cached_service = pin!(client.discover_all_primary_services()).next().await;

        assert_eq!(first_service, first_cached_service);
        assert!(first_service.is_some());
        assert_eq!(client.inner.bearer.into_current_step(), 0, "Second discovery is cached");
        assert_eq!(complete, false);
    }

    #[futures_test::test]
    async fn incomplete_then_complete_discover_all_primary_services_is_not_cached() {
        let gatt = super::gatt::Client::new(gatt_tests::new_discover_all_primary_services_bearer());
        let mut client = super::Client::new(gatt);

        let first_service = pin!(client.discover_all_primary_services()).next().await.unwrap();
        client.inner.bearer.reset();
        let services = client.discover_all_primary_services().collect::<Vec<_>>().await;

        assert!(services.contains(&first_service));
        assert_eq!(client.inner.bearer.is_complete(), true);
    }

    #[futures_test::test]
    async fn complete_discover_primary_service_by_service_uuid_is_cached() {
        let gatt = super::gatt::Client::new(
            gatt_tests::new_discover_primary_service_by_service_uuid_bearer(),
        );
        let mut client = super::Client::new(gatt);

        let services = client
            .discover_primary_service_by_service_uuid(gatt_tests::UUID1)
            .collect::<Vec<_>>()
            .await;
        let complete = client.inner.bearer.is_complete();
        client.inner.bearer.reset();
        let cached_services = client
            .discover_primary_service_by_service_uuid(gatt_tests::UUID1)
            .collect::<Vec<_>>()
            .await;

        assert_eq!(services, cached_services);
        assert_eq!(client.inner.bearer.into_current_step(), 0, "Second discovery is cached");
        assert_eq!(complete, true);
    }
}
