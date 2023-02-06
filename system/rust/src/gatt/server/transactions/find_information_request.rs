use crate::{
    gatt::{
        ids::AttHandle,
        server::{
            att_database::{AttAttribute, AttDatabase},
            utils::PayloadAccumulator,
        },
    },
    packets::{
        AttChild, AttErrorCode, AttErrorResponseBuilder, AttFindInformationLongResponseBuilder,
        AttFindInformationRequestView, AttFindInformationResponseBuilder,
        AttFindInformationResponseFormat, AttFindInformationResponseLongEntryBuilder,
        AttFindInformationResponseShortEntryBuilder, AttFindInformationShortResponseBuilder,
        AttOpcode, SerializeError,
    },
};

pub fn handle_find_information_request<T: AttDatabase>(
    request: AttFindInformationRequestView<'_>,
    mtu: usize,
    db: &T,
) -> AttChild {
    if request.get_starting_handle().get_handle() == 0
        || request.get_starting_handle().get_handle() > request.get_ending_handle().get_handle()
    {
        return AttErrorResponseBuilder {
            opcode_in_error: AttOpcode::FIND_INFORMATION_REQUEST,
            handle_in_error: AttHandle::from(request.get_starting_handle()).into(),
            error_code: AttErrorCode::INVALID_HANDLE,
        }
        .into();
    }

    let attrs = db.list_attributes().into_iter().filter(|AttAttribute { handle, .. }| {
        request.get_starting_handle().get_handle() <= handle.0
            && handle.0 <= request.get_ending_handle().get_handle()
    });

    if let Some(resp) = handle_find_information_request_short(attrs.clone(), mtu) {
        AttFindInformationResponseBuilder {
            format: AttFindInformationResponseFormat::SHORT,
            _child_: resp.into(),
        }
        .into()
    } else if let Some(resp) = handle_find_information_request_long(attrs, mtu) {
        AttFindInformationResponseBuilder {
            format: AttFindInformationResponseFormat::LONG,
            _child_: resp.into(),
        }
        .into()
    } else {
        AttErrorResponseBuilder {
            opcode_in_error: AttOpcode::FIND_INFORMATION_REQUEST,
            handle_in_error: AttHandle::from(request.get_starting_handle()).into(),
            error_code: AttErrorCode::ATTRIBUTE_NOT_FOUND,
        }
        .into()
    }
}

fn handle_find_information_request_short(
    attributes: impl Iterator<Item = AttAttribute>,
    mtu: usize,
) -> Option<AttFindInformationShortResponseBuilder> {
    let mut out = PayloadAccumulator::new(mtu - 2);
    for AttAttribute { handle, type_: uuid, .. } in attributes {
        match uuid.try_into().map_err(|_| SerializeError::ValueTooLarge) {
            Ok(uuid) => {
                if !out.push(AttFindInformationResponseShortEntryBuilder {
                    handle: handle.into(),
                    uuid,
                }) {
                    break;
                }
            }
            Err(_) => {
                if out.is_empty() {
                    return None;
                } else {
                    break;
                }
            }
        }
    }

    if out.is_empty() {
        None
    } else {
        Some(AttFindInformationShortResponseBuilder { data: out.into_boxed_slice() })
    }
}

fn handle_find_information_request_long(
    attributes: impl Iterator<Item = AttAttribute>,
    mtu: usize,
) -> Option<AttFindInformationLongResponseBuilder> {
    let mut out = PayloadAccumulator::new(mtu - 2);
    for AttAttribute { handle, type_: uuid, .. } in attributes {
        if !out.push(AttFindInformationResponseLongEntryBuilder {
            handle: handle.into(),
            uuid: uuid.into(),
        }) {
            break;
        }
    }

    if out.is_empty() {
        None
    } else {
        Some(AttFindInformationLongResponseBuilder { data: out.into_boxed_slice() })
    }
}

#[cfg(test)]
mod test {
    use crate::{
        core::uuid::Uuid,
        gatt::server::{gatt_database::AttPermissions, test::test_att_db::TestAttDatabase},
        packets::{
            AttFindInformationRequestBuilder, AttFindInformationResponseChild, Serializable,
            Uuid128Builder, Uuid16Builder,
        },
        utils::packet::build_view_or_crash,
    };

    use super::*;

    #[test]
    fn test_long_uuids() {
        // arrange
        let db = TestAttDatabase::new(vec![
            (
                AttAttribute {
                    handle: AttHandle(3),
                    type_: Uuid::new(0x01020304),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(4),
                    type_: Uuid::new(0x01020305),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(5),
                    type_: Uuid::new(0x01020306),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
        ]);
        let att_view = build_view_or_crash(AttFindInformationRequestBuilder {
            starting_handle: AttHandle(3).into(),
            ending_handle: AttHandle(4).into(),
        });

        // act
        let response = handle_find_information_request((&att_view).into(), 128, &db);
        print!("{:?}", response);

        // assert
        // check it serializes
        response.to_vec().unwrap();
        if let AttChild::AttFindInformationResponse(response) = response {
            assert_eq!(response.format, AttFindInformationResponseFormat::LONG);
            if let AttFindInformationResponseChild::AttFindInformationLongResponse(response) =
                response._child_
            {
                assert_eq!(response.data.len(), 2);
                assert_eq!(response.data[0].handle.handle, 3);
                assert_eq!(
                    response.data[0].uuid.data.to_vec(),
                    Uuid128Builder::from(Uuid::new(0x01020304)).data.to_vec()
                );
                assert_eq!(response.data[1].handle.handle, 4);
                assert_eq!(
                    response.data[1].uuid.data.to_vec(),
                    Uuid128Builder::from(Uuid::new(0x01020305)).data.to_vec()
                );
            } else {
                unreachable!()
            }
        } else {
            unreachable!()
        }
    }

    #[test]
    fn test_short_uuids() {
        // arrange
        let db = TestAttDatabase::new(vec![
            (
                AttAttribute {
                    handle: AttHandle(3),
                    type_: Uuid::new(0x0102),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(4),
                    type_: Uuid::new(0x0103),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(5),
                    type_: Uuid::new(0x01020306),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
        ]);
        let att_view = build_view_or_crash(AttFindInformationRequestBuilder {
            starting_handle: AttHandle(3).into(),
            ending_handle: AttHandle(5).into(),
        });

        // act
        let response = handle_find_information_request((&att_view).into(), 128, &db);
        print!("{:?}", response);

        // assert
        // check it serializes
        response.to_vec().unwrap();
        if let AttChild::AttFindInformationResponse(response) = response {
            assert_eq!(response.format, AttFindInformationResponseFormat::SHORT);
            if let AttFindInformationResponseChild::AttFindInformationShortResponse(response) =
                response._child_
            {
                assert_eq!(response.data.len(), 2);
                assert_eq!(response.data[0].handle.handle, 3);
                assert_eq!(
                    response.data[0].uuid.data,
                    Uuid16Builder::try_from(Uuid::new(0x0102)).unwrap().data
                );
                assert_eq!(response.data[1].handle.handle, 4);
                assert_eq!(
                    response.data[1].uuid.data,
                    Uuid16Builder::try_from(Uuid::new(0x0103)).unwrap().data
                );
            } else {
                unreachable!()
            }
        } else {
            unreachable!()
        }
    }
}
