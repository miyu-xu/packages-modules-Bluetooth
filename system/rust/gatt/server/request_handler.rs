use std::rc::Rc;

use log::warn;

use crate::packets::{
    AttAttributeDataBuilder, AttChild, AttErrorCode, AttErrorResponseBuilder, AttOpcode,
    AttReadByGroupTypeDataElementBuilder, AttReadByGroupTypeRequestView,
    AttReadByGroupTypeResponseBuilder, AttReadByTypeDataElementBuilder, AttReadByTypeRequestView,
    AttReadByTypeResponseBuilder, AttReadRequestView, AttReadResponseBuilder, AttView,
    Serializable,
};

use super::att_database::{AttAttribute, AttDatabase, AttHandle, AttUuid};

/// This struct handles all requests needing ACKs. Only ONE should exist per bearer per database,
/// to ensure serialization.
pub struct GattRequestHandler<Db: AttDatabase> {
    db: Rc<Db>,
}

impl<Db: AttDatabase> GattRequestHandler<Db> {
    pub fn new(db: Rc<Db>) -> Self {
        Self { db }
    }

    // Runs a task to process an incoming packet. We take an exclusive reference to ensure
    // that only one request is outstanding at a time (notifications + commands should take a different path)
    pub async fn process_packet(&mut self, packet: AttView<'_>) -> Result<AttChild, String> {
        // TODO: any error should give an error packet, not a Result<>
        Ok(match packet.get_opcode() {
            AttOpcode::READ_REQUEST => {
                let read_request = AttReadRequestView::try_parse(packet)
                    .map_err(|_| "parse failure".to_string())?;
                let handle = read_request.get_attribute_handle().into();
                let data = self.db.read_attribute(handle).await?;
                AttReadResponseBuilder { value: AttAttributeDataBuilder { _child_: data } }.into()
            }
            AttOpcode::READ_BY_GROUP_TYPE_REQUEST => {
                let request = AttReadByGroupTypeRequestView::try_parse(packet)
                    .map_err(|_| "parse failure".to_string())?;
                let mut matches = vec![];
                let mut curr_elem: Option<AttReadByGroupTypeDataElementBuilder> = None;
                let group_type: AttUuid = request.get_attribute_group_type().try_into()?;
                for AttAttribute { handle, uuid, permissions } in self.db.list_attributes() {
                    if !permissions.readable
                        || (handle.0 as u64) < request.get_starting_handle().get_handle()
                        || (handle.0 as u64) > request.get_ending_handle().get_handle()
                    {
                        continue;
                    }

                    if uuid == group_type {
                        // starting the next group
                        let value = self.db.read_attribute(handle).await?;

                        if let Some(curr_elem) = &curr_elem {
                            if curr_elem.value.size_in_bits().map_err(|_| "err".to_string())?
                                != value.size_in_bits().map_err(|_| "invalid data".to_string())?
                            {
                                // value change, end packet here (we'll push in cleanup)
                                break;
                            }
                        }

                        if let Some(curr_elem) = curr_elem {
                            // push and (later) overwrite
                            matches.push(curr_elem);
                        }

                        // advance to the next group
                        curr_elem = Some(AttReadByGroupTypeDataElementBuilder {
                            handle: handle.into(),
                            end_group_handle: handle.into(),
                            value: AttAttributeDataBuilder { _child_: value },
                        });
                    } else {
                        // advance the group end handle if we're within a group
                        if let Some(curr_elem) = &mut curr_elem {
                            curr_elem.end_group_handle = handle.into();
                        }
                    }
                }
                if let Some(curr_elem) = curr_elem {
                    matches.push(curr_elem);
                }

                if matches.is_empty() {
                    AttErrorResponseBuilder {
                        opcode_in_error: AttOpcode::READ_BY_GROUP_TYPE_REQUEST,
                        command_flag_in_error: packet.get_command_flag(),
                        handle_in_error: AttHandle::from(request.get_starting_handle()).into(),
                        error_code: AttErrorCode::ATTRIBUTE_NOT_FOUND,
                    }
                    .into()
                } else {
                    // TODO: prevent MTU violation
                    AttReadByGroupTypeResponseBuilder { data: matches.into() }.into()
                }
            }
            AttOpcode::READ_BY_TYPE_REQUEST => {
                let request = AttReadByTypeRequestView::try_parse(packet)
                    .map_err(|_| "parse failure".to_string())?;
                let request_type: AttUuid = request.get_attribute_type().try_into()?;
                let mut out = vec![];
                for AttAttribute { handle, uuid, permissions } in self.db.list_attributes() {
                    if !permissions.readable
                        || (handle.0 as u64) < request.get_starting_handle().get_handle()
                        || (handle.0 as u64) > request.get_ending_handle().get_handle()
                    {
                        continue;
                    }

                    if uuid == request_type {
                        out.push(AttReadByTypeDataElementBuilder {
                            handle: handle.into(),
                            value: AttAttributeDataBuilder {
                                _child_: self.db.read_attribute(handle).await?,
                            },
                        })
                    }
                }

                if out.is_empty() {
                    AttErrorResponseBuilder {
                        opcode_in_error: AttOpcode::READ_BY_GROUP_TYPE_REQUEST,
                        command_flag_in_error: packet.get_command_flag(),
                        handle_in_error: AttHandle::from(request.get_starting_handle()).into(),
                        error_code: AttErrorCode::ATTRIBUTE_NOT_FOUND,
                    }
                    .into()
                } else {
                    AttReadByTypeResponseBuilder { data: out.into() }.into()
                }
            }
            _ => {
                warn!("Dropping unsupported opcode {:?}", packet.get_opcode());
                AttErrorResponseBuilder {
                    opcode_in_error: packet.get_opcode(),
                    command_flag_in_error: packet.get_command_flag(),
                    handle_in_error: AttHandle(0).into(),
                    error_code: AttErrorCode::REQUEST_NOT_SUPPORTED,
                }
                .into()
            }
        })
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
    }
}

