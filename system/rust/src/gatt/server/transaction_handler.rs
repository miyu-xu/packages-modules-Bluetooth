use log::warn;

use crate::{
    gatt::ids::AttHandle,
    packets::{
        AttChild, AttErrorCode, AttErrorResponseBuilder, AttFindByTypeValueRequestView,
        AttFindInformationRequestView, AttOpcode, AttReadByGroupTypeRequestView,
        AttReadByTypeRequestView, AttReadRequestView, AttView, AttWriteRequestView, Packet,
        ParseError,
    },
};

use super::{
    att_database::AttDatabase,
    transactions::{
        find_by_type_value::handle_find_by_type_value_request,
        find_information_request::handle_find_information_request,
        read_by_group_type_request::handle_read_by_group_type_request,
        read_by_type_request::handle_read_by_type_request, read_request::handle_read_request,
        write_request::handle_write_request,
    },
};

/// This struct handles all requests needing ACKs. Only ONE should exist per bearer per database,
/// to ensure serialization.
pub struct GattRequestHandler<Db: AttDatabase> {
    db: Db,
}

impl<Db: AttDatabase> GattRequestHandler<Db> {
    pub fn new(db: Db) -> Self {
        Self { db }
    }

    // Runs a task to process an incoming packet. We take an exclusive reference to ensure
    // that only one request is outstanding at a time (notifications + commands should take a different path)
    pub async fn process_packet(&mut self, packet: AttView<'_>, mtu: usize) -> AttChild {
        match self.try_parse_and_process_packet(packet, mtu).await {
            Ok(result) => result,
            Err(_) => {
                // parse error, assume it's an unsupported request
                AttErrorResponseBuilder {
                    opcode_in_error: packet.get_opcode(),
                    handle_in_error: AttHandle(0).into(),
                    error_code: AttErrorCode::REQUEST_NOT_SUPPORTED,
                }
                .into()
            }
        }
    }

    async fn try_parse_and_process_packet(
        &mut self,
        packet: AttView<'_>,
        mtu: usize,
    ) -> Result<AttChild, ParseError> {
        match packet.get_opcode() {
            AttOpcode::READ_REQUEST => {
                Ok(handle_read_request(AttReadRequestView::try_parse(packet)?, mtu, &self.db).await)
            }
            AttOpcode::READ_BY_GROUP_TYPE_REQUEST => {
                handle_read_by_group_type_request(
                    AttReadByGroupTypeRequestView::try_parse(packet)?,
                    mtu,
                    &self.db,
                )
                .await
            }
            AttOpcode::READ_BY_TYPE_REQUEST => {
                handle_read_by_type_request(
                    AttReadByTypeRequestView::try_parse(packet)?,
                    mtu,
                    &self.db,
                )
                .await
            }
            AttOpcode::FIND_INFORMATION_REQUEST => Ok(handle_find_information_request(
                AttFindInformationRequestView::try_parse(packet)?,
                mtu,
                &self.db,
            )),
            AttOpcode::FIND_BY_TYPE_VALUE_REQUEST => Ok(handle_find_by_type_value_request(
                AttFindByTypeValueRequestView::try_parse(packet)?,
                mtu,
                &self.db,
            )
            .await),
            AttOpcode::WRITE_REQUEST => {
                Ok(handle_write_request(AttWriteRequestView::try_parse(packet)?, &self.db).await)
            }
            _ => {
                warn!("Dropping unsupported opcode {:?}", packet.get_opcode());
                Err(ParseError::InvalidEnumValue)
            }
        }
    }
}

// TODO(aryarahul) - get rid of this, PDL should deal with it!
#[allow(non_snake_case)]
pub fn HACK_child_to_opcode(child: &AttChild) -> AttOpcode {
    match child {
        AttChild::RawData(_vec) => unreachable!(),
        AttChild::AttFindInformationRequest(_) => AttOpcode::FIND_INFORMATION_REQUEST,
        AttChild::AttReadByGroupTypeRequest(_) => AttOpcode::READ_BY_GROUP_TYPE_REQUEST,
        AttChild::AttReadByTypeRequest(_) => AttOpcode::READ_BY_TYPE_REQUEST,
        AttChild::AttReadRequest(_) => AttOpcode::READ_REQUEST,
        AttChild::AttReadResponse(_) => AttOpcode::READ_RESPONSE,
        AttChild::AttErrorResponse(_) => AttOpcode::ERROR_RESPONSE,
        AttChild::AttReadByGroupTypeResponse(_) => AttOpcode::READ_BY_GROUP_TYPE_RESPONSE,
        AttChild::AttReadByTypeResponse(_) => AttOpcode::READ_BY_TYPE_RESPONSE,
        AttChild::AttFindInformationResponse(_) => AttOpcode::FIND_BY_TYPE_VALUE_RESPONSE,
        AttChild::AttFindByTypeValueRequest(_) => AttOpcode::FIND_BY_TYPE_VALUE_REQUEST,
        AttChild::AttFindByTypeValueResponse(_) => AttOpcode::FIND_BY_TYPE_VALUE_RESPONSE,
        AttChild::AttWriteRequest(_) => AttOpcode::WRITE_REQUEST,
        AttChild::AttWriteResponse(_) => AttOpcode::WRITE_RESPONSE,
    }
}

#[cfg(test)]
mod test {

    use super::*;

    use crate::{
        gatt::server::{
            att_database::{AttAttribute, AttPermissions, AttUuid},
            test::packet::build_att_view,
            test::test_att_db::TestAttDatabase,
            transaction_handler::GattRequestHandler,
        },
        packets::{
            AttReadByTypeRequestBuilder, AttReadByTypeResponseView, AttReadRequestBuilder,
            AttReadResponseView,
        },
    };

    #[test]
    fn test_read_request() {
        // arrange
        let db = TestAttDatabase::new(vec![(
            AttAttribute {
                handle: AttHandle(3),
                uuid: AttUuid::new([1, 2, 3, 4]),
                permissions: AttPermissions { readable: true, writable: false },
            },
            vec![1, 2, 3],
        )]);
        let mut handler = GattRequestHandler { db };
        let att_view =
            build_att_view(AttReadRequestBuilder { attribute_handle: AttHandle(3).into() });

        // act
        let response = tokio_test::block_on(handler.process_packet((&att_view).into(), 31));

        // assert
        let response = build_att_view(response);
        let response = AttReadResponseView::try_parse(response.view()).unwrap();
        assert_eq!(response.get_value().get_raw_payload().collect::<Vec<_>>(), vec![1, 2, 3]);
    }

    #[test]
    fn test_read_by_type_request() {
        // arrange
        let db = TestAttDatabase::new(vec![(
            AttAttribute {
                handle: AttHandle(3),
                uuid: AttUuid::new([1, 2, 3, 4]),
                permissions: AttPermissions { readable: true, writable: false },
            },
            vec![4, 5],
        )]);
        let mut handler = GattRequestHandler { db };
        let att_view = build_att_view(AttReadByTypeRequestBuilder {
            starting_handle: AttHandle(2).into(),
            ending_handle: AttHandle(4).into(),
            attribute_type: AttUuid::new([1, 2, 3, 4]).into(),
        });

        // act
        let response = tokio_test::block_on(handler.process_packet((&att_view).into(), 31));

        // assert
        let response = build_att_view(response);
        let response = AttReadByTypeResponseView::try_parse(response.view()).unwrap();
        for resp in response.get_data_iter() {
            assert_eq!(resp.get_handle().get_handle(), 3);
            assert_eq!(resp.get_value().get_raw_payload().collect::<Vec<_>>(), vec![4, 5]);
        }
    }
}
