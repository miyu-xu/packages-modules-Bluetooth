use std::pin::pin;

use async_stream::{stream, try_stream};
use futures::stream::{Stream, StreamExt};
use thiserror::Error;

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

#[derive(Debug, PartialEq)]
enum InternalDiscoveryError {
    AttError(att::ErrorRsp),
    InvalidValue(att::AttributeValue),
    ServiceAttributeNotFound(att::AttributeHandle),
    DecodeError(pdl_runtime::DecodeError),
    InvalidFindInformationRsp(att::FindInformationRsp),
}

#[derive(Error, Debug, PartialEq)]
pub enum DiscoveryError {
    /// Cached Database is out of sync and a new discovery
    /// should be issued.
    #[error("database out of sync")]
    DatabaseOutOfSync,
    /// Application error code defined by a higher layer specification.
    #[error("application error (code {0})")]
    ApplicationError(u8),
    /// Common Profile And Service Error Codes defined in
    /// Core Specification Supplement Part B
    #[error("common profile and service error (code {0})")]
    CommonProfileAndServiceError(u8),
    /// Internal GATT Error.
    /// This means that an invariant has been broken.
    /// This error is usually not recoverable.
    #[error("internal error (reason {0:?})")]
    #[allow(private_interfaces)]
    InternalError(InternalDiscoveryError),
}

impl From<InternalDiscoveryError> for DiscoveryError {
    fn from(value: InternalDiscoveryError) -> DiscoveryError {
        DiscoveryError::InternalError(value)
    }
}

enum RecoverableDiscoveryError {
    AttributeNotFound,
}

impl RecoverableDiscoveryError {
    fn try_from(value: att::ErrorRsp) -> Result<Self, DiscoveryError> {
        match value.error_code {
            att::ErrorCode::AttributeNotFound => Ok(Self::AttributeNotFound),
            att::ErrorCode::ApplicationError(error) => {
                Err(DiscoveryError::ApplicationError(*error))
            }
            att::ErrorCode::CommonProfileAndServiceErrorCodes(error) => {
                Err(DiscoveryError::CommonProfileAndServiceError(*error))
            }
            att::ErrorCode::DatabaseOutOfSync => Err(DiscoveryError::DatabaseOutOfSync),
            att::ErrorCode::InvalidHandle
            | att::ErrorCode::ReadNotPermitted
            | att::ErrorCode::WriteNotPermitted
            | att::ErrorCode::InvalidPdu
            | att::ErrorCode::InsufficientAuthentication
            | att::ErrorCode::RequestNotSupported
            | att::ErrorCode::InvalidOffset
            | att::ErrorCode::InsufficientAuthorization
            | att::ErrorCode::PrepareQueueFull
            | att::ErrorCode::AttributeNotLong
            | att::ErrorCode::EncryptionKeySizeTooShort
            | att::ErrorCode::InvalidAttributeValueLength
            | att::ErrorCode::UnlikelyError
            | att::ErrorCode::InsufficientEncryption
            | att::ErrorCode::UnsupportedGroupType
            | att::ErrorCode::InsufficientResources
            | att::ErrorCode::ValueNotAllowed => {
                Err(DiscoveryError::InternalError(InternalDiscoveryError::AttError(value)))
            }
        }
    }
}

fn att_read_service_to_discovery_error(err: att::ErrorRsp) -> DiscoveryError {
    let attribute_handle = err.attribute_handle_in_error;
    match RecoverableDiscoveryError::try_from(err) {
        Ok(RecoverableDiscoveryError::AttributeNotFound) => {
            InternalDiscoveryError::ServiceAttributeNotFound(attribute_handle).into()
        }
        Err(e) => e,
    }
}

