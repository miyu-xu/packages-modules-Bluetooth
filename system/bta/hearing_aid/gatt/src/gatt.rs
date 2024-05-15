use std::pin::pin;

use async_stream::stream;
use futures::stream::{Stream, StreamExt};

use crate::att;
use crate::uuid::Uuid;

// See Vol 3, Part G - 3.4 Summary of GATT Profile attribute types
pub const PRIMARY_SERVICE: att::AttributeType16 = att::AttributeType16(0x2800);
pub const SECONDARY_SERVICE: att::AttributeType16 = att::AttributeType16(0x2801);
pub const INCLUDE: att::AttributeType16 = att::AttributeType16(0x2802);
pub const CHARACTERISTIC: att::AttributeType16 = att::AttributeType16(0x2803);
pub const CHARACTERISTIC_EXTENDED_PROPERTIES: att::AttributeType16 = att::AttributeType16(0x2900);
pub const CHARACTERISTIC_USER_DESCRIPTION: att::AttributeType16 = att::AttributeType16(0x2901);
pub const CLIENT_CHARACTERISTIC_CONFIGURATION: att::AttributeType16 = att::AttributeType16(0x2902);
pub const SERVER_CHARACTERISTIC_CONFIGURATION: att::AttributeType16 = att::AttributeType16(0x2903);
pub const CHARACTERISTIC_PRESENTATION_FORMAT: att::AttributeType16 = att::AttributeType16(0x2904);
pub const CHARACTERISTIC_AGGREGATE_FORMAT: att::AttributeType16 = att::AttributeType16(0x2905);

#[derive(Debug, PartialEq, Clone)]
pub struct Service {
    pub uuid: Uuid,
    pub attribute_handle: att::AttributeHandle,
    pub end_group_handle: att::AttributeHandle,
}

#[derive(Debug, PartialEq, Clone)]
pub struct Include {
    pub attribute_handle: att::AttributeHandle,
    pub service: Service,
}

#[derive(Debug, PartialEq, Clone)]
pub struct Characteristic {
    pub uuid: Uuid,
    pub attribute_handle: att::AttributeHandle,
    pub properties: u8,
    pub value_handle: att::AttributeHandle,
    pub end_handle: att::AttributeHandle,
}

#[derive(Debug, PartialEq, Clone)]
pub struct Descriptor {
    pub uuid: Uuid,
    pub attribute_handle: att::AttributeHandle,
}

pub struct Client {
    #[cfg(test)]
    pub bearer: att::Bearer,
    #[cfg(not(test))]
    bearer: att::Bearer,
    mtu: u16,
}

impl Client {
    pub fn new(bearer: att::Bearer) -> Self {
        Self { bearer, mtu: 23 }
    }

    /// See Vol 3, Part G - 4.3.1 Exchange MTU
    pub async fn exchange_mtu(&mut self, rx_mtu: u16) {
        // TODO: enforce: This subprocedure shall only be initiated once during a connection.
        let result = self.bearer.transaction(att::ExchangeMtuReq { client_rx_mtu: rx_mtu }).await;

        match result {
            Ok(att::ExchangeMtuRsp { server_rx_mtu }) => {
                // Once the messages have been exchanged, the ATT_MTU shall be
                // set to the minimum of the Client Rx MTU and Server Rx MTU
                // values.
                self.mtu = std::cmp::min(server_rx_mtu, rx_mtu);
            }
            Err(att::ErrorRsp { error_code, .. }) => {
                log::error!("Exchange MTU failed with {:?}", error_code);
            }
        }
    }