#[cfg(test)]
mod test {

    use std::rc::Rc;

    use crate::{
        gatt::server::{
            att_database::{AttAttribute, AttHandle, AttPermissions, AttUuid},
            callback_att_db::CallbackAttDatabase,
            request_handler::GattRequestHandler,
        },
        packets::{
            AttBuilder, AttChild, AttReadByGroupTypeRequestBuilder, AttReadByGroupTypeResponseView,
            AttReadByTypeRequestBuilder, AttReadByTypeResponseView, AttReadRequestBuilder,
            AttReadResponseView, OwnedAttView,
        },
    };

    use super::HACK_child_to_opcode;

    fn build_view(child: impl Into<AttChild>) -> OwnedAttView {
        let child = child.into();
        let opcode = HACK_child_to_opcode(&child);
        let serialized = AttBuilder { _child_: child, command_flag: 0, opcode }.to_vec().unwrap();
        OwnedAttView::try_parse(serialized.into_boxed_slice()).unwrap()
    }

    #[test]
    fn test_read_request() {
        // arrange
        let db = CallbackAttDatabase::new(
            |_| Ok(vec![1, 2, 3].into_boxed_slice()),
            |_, _| unreachable!(),
            vec![AttAttribute {
                handle: AttHandle(0),
                uuid: AttUuid::new([1, 2, 3, 4]),
                permissions: AttPermissions { readable: true, writable: false },
            }],
        );
        let mut handler = GattRequestHandler { db: Rc::new(db) };
        let att_view = build_view(AttReadRequestBuilder { attribute_handle: AttHandle(3).into() });

        // act
        let response = tokio_test::block_on(handler.process_packet((&att_view).into())).unwrap();

        // assert
        let response = build_view(response);
        let response = AttReadResponseView::try_parse(response.view()).unwrap();
        assert_eq!(response.get_value().get_raw_payload().collect::<Vec<_>>(), vec![1, 2, 3]);
    }

    #[test]
    fn test_read_by_type_request() {
        // arrange
        let db = CallbackAttDatabase::new(
            |handle| Ok(vec![(handle.0 + 1) as u8, (handle.0 + 2) as u8].into_boxed_slice()),
            |_, _| unreachable!(),
            vec![AttAttribute {
                handle: AttHandle(3),
                uuid: AttUuid::new([1, 2, 3, 4]),
                permissions: AttPermissions { readable: true, writable: false },
            }],
        );
        let mut handler = GattRequestHandler { db: Rc::new(db) };
        let att_view = build_view(AttReadByTypeRequestBuilder {
            starting_handle: AttHandle(2).into(),
            ending_handle: AttHandle(4).into(),
            attribute_type: AttUuid::new([1, 2, 3, 4]).into(),
        });

        // act
        let response = tokio_test::block_on(handler.process_packet((&att_view).into())).unwrap();

        // assert
        let response = build_view(response);
        let response = AttReadByTypeResponseView::try_parse(response.view()).unwrap();
        for resp in response.get_data_iter() {
            assert_eq!(resp.get_handle().get_handle(), 3);
            assert_eq!(resp.get_value().get_raw_payload().collect::<Vec<_>>(), vec![4, 5]);
        }
    }

    #[test]
    fn test_read_by_group_type_request() {
        // arrange
        let grouping_uuid = AttUuid::new([1, 2, 3, 4]);
        let db = CallbackAttDatabase::new(
            |handle| Ok(vec![(handle.0 + 1) as u8, (handle.0 + 2) as u8].into_boxed_slice()),
            |_, _| unreachable!(),
            vec![
                AttAttribute {
                    handle: AttHandle(3),
                    uuid: grouping_uuid,
                    permissions: AttPermissions { readable: true, writable: false },
                },
                AttAttribute {
                    handle: AttHandle(4),
                    uuid: AttUuid::new([1, 2, 3, 5]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                AttAttribute {
                    handle: AttHandle(5),
                    uuid: grouping_uuid,
                    permissions: AttPermissions { readable: true, writable: false },
                },
            ],
        );
        let mut handler = GattRequestHandler { db: Rc::new(db) };
        let att_view = build_view(AttReadByGroupTypeRequestBuilder {
            starting_handle: AttHandle(2).into(),
            ending_handle: AttHandle(6).into(),
            attribute_group_type: grouping_uuid.into(),
        });

        // act
        let response = tokio_test::block_on(handler.process_packet((&att_view).into())).unwrap();
        print!("{:?}", response);

        // assert
        let response = build_view(response);
        let response = AttReadByGroupTypeResponseView::try_parse(response.view()).unwrap();
        let response = response.get_data_iter().collect::<Vec<_>>();
        assert_eq!(response.len(), 2);
        assert_eq!(response[0].get_handle().get_handle(), 3);
        assert_eq!(response[0].get_end_group_handle().get_handle(), 4);
        assert_eq!(response[0].get_value().get_raw_payload().collect::<Vec<_>>(), vec![4, 5]);
        assert_eq!(response[1].get_handle().get_handle(), 5);
        assert_eq!(response[1].get_end_group_handle().get_handle(), 5);
        assert_eq!(response[1].get_value().get_raw_payload().collect::<Vec<_>>(), vec![6, 7]);
    }
}