pub struct Client {
    pub bearer: att::Bearer,
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
    pub fn discover_all_primary_services(
        &mut self,
    ) -> impl Stream<Item = Result<Service, DiscoveryError>> + '_ {
        try_stream!({
            let mut starting_handle = att::AttributeHandle::MIN;

            loop {
                let result = self
                    .bearer
                    .transaction(att::ReadByGroupTypeReq {
                        starting_handle,
                        ending_handle: att::AttributeHandle::MAX,
                        attribute_group_type: PRIMARY_SERVICE.into(),
                    })
                    .await
                    .map_err(RecoverableDiscoveryError::try_from);

                let attribute_data_list = match result {
                    Ok(att::ReadByGroupTypeRsp { attribute_data_list }) => attribute_data_list,
                    // This sub-procedure is complete when the ATT_ERROR_RSP PDU is received
                    // and the Error Code parameter is set to Attribute Not Found (0x0A)
                    Err(Ok(RecoverableDiscoveryError::AttributeNotFound)) => {
                        break;
                    }
                    Err(Err(e)) => Err(e)?,
                };

                for att::GroupAttributeData {
                    attribute_handle,
                    end_group_handle,
                    attribute_value,
                } in attribute_data_list.iter()
                {
                    let uuid = if let Ok(att::ServiceAttributeValue16 { uuid }) =
                        attribute_value.try_into()
                    {
                        uuid.into()
                    } else if let Ok(att::ServiceAttributeValue128 { uuid }) =
                        attribute_value.try_into()
                    {
                        uuid.into()
                    } else {
                        Err(InternalDiscoveryError::InvalidValue(attribute_value.clone()))?
                    };

                    yield Service {
                        uuid,
                        attribute_handle: *attribute_handle,
                        end_group_handle: *end_group_handle,
                    };
                }

                let Some(last) = attribute_data_list.last() else {
                    break;
                };

                // This sub-procedure is complete when the (...) or when
                // the End Group Handle in the Read by Type Group Response
                // is 0xFFFF.

                // checked_add will return None if the end_group_handle is
                // 0xFFFF (max value).
                let Some(next_handle) = last.end_group_handle.checked_add(1) else {
                    break;
                };

                // The ATT_READ_BY_GROUP_TYPE_REQ PDU shall be issued
                // again with the Starting Handle set to one greater
                // than the last End Group Handle in the
                // ATT_READ_BY_GROUP_TYPE_RSP PDU.

                starting_handle = next_handle;
            }
        })
    }

    /// See Vol 3, Part G - 4.4.2 Discover Primary Service by Service UUID
    pub fn discover_primary_service_by_service_uuid(
        &mut self,
        uuid: Uuid,
    ) -> impl Stream<Item = Result<Service, DiscoveryError>> + '_ {
        try_stream!({
            let mut starting_handle = att::AttributeHandle::MIN;

            loop {
                let result = self
                    .bearer
                    .transaction(att::FindByTypeValueReq {
                        starting_handle,
                        ending_handle: att::AttributeHandle::MAX,
                        attribute_type: PRIMARY_SERVICE.into(),
                        // TODO: modify pdl to remove the unwrap
                        attribute_value: att::ServiceAttributeValue { uuid: uuid.into() }
                            .try_into()
                            .unwrap(),
                    })
                    .await
                    .map_err(RecoverableDiscoveryError::try_from);

                let handles_information_list = match result {
                    Ok(att::FindByTypeValueRsp { handles_information_list }) => {
                        handles_information_list
                    }
                    // This sub-procedure is complete when the ATT_ERROR_RSP PDU is received
                    // and the Error Code parameter is set to Attribute Not Found (0x0A)
                    Err(Ok(RecoverableDiscoveryError::AttributeNotFound)) => {
                        break;
                    }
                    Err(Err(e)) => Err(e)?,
                };

                for handles_information in handles_information_list.iter() {
                    yield Service {
                        uuid,
                        attribute_handle: handles_information.found_attribute_handle,
                        end_group_handle: handles_information.group_end_handle,
                    };
                }

                let Some(last) = handles_information_list.last() else {
                    break;
                };

                // This sub-procedure is complete when the (...) or when
                // the End Group Handle in the ATT_FIND_BY_TYPE_VALUE_RSP
                // is 0xFFFF.

                // checked_add will return None if the end_group_handle is
                // 0xFFFF (max value).
                let Some(next_handle) = last.group_end_handle.checked_add(1) else {
                    break;
                };

                // the ATT_FIND_BY_TYPE_VALUE_REQ PDU may be issued
                // again with the Starting Handle set to one greater
                // than the last Attribute Handle range in the
                // ATT_FIND_BY_TYPE_VALUE_RSP PDU.

                starting_handle = next_handle;
            }
        })
    }

    /// See Vol 3, Part G - 4.5.1 Find Included Services
    pub fn find_included_services(
        &mut self,
        Service { attribute_handle, end_group_handle, .. }: Service,
    ) -> impl Stream<Item = Result<Include, DiscoveryError>> + '_ {
        try_stream!({
            let mut starting_handle = attribute_handle;
            let ending_handle = end_group_handle;

            loop {
                let result = self
                    .bearer
                    .transaction(att::ReadByTypeReq {
                        starting_handle,
                        ending_handle,
                        attribute_type: INCLUDE.into(),
                    })
                    .await
                    .map_err(RecoverableDiscoveryError::try_from);

                let attribute_data_list = match result {
                    Ok(att::ReadByTypeRsp { attribute_data_list }) => attribute_data_list,
                    // This sub-procedure is complete when the ATT_ERROR_RSP PDU is received
                    // and the Error Code parameter is set to Attribute Not Found (0x0A)
                    Err(Ok(RecoverableDiscoveryError::AttributeNotFound)) => {
                        break;
                    }
                    Err(Err(e)) => Err(e)?,
                };

                for att::AttributeData {
                    attribute_handle: include_attribute_handle,
                    attribute_value,
                } in attribute_data_list.iter()
                {
                    let service = if let Ok(att::IncludeAttributeValue16 {
                        attribute_handle,
                        end_group_handle,
                        uuid,
                    }) = attribute_value.try_into()
                    {
                        Service { attribute_handle, end_group_handle, uuid: uuid.into() }
                    } else if let Ok(att::IncludeAttributeValue128 {
                        attribute_handle,
                        end_group_handle,
                    }) = attribute_value.try_into()
                    {
                        // To get the included service UUID when the included service uses a 128-bit
                        // UUID, the ATT_READ_REQ PDU is used. The Attribute Handle for the
                        // ATT_READ_REQ PDU is the Attribute Handle of the included service.
                        let att::ReadRsp { ref attribute_value } = self
                            .bearer
                            .transaction(att::ReadReq { attribute_handle })
                            .await
                            .map_err(att_read_service_to_discovery_error)?;

                        let att::ServiceAttributeValue128 { uuid } = attribute_value
                            .try_into()
                            .map_err(InternalDiscoveryError::DecodeError)?;

                        Service { attribute_handle, end_group_handle, uuid: uuid.into() }
                    } else {
                        Err(InternalDiscoveryError::InvalidValue(attribute_value.clone()))?
                    };

                    yield Include { attribute_handle: *include_attribute_handle, service };
                }

                let Some(last) = attribute_data_list.last() else {
                    break;
                };

                // This sub-procedure is complete when the (...) or the
                // ATT_READ_BY_TYPE_RSP PDU has an Attribute Handle that is
                // equal to the Ending Handle of the request.

                if last.attribute_handle >= ending_handle {
                    break;
                }

                let Some(next_handle) = last.attribute_handle.checked_add(1) else {
                    break;
                };

                // The ATT_READ_BY_TYPE_REQ PDU shall be issued again
                // with the Starting Handle set to one greater than the last
                // Attribute Handle in the ATT_READ_BY_TYPE_RSP PDU.

                starting_handle = next_handle;
            }
        })
    }

    /// See Vol 3, Part G - 4.6.1 Discover All Characteristics of a Service
    pub fn discover_all_characteristics_of_a_service(
        &mut self,
        Service { attribute_handle, end_group_handle, .. }: Service,
    ) -> impl Stream<Item = Result<Characteristic, DiscoveryError>> + '_ {
        let characteristics = try_stream!({
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
                    .await
                    .map_err(RecoverableDiscoveryError::try_from);

                let attribute_data_list = match result {
                    Ok(att::ReadByTypeRsp { attribute_data_list }) => Ok(attribute_data_list),
                    // This sub-procedure is complete when the ATT_ERROR_RSP PDU is received
                    // and the Error Code parameter is set to Attribute Not Found (0x0A)
                    Err(Ok(RecoverableDiscoveryError::AttributeNotFound)) => {
                        break;
                    }
                    Err(Err(e)) => Err(e),
                }?;

                for att::AttributeData {
                    attribute_handle: characteristic_attribute_handle,
                    attribute_value,
                } in attribute_data_list.iter()
                {
                    if let Ok(att::CharacteristicAttributeValue16 {
                        properties,
                        value_attribute_handle,
                        uuid,
                    }) = attribute_value.try_into()
                    {
                        yield (
                            uuid.into(),
                            *characteristic_attribute_handle,
                            properties,
                            value_attribute_handle,
                        )
                    } else if let Ok(att::CharacteristicAttributeValue128 {
                        properties,
                        value_attribute_handle,
                        uuid,
                    }) = attribute_value.try_into()
                    {
                        yield (
                            uuid.into(),
                            *characteristic_attribute_handle,
                            properties,
                            value_attribute_handle,
                        )
                    } else {
                        Err(DiscoveryError::InternalError(InternalDiscoveryError::InvalidValue(
                            attribute_value.clone(),
                        )))?
                    }
                }

                let Some(last) = attribute_data_list.last() else {
                    break;
                };

                // This sub-procedure is complete when the (...) or the
                // ATT_READ_BY_TYPE_RSP PDU has an Attribute Handle that is
                // equal to the Ending Handle of the request.

                if last.attribute_handle >= ending_handle {
                    break;
                }

                let Some(next_handle) = last.attribute_handle.checked_add(1) else {
                    break;
                };

                // The ATT_READ_BY_TYPE_REQ PDU shall be issued again
                // with the Starting Handle set to one greater than the last
                // Attribute Handle in the ATT_READ_BY_TYPE_RSP PDU.

                starting_handle = next_handle
            }
        });

        stream!({
            let mut characteristics = pin!(characteristics.peekable());
            while let Some(characteristic) = characteristics.next().await {
                let end_handle = characteristics
                    .as_mut()
                    .peek()
                    .await
                    .map(Result::as_ref)
                    .and_then(Result::ok)
                    .map(|(_, attribute_handle, _, _)| {
                        attribute_handle.checked_sub(1).unwrap_or(att::AttributeHandle::MIN)
                    })
                    .unwrap_or(end_group_handle);

                yield characteristic.map(|(uuid, attribute_handle, properties, value_handle)| {
                    Characteristic { uuid, attribute_handle, properties, value_handle, end_handle }
                });
            }
        })
    }

    /// See Vol 3, Part G - 4.7.1 Discover All Characteristic Descriptors
    pub fn discover_all_characteristic_descriptors(
        &mut self,
        Characteristic { value_handle, end_handle: ending_handle, .. }: Characteristic,
    ) -> impl Stream<Item = Result<Descriptor, DiscoveryError>> + '_ {
        try_stream!({
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
                    .await
                    .map_err(RecoverableDiscoveryError::try_from);

                let find_information_rsp = match result {
                    Ok(find_information_rsp) => find_information_rsp,
                    // This sub-procedure is complete when the ATT_ERROR_RSP PDU is received
                    // and the Error Code parameter is set to Attribute Not Found (0x0A)
                    Err(Ok(RecoverableDiscoveryError::AttributeNotFound)) => {
                        break;
                    }
                    Err(Err(e)) => Err(e)?,
                };

                let last_handle = match find_information_rsp.specialize() {
                    Ok(att::packets::FindInformationRspChild::FindInformationRsp16(
                        att::FindInformationRsp16 { information_data },
                    )) => {
                        for att::InformationData16 { attribute_handle, uuid } in
                            information_data.iter().cloned()
                        {
                            yield Descriptor { attribute_handle, uuid: uuid.into() }
                        }
                        information_data.last().map(|data| data.attribute_handle)
                    }
                    Ok(att::packets::FindInformationRspChild::FindInformationRsp128(
                        att::FindInformationRsp128 { information_data },
                    )) => {
                        for att::InformationData128 { attribute_handle, uuid } in
                            information_data.iter().cloned()
                        {
                            yield Descriptor { attribute_handle, uuid: uuid.into() }
                        }
                        information_data.last().map(|data| data.attribute_handle)
                    }
                    Ok(att::packets::FindInformationRspChild::None) => Err(
                        InternalDiscoveryError::InvalidFindInformationRsp(find_information_rsp),
                    )?,
                    Err(e) => Err(InternalDiscoveryError::DecodeError(e))?,
                };

                let Some(last_handle) = last_handle else {
                    break;
                };

                // This sub-procedure is complete when the (...) or the
                // ATT_FIND_INFORMATION_RSP PDU has an Attribute Handle that is
                // equal to the Ending Handle of the request.

                if last_handle >= ending_handle {
                    break;
                }

                // The ATT_FIND_INFORMATION_REQ PDU shall be issued again
                // with the Starting Handle set to one greater than the last
                // Attribute Handle in the ATT_FIND_INFORMATION_RSP PDU.

                let Some(next_handle) = last_handle.checked_add(1) else {
                    break;
                };

                starting_handle = next_handle;
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

    pub const UUID1: Uuid = Uuid::uuid16(1);
    pub const UUID2: Uuid = Uuid::uuid128(2);
    pub const UUID3: Uuid = Uuid::uuid16(3);
    pub const UUID4: Uuid = Uuid::uuid16(4);
    pub const UUID5: Uuid = Uuid::uuid16(5);
    pub const UUID6: Uuid = Uuid::uuid16(6);

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
                            attribute_value: att::ServiceAttributeValue { uuid: UUID1.into() }
                                .try_into()
                                .unwrap(),
                        },
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0010).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x0017).unwrap(),
                            attribute_value: att::ServiceAttributeValue { uuid: UUID2.into() }
                                .try_into()
                                .unwrap(),
                        },
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0100).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x01FF).unwrap(),
                            attribute_value: att::ServiceAttributeValue { uuid: UUID3.into() }
                                .try_into()
                                .unwrap(),
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
                            attribute_value: att::ServiceAttributeValue { uuid: UUID4.into() }
                                .try_into()
                                .unwrap(),
                        },
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0300).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x03FF).unwrap(),
                            attribute_value: att::ServiceAttributeValue { uuid: UUID5.into() }
                                .try_into()
                                .unwrap(),
                        },
                        att::GroupAttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0400).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x04FF).unwrap(),
                            attribute_value: att::ServiceAttributeValue { uuid: UUID6.into() }
                                .try_into()
                                .unwrap(),
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
                    request_opcode_in_error: att::Opcode::AttReadByGroupTypeReq,
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
            Some(Ok(super::Service {
                uuid: UUID1,
                attribute_handle: att::AttributeHandle::new(0x0001).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x000F).unwrap()
            }))
        );
        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Service {
                uuid: UUID2,
                attribute_handle: att::AttributeHandle::new(0x0010).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x0017).unwrap()
            }))
        );
        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Service {
                uuid: UUID3,
                attribute_handle: att::AttributeHandle::new(0x0100).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x01FF).unwrap()
            }))
        );
        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Service {
                uuid: UUID4,
                attribute_handle: att::AttributeHandle::new(0x02CF).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x02FF).unwrap()
            }))
        );
        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Service {
                uuid: UUID5,
                attribute_handle: att::AttributeHandle::new(0x0300).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x03FF).unwrap()
            }))
        );
        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Service {
                uuid: UUID6,
                attribute_handle: att::AttributeHandle::new(0x0400).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x04FF).unwrap()
            }))
        );
        assert_eq!(discovery.next().await, None);
    }

    pub fn new_discover_primary_service_by_service_uuid_bearer() -> att::Bearer {
        att::Bearer::new_for_test()
            .receive_transaction(|request: att::FindByTypeValueReq| {
                assert_eq!(
                    request,
                    att::FindByTypeValueReq {
                        starting_handle: att::AttributeHandle::new(0x01).unwrap(),
                        ending_handle: att::AttributeHandle::new(0xFFFF).unwrap(),
                        attribute_type: super::PRIMARY_SERVICE.into(),
                        attribute_value: att::ServiceAttributeValue { uuid: UUID1.into() }
                            .try_into()
                            .unwrap(),
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
                        attribute_type: super::PRIMARY_SERVICE.into(),
                        attribute_value: att::ServiceAttributeValue { uuid: UUID1.into() }
                            .try_into()
                            .unwrap(),
                    }
                );
                Err(att::ErrorRsp {
                    request_opcode_in_error: att::Opcode::AttFindByTypeValueReq,
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
            Some(Ok(super::Service {
                uuid: UUID1,
                attribute_handle: att::AttributeHandle::new(0x0200).unwrap(),
                end_group_handle: att::AttributeHandle::new(0x0214).unwrap(),
            }))
        );
        assert_eq!(discovery.next().await, None);
    }

    pub fn new_find_included_services_bearer() -> att::Bearer {
        att::Bearer::new_for_test()
            .receive_transaction(|request: att::ReadByTypeReq| {
                assert_eq!(
                    request,
                    att::ReadByTypeReq {
                        starting_handle: att::AttributeHandle::new(0x0200).unwrap(),
                        ending_handle: att::AttributeHandle::new(0x0214).unwrap(),
                        attribute_type: super::INCLUDE.into(),
                    }
                );
                Ok(att::ReadByTypeRsp {
                    attribute_data_list: [att::AttributeData {
                        attribute_handle: att::AttributeHandle::new(0x0201).unwrap(),
                        attribute_value: att::IncludeAttributeValue {
                            attribute_handle: att::AttributeHandle::new(0x0500).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x0518).unwrap(),
                            uuid: UUID1.into(),
                        }
                        .try_into()
                        .unwrap(),
                    }]
                    .to_vec(),
                })
            })
            .receive_transaction(|request: att::ReadByTypeReq| {
                assert_eq!(
                    request,
                    att::ReadByTypeReq {
                        starting_handle: att::AttributeHandle::new(0x0202).unwrap(),
                        ending_handle: att::AttributeHandle::new(0x0214).unwrap(),
                        attribute_type: super::INCLUDE.into(),
                    }
                );
                Ok(att::ReadByTypeRsp {
                    attribute_data_list: [att::AttributeData {
                        attribute_handle: att::AttributeHandle::new(0x0202).unwrap(),
                        attribute_value: att::IncludeAttributeValue128 {
                            attribute_handle: att::AttributeHandle::new(0x0550).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x0568).unwrap(),
                        }
                        .try_into()
                        .unwrap(),
                    }]
                    .to_vec(),
                })
            })
            .receive_transaction(|request: att::ReadReq| {
                assert_eq!(
                    request,
                    att::ReadReq { attribute_handle: att::AttributeHandle::new(0x0550).unwrap() }
                );
                Ok(att::ReadRsp {
                    attribute_value: att::ServiceAttributeValue { uuid: UUID2.into() }
                        .try_into()
                        .unwrap(),
                })
            })
            .receive_transaction(|request: att::ReadByTypeReq| {
                assert_eq!(
                    request,
                    att::ReadByTypeReq {
                        starting_handle: att::AttributeHandle::new(0x0203).unwrap(),
                        ending_handle: att::AttributeHandle::new(0x0214).unwrap(),
                        attribute_type: super::INCLUDE.into(),
                    }
                );
                Err(att::ErrorRsp {
                    request_opcode_in_error: att::Opcode::AttReadByTypeReq,
                    attribute_handle_in_error: att::AttributeHandle::new(0x0203).unwrap(),
                    error_code: att::ErrorCode::AttributeNotFound,
                })
            })
    }

    // See Vol 3, Part G - 4.5.1 Find Included Services, Figure 4.4
    #[futures_test::test]
    async fn find_included_services() {
        let bearer = new_find_included_services_bearer();
        let mut gatt = super::Client::new(bearer);

        let mut discovery = pin!(gatt.find_included_services(super::Service {
            uuid: UUID1,
            attribute_handle: att::AttributeHandle::new(0x0200).unwrap(),
            end_group_handle: att::AttributeHandle::new(0x0214).unwrap(),
        }));

        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Include {
                attribute_handle: att::AttributeHandle::new(0x0201).unwrap(),
                service: super::Service {
                    uuid: UUID1,
                    attribute_handle: att::AttributeHandle::new(0x0500).unwrap(),
                    end_group_handle: att::AttributeHandle::new(0x0518).unwrap(),
                }
            }))
        );
        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Include {
                attribute_handle: att::AttributeHandle::new(0x0202).unwrap(),
                service: super::Service {
                    uuid: UUID2,
                    attribute_handle: att::AttributeHandle::new(0x0550).unwrap(),
                    end_group_handle: att::AttributeHandle::new(0x0568).unwrap(),
                }
            }))
        );
        assert_eq!(discovery.next().await, None);
    }

    pub fn new_discover_all_characteristics_of_a_service_bearer() -> att::Bearer {
        att::Bearer::new_for_test()
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
                            attribute_value: att::CharacteristicAttributeValue {
                                properties: 0x02,
                                value_attribute_handle: att::AttributeHandle::new(0x0204).unwrap(),
                                uuid: UUID1.into(),
                            }
                            .try_into()
                            .unwrap(),
                        },
                        att::AttributeData {
                            attribute_handle: att::AttributeHandle::new(0x0210).unwrap(),
                            attribute_value: att::CharacteristicAttributeValue {
                                properties: 0x02,
                                value_attribute_handle: att::AttributeHandle::new(0x0212).unwrap(),
                                uuid: UUID2.into(),
                            }
                            .try_into()
                            .unwrap(),
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
                    request_opcode_in_error: att::Opcode::AttReadByTypeReq,
                    attribute_handle_in_error: att::AttributeHandle::new(0x0211).unwrap(),
                    error_code: att::ErrorCode::AttributeNotFound,
                })
            })
    }

    // See Vol 3, Part G - 4.6.1 Discover All Characteristics of a Service, Figure 4.5
    #[futures_test::test]
    async fn discover_all_characteristics_of_a_service() {
        let bearer = new_discover_all_characteristics_of_a_service_bearer();
        let mut gatt = super::Client::new(bearer);
        let mut discovery = pin!(gatt.discover_all_characteristics_of_a_service(super::Service {
            uuid: UUID1,
            attribute_handle: att::AttributeHandle::new(0x0200).unwrap(),
            end_group_handle: att::AttributeHandle::new(0x0214).unwrap(),
        }));

        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Characteristic {
                uuid: UUID1,
                attribute_handle: att::AttributeHandle::new(0x0203).unwrap(),
                properties: 0x02,
                value_handle: att::AttributeHandle::new(0x0204).unwrap(),
                end_handle: att::AttributeHandle::new(0x020F).unwrap(),
            }))
        );
        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Characteristic {
                uuid: UUID2,
                attribute_handle: att::AttributeHandle::new(0x0210).unwrap(),
                properties: 0x02,
                value_handle: att::AttributeHandle::new(0x0212).unwrap(),
                end_handle: att::AttributeHandle::new(0x0214).unwrap(),
            }))
        );
        assert_eq!(discovery.next().await, None);
    }

    pub fn new_discover_all_characteristic_descriptors_bearer() -> att::Bearer {
        att::Bearer::new_for_test()
            .receive_transaction(|request: att::FindInformationReq| {
                assert_eq!(
                    request,
                    att::FindInformationReq {
                        starting_handle: att::AttributeHandle::new(0x0205).unwrap(),
                        ending_handle: att::AttributeHandle::new(0x020F).unwrap(),
                    }
                );
                Ok(att::FindInformationRsp128 {
                    information_data: [
                        att::InformationData128 {
                            attribute_handle: att::AttributeHandle::new(0x0205).unwrap(),
                            uuid: att::packets::Uuid128 { value: UUID1.get_as_128().to_le_bytes() },
                        },
                        att::InformationData128 {
                            attribute_handle: att::AttributeHandle::new(0x0206).unwrap(),
                            uuid: att::packets::Uuid128 { value: UUID2.get_as_128().to_le_bytes() },
                        },
                    ]
                    .to_vec(),
                }
                .try_into()
                .unwrap())
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
                    request_opcode_in_error: att::Opcode::AttFindInformationReq,
                    attribute_handle_in_error: att::AttributeHandle::new(0x0207).unwrap(),
                    error_code: att::ErrorCode::AttributeNotFound,
                })
            })
    }

    // See Vol 3, Part G - 4.7.1 Discover All Characteristics Descriptors, Figure 4.7
    #[futures_test::test]
    async fn discover_all_characteristic_descriptors() {
        let bearer = new_discover_all_characteristic_descriptors_bearer();

        let mut gatt = super::Client::new(bearer);
        let mut discovery =
            pin!(gatt.discover_all_characteristic_descriptors(super::Characteristic {
                uuid: UUID1,
                attribute_handle: att::AttributeHandle::new(0x0203).unwrap(),
                properties: 0x02,
                value_handle: att::AttributeHandle::new(0x0204).unwrap(),
                end_handle: att::AttributeHandle::new(0x020F).unwrap(),
            }));

        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Descriptor {
                uuid: UUID1,
                attribute_handle: att::AttributeHandle::new(0x0205).unwrap(),
            }))
        );
        assert_eq!(
            discovery.next().await,
            Some(Ok(super::Descriptor {
                uuid: UUID2,
                attribute_handle: att::AttributeHandle::new(0x0206).unwrap(),
            }))
        );
        assert_eq!(discovery.next().await, None);
    }
}