    /// See Vol 3, Part G - 4.4.1 Discover All Primary Services
    pub fn discover_all_primary_services(&mut self) -> impl Stream<Item = Service> + '_ {
        stream!({
            let mut starting_handle = att::AttributeHandle::MIN;

            loop {
                let result = self
                    .bearer
                    .transaction(att::ReadByGroupTypeReq {
                        starting_handle,
                        ending_handle: att::AttributeHandle::MAX,
                        attribute_group_type: PRIMARY_SERVICE.into(),
                    })
                    .await;

                starting_handle = match result {
                    Ok(att::ReadByGroupTypeRsp { attribute_data_list }) => {
                        for attribute_data in attribute_data_list.iter() {
                            yield Service {
                                // TODO
                                uuid: Uuid::uuid16(42),
                                attribute_handle: attribute_data.attribute_handle,
                                end_group_handle: attribute_data.end_group_handle,
                            };
                        }

                        // The ATT_READ_BY_GROUP_TYPE_REQ PDU shall be issued
                        // again with the Starting Handle set to one greater
                        // than the last End Group Handle in the
                        // ATT_READ_BY_GROUP_TYPE_RSP PDU.

                        // This sub-procedure is complete when the (...) or when
                        // the End Group Handle in the Read by Type Group Response
                        // is 0xFFFF.

                        // checked_add will return None if the end_group_handle is
                        // 0xFFFF (max value).
                        let Some(last) = attribute_data_list.last() else {
                            break;
                        };

                        let Some(next_handle) = last.end_group_handle.checked_add(1) else {
                            break;
                        };

                        next_handle
                    }
                    Err(_) => {
                        // TODO: Handle errors
                        break;
                    }
                }
            }
        })
    }

    /// See Vol 3, Part G - 4.4.2 Discover Primary Service by Service UUID
    pub fn discover_primary_service_by_service_uuid(
        &mut self,
        uuid: Uuid,
    ) -> impl Stream<Item = Service> + '_ {
        stream!({
            let mut starting_handle = att::AttributeHandle::MIN;

            loop {
                let result = self
                    .bearer
                    .transaction(att::FindByTypeValueReq {
                        starting_handle,
                        ending_handle: att::AttributeHandle::MAX,
                        attribute_type: PRIMARY_SERVICE.to_le_bytes(),
                        attribute_value: uuid.to_le_bytes().to_vec(),
                    })
                    .await;

                starting_handle = match result {
                    Ok(att::FindByTypeValueRsp { handles_information_list }) => {
                        for handles_information in handles_information_list.iter() {
                            yield Service {
                                uuid,
                                attribute_handle: handles_information.found_attribute_handle,
                                end_group_handle: handles_information.group_end_handle,
                            };
                        }
                        // the ATT_FIND_BY_TYPE_VALUE_REQ PDU may be issued
                        // again with the Starting Handle set to one greater
                        // than the last Attribute Handle range in the
                        // ATT_FIND_BY_TYPE_VALUE_RSP PDU.

                        // This sub-procedure is complete when the (...) or when
                        // the End Group Handle in the ATT_FIND_BY_TYPE_VALUE_RSP
                        // is 0xFFFF.

                        // checked_add will return None if the end_group_handle is
                        // 0xFFFF (max value).
                        let Some(last) = handles_information_list.last() else {
                            break;
                        };

                        let Some(next_handle) = last.group_end_handle.checked_add(1) else {
                            break;
                        };

                        next_handle
                    }
                    Err(_) => {
                        // TODO: Handle errors
                        break;
                    }
                }
            }
        })
    }

    /// See Vol 3, Part G - 4.5.1 Find Included Services
    pub fn find_included_services(
        &mut self,
        Service { attribute_handle, end_group_handle, .. }: Service,
    ) -> impl Stream<Item = Service> + '_ {
        futures::stream::empty()
    }

    /// See Vol 3, Part G - 4.6.1 Discover All Characteristics of a Service
    pub fn discover_all_characteristics_of_a_service(
        &mut self,
        Service { attribute_handle, end_group_handle, .. }: Service,
    ) -> impl Stream<Item = Characteristic> + '_ {
        let characteristics = stream!({
            let mut starting_handle = attribute_handle;
            let ending_handle = end_group_handle;

            loop {
                let result = self
                    .bearer
                    .transaction(att::ReadByTypeReq {
                        starting_handle,
                        ending_handle,
                        attribute_type: CHARACTERISTIC.into(),
                    })
                    .await;

                starting_handle = match result {
                    Ok(att::ReadByTypeRsp { attribute_data_list }) => {
                        for attribute_data in attribute_data_list.iter() {
                            // TODO: uuid, value_handle
                            yield (
                                Uuid::uuid16(42),
                                attribute_data.attribute_handle,
                                attribute_data.attribute_handle,
                            );
                        }

                        // This sub-procedure is complete when the (...) or the
                        // ATT_READ_BY_TYPE_RSP PDU has an Attribute Handle that is
                        // equal to the Ending Handle of the request.

                        let Some(last) = attribute_data_list.last() else {
                            break;
                        };

                        if last.attribute_handle >= ending_handle {
                            break;
                        } else {
                            // The ATT_READ_BY_TYPE_REQ PDU shall be issued again
                            // with the Starting Handle set to one greater than the last
                            // Attribute Handle in the ATT_READ_BY_TYPE_RSP PDU.

                            let Some(next_handle) = last.attribute_handle.checked_add(1) else {
                                break;
                            };

                            next_handle
                        }
                    }
                    Err(_) => {
                        // TODO: Handle errors
                        break;
                    }
                }
            }
        });

        stream!({
            let mut characteristics = pin!(characteristics.peekable());
            while let Some((uuid, attribute_handle, value_handle)) = characteristics.next().await {
                yield Characteristic {
                    uuid,
                    attribute_handle,
                    properties: 0,
                    value_handle,
                    end_handle: characteristics
                        .as_mut()
                        .peek()
                        .await
                        .map(|(_, attribute_handle, _)| {
                            attribute_handle.checked_sub(1).unwrap_or(att::AttributeHandle::MIN)
                        })
                        .unwrap_or(end_group_handle),
                };
            }
        })
    }

    /// See Vol 3, Part G - 4.7.1 Discover All Characteristic Descriptors
    pub fn discover_all_characteristic_descriptors(
        &mut self,
        Characteristic { value_handle, end_handle: ending_handle, .. }: Characteristic,
    ) -> impl Stream<Item = Descriptor> + '_ {
        stream!({
            // The ATT_FIND_INFORMATION_REQ PDU shall be used with the Starting
            // Handle set to the handle of the specified characteristic value + 1 and the
            // Ending Handle set to the ending handle of the specified characteristic.

            let Some(mut starting_handle) = value_handle.checked_add(1) else {
                return;
            };

            loop {
                let result = self
                    .bearer
                    .transaction(att::FindInformationReq { starting_handle, ending_handle })
                    .await;

                starting_handle = match result {
                    Ok(res) => {
                        let last_handle = match res {
                            att::FindInformationRes::FindInformationRes16 { information_data } => {
                                for data in information_data.iter() {
                                    yield Descriptor {
                                        attribute_handle: data.attribute_handle,
                                        uuid: Uuid::uuid16(data.uuid),
                                    }
                                }
                                information_data.last().map(|data| data.attribute_handle)
                            }
                            att::FindInformationRes::FindInformationRes128 { information_data } => {
                                for data in information_data.iter() {
                                    yield Descriptor {
                                        attribute_handle: data.attribute_handle,
                                        uuid: Uuid::uuid128(data.uuid),
                                    }
                                }
                                information_data.last().map(|data| data.attribute_handle)
                            }
                        };

                        let Some(last_handle) = last_handle else {
                            break;
                        };

                        // This sub-procedure is complete when the (...) or the
                        // ATT_FIND_INFORMATION_RSP PDU has an Attribute Handle that is
                        // equal to the Ending Handle of the request.

                        if last_handle >= ending_handle {
                            break;
                        } else {
                            // The ATT_FIND_INFORMATION_REQ PDU shall be issued again
                            // with the Starting Handle set to one greater than the last
                            // Attribute Handle in the ATT_FIND_INFORMATION_RSP PDU.

                            let Some(next_handle) = last_handle.checked_add(1) else {
                                break;
                            };

                            next_handle
                        }
                    }
                    Err(_) => {
                        // TODO: Handle errors
                        break;
                    }
                }
            }
        })
    }
}

#[cfg(test)]
pub mod tests {
    use std::pin::pin;

    use futures::StreamExt;

    use crate::att;
    use crate::uuid::Uuid;

    // See Vol 3, Part G - 4.3.1 Exchange MTU, Figure 4.1
    #[futures_test::test]
    async fn exchange_mtu() {
        let bearer =
            att::Bearer::new_for_test().receive_transaction(|request: att::ExchangeMtuReq| {
                assert_eq!(request, att::ExchangeMtuReq { client_rx_mtu: 0x0200 });
                Ok(att::ExchangeMtuRsp { server_rx_mtu: 0x0032 })
            });

        let mut gatt = super::Client::new(bearer);

        gatt.exchange_mtu(0x200).await;

        assert_eq!(gatt.mtu, 0x0032);
    }

    pub fn new_discover_all_primary_services_bearer() -> att::Bearer {
        att::Bearer::new_for_test()
            .receive_transaction(|request: att::ReadByGroupTypeReq| {
                assert_eq!(
                    request,
                    att::ReadByGroupTypeReq {
                        starting_handle: att::AttributeHandle::new(0x01).unwrap(),
                        ending_handle: att::AttributeHandle::new(0xFFFF).unwrap(),
                        attribute_group_type: super::PRIMARY_SERVICE.into(),
                    }
                );
                Ok(att::ReadByGroupTypeRsp {
                    attribute_data_list: [
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0001).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x000F).unwrap(),
                            attribute_value: Default::default(),
                        },
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0010).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x0017).unwrap(),
                            attribute_value: Default::default(),
                        },
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0100).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x01FF).unwrap(),
                            attribute_value: Default::default(),
                        },
                    ]
                    .to_vec(),
                })
            })
            .receive_transaction(|request: att::ReadByGroupTypeReq| {
                assert_eq!(
                    request,
                    att::ReadByGroupTypeReq {
                        starting_handle: att::AttributeHandle::new(0x0200).unwrap(),
                        ending_handle: att::AttributeHandle::new(0xFFFF).unwrap(),
                        attribute_group_type: super::PRIMARY_SERVICE.into(),
                    }
                );
                Ok(att::ReadByGroupTypeRsp {
                    attribute_data_list: [
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x02CF).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x02FF).unwrap(),
                            attribute_value: Default::default(),
                        },
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0300).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x03FF).unwrap(),
                            attribute_value: Default::default(),
                        },
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0400).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x04FF).unwrap(),
                            attribute_value: Default::default(),
                        },
                    ]
                    .to_vec(),
                })
            })
            .receive_transaction(|request: att::ReadByGroupTypeReq| {
                assert_eq!(
                    request,
                    att::ReadByGroupTypeReq {
                        starting_handle: att::AttributeHandle::new(0x0500).unwrap(),
                        ending_handle: att::AttributeHandle::new(0xFFFF).unwrap(),
                        attribute_group_type: super::PRIMARY_SERVICE.into(),
                    }
                );
                Err(att::ErrorRsp {
                    attribute_handle_in_error: att::AttributeHandle::new(0x0500).unwrap(),
                    error_code: att::ErrorCode::AttributeNotFound,
                })
            })
    }

    // See Vol 3, Part G - 4.4.1 Discover All Primary Services, Figure 4.2
    #[futures_test::test]
    async fn discover_all_primary_services() {
        let bearer = new_discover_all_primary_services_bearer();
        let mut gatt = super::Client::new(bearer);

        let mut discovery = pin!(gatt.discover_all_primary_services());

        assert_eq!(
            discovery.next().await,
            Some(super::Service {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x0001).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x000F).unwrap()
            })
        );
        assert_eq!(
            discovery.next().await,
            Some(super::Service {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x0010).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x0017).unwrap()
            })
        );
        assert_eq!(
            discovery.next().await,
            Some(super::Service {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x0100).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x01FF).unwrap()
            })
        );
        assert_eq!(
            discovery.next().await,
            Some(super::Service {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x02CF).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x02FF).unwrap()
            })
        );
        assert_eq!(
            discovery.next().await,
            Some(super::Service {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x0300).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x03FF).unwrap()
            })
        );
        assert_eq!(
            discovery.next().await,
            Some(super::Service {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x0400).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x04FF).unwrap()
            })
        );
        assert_eq!(discovery.next().await, None);
    }

    pub const UUID1: Uuid = Uuid::uuid16(0x42);
    pub fn new_discover_primary_service_by_service_uuid_bearer() -> att::Bearer {
        att::Bearer::new_for_test()
            .receive_transaction(|request: att::FindByTypeValueReq| {
                assert_eq!(
                    request,
                    att::FindByTypeValueReq {
                        starting_handle: att::AttributeHandle::new(0x01).unwrap(),
                        ending_handle: att::AttributeHandle::new(0xFFFF).unwrap(),
                        attribute_type: super::PRIMARY_SERVICE.to_le_bytes(),
                        attribute_value: UUID1.to_le_bytes().to_vec(),
                    }
                );
                Ok(att::FindByTypeValueRsp {
                    handles_information_list: [att::HandlesInformation {
                        found_attribute_handle: att::AttributeHandle::new(0x0200).unwrap(),
                        group_end_handle: att::AttributeHandle::new(0x0214).unwrap(),
                    }]
                    .to_vec(),
                })
            })
            .receive_transaction(|request: att::FindByTypeValueReq| {
                assert_eq!(
                    request,
                    att::FindByTypeValueReq {
                        starting_handle: att::AttributeHandle::new(0x0215).unwrap(),
                        ending_handle: att::AttributeHandle::new(0xFFFF).unwrap(),
                        attribute_type: super::PRIMARY_SERVICE.to_le_bytes(),
                        attribute_value: UUID1.to_le_bytes().to_vec(),
                    }
                );
                Err(att::ErrorRsp {
                    attribute_handle_in_error: att::AttributeHandle::new(0x0215).unwrap(),
                    error_code: att::ErrorCode::AttributeNotFound,
                })
            })
    }

    // See Vol 3, Part G - 4.4.2 Discover Primary Service by Service UUID, Figure 4.3
    #[futures_test::test]
    async fn discover_primary_service_by_service_uuid() {
        let bearer = new_discover_primary_service_by_service_uuid_bearer();
        let mut gatt = super::Client::new(bearer);

        let mut discovery = pin!(gatt.discover_primary_service_by_service_uuid(UUID1));

        assert_eq!(
            discovery.next().await,
            Some(super::Service {
                uuid: UUID1,
                attribute_handle: att::AttributeHandle::new(0x0200).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x0214).unwrap(),
            })
        );
        assert_eq!(discovery.next().await, None);
    }

    // See Vol 3, Part G - 4.6.1 Discover All Characteristics of a Service, Figure 4.5
    #[futures_test::test]
    async fn discover_all_characteristics_of_a_service() {
        let bearer = att::Bearer::new_for_test()
            .receive_transaction(|request: att::ReadByTypeReq| {
                assert_eq!(
                    request,
                    att::ReadByTypeReq {
                        starting_handle: att::AttributeHandle::new(0x0200).unwrap(),
                        ending_handle: att::AttributeHandle::new(0x0214).unwrap(),
                        attribute_type: super::CHARACTERISTIC.into(),
                    }
                );
                Ok(att::ReadByTypeRsp {
                    attribute_data_list: [
                        att::AttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0203).unwrap(),
                            attribute_value: Default::default(),
                        },
                        att::AttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0210).unwrap(),
                            attribute_value: Default::default(),
                        },
                    ]
                    .to_vec(),
                })
            })
            .receive_transaction(|request: att::ReadByTypeReq| {
                assert_eq!(
                    request,
                    att::ReadByTypeReq {
                        starting_handle: att::AttributeHandle::new(0x0211).unwrap(),
                        ending_handle: att::AttributeHandle::new(0x0214).unwrap(),
                        attribute_type: super::CHARACTERISTIC.into(),
                    }
                );
                Err(att::ErrorRsp {
                    attribute_handle_in_error: att::AttributeHandle::new(0x0211).unwrap(),
                    error_code: att::ErrorCode::AttributeNotFound,
                })
            });

        let mut gatt = super::Client::new(bearer);
        let mut discovery = pin!(gatt.discover_all_characteristics_of_a_service(super::Service {
            uuid: Uuid::uuid16(42),
            attribute_handle: att::AttributeHandle::new(0x0200).unwrap(),
            end_group_handle: att::AttributeHandle::new(0x0214).unwrap(),
        }));

        assert_eq!(
            discovery.next().await,
            Some(super::Characteristic {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x0203).unwrap(),
                properties: 0,
                value_handle: att::AttributeHandle::new(0x0203).unwrap(),
                end_handle: att::AttributeHandle::new(0x020F).unwrap(),
            })
        );
        assert_eq!(
            discovery.next().await,
            Some(super::Characteristic {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x0210).unwrap(),
                properties: 0,
                value_handle: att::AttributeHandle::new(0x0210).unwrap(),
                end_handle: att::AttributeHandle::new(0x0214).unwrap(),
            })
        );
        assert_eq!(discovery.next().await, None);
    }

    // See Vol 3, Part G - 4.7.1 Discover All Characteristics Descriptors, Figure 4.7
    #[futures_test::test]
    async fn discover_all_characteristic_descriptors() {
        const UUID1: u128 = 0x42;
        const UUID2: u128 = 0x43;

        let bearer = att::Bearer::new_for_test()
            .receive_transaction(|request: att::FindInformationReq| {
                assert_eq!(
                    request,
                    att::FindInformationReq {
                        starting_handle: att::AttributeHandle::new(0x0205).unwrap(),
                        ending_handle: att::AttributeHandle::new(0x020F).unwrap(),
                    }
                );
                Ok(att::FindInformationRes::FindInformationRes128 {
                    information_data: Box::new([
                        att::InformationData128 {
                            attribute_handle: att::AttributeHandle::new(0x0205).unwrap(),
                            uuid: UUID1,
                        },
                        att::InformationData128 {
                            attribute_handle: att::AttributeHandle::new(0x0206).unwrap(),
                            uuid: UUID2,
                        },
                    ]),
                })
            })
            .receive_transaction(|request: att::FindInformationReq| {
                assert_eq!(
                    request,
                    att::FindInformationReq {
                        starting_handle: att::AttributeHandle::new(0x0207).unwrap(),
                        ending_handle: att::AttributeHandle::new(0x020F).unwrap(),
                    }
                );
                Err(att::ErrorRsp {
                    attribute_handle_in_error: att::AttributeHandle::new(0x0207).unwrap(),
                    error_code: att::ErrorCode::AttributeNotFound,
                })
            });

        let mut gatt = super::Client::new(bearer);
        let mut discovery =
            pin!(gatt.discover_all_characteristic_descriptors(super::Characteristic {
                uuid: Uuid::uuid16(42),
                attribute_handle: att::AttributeHandle::new(0x0203).unwrap(),
                properties: 0,
                value_handle: att::AttributeHandle::new(0x0204).unwrap(),
                end_handle: att::AttributeHandle::new(0x020F).unwrap(),
            }));

        assert_eq!(
            discovery.next().await,
            Some(super::Descriptor {
                uuid: Uuid::uuid128(UUID1),
                attribute_handle: att::AttributeHandle::new(0x0205).unwrap(),
            })
        );
        assert_eq!(
            discovery.next().await,
            Some(super::Descriptor {
                uuid: Uuid::uuid128(UUID2),
                attribute_handle: att::AttributeHandle::new(0x0206).unwrap(),
            })
        );
        assert_eq!(discovery.next().await, None);
    }
}
