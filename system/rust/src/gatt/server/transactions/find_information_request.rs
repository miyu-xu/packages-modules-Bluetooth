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
        request.get_starting_handle().get_handle() <= (handle.0 as u64)
            && (handle.0 as u64) <= request.get_ending_handle().get_handle()
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
    for AttAttribute { handle, uuid, .. } in attributes {
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
    for AttAttribute { handle, uuid, .. } in attributes {
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
        gatt::server::{
            gatt_database::{AttPermissions, AttUuid},
            test::packet::build_view,
            test::test_att_db::TestAttDatabase,
        },
        packets::{
            AttFindInformationRequestBuilder, AttFindInformationResponseChild, Serializable,
            Uuid128Builder, Uuid16Builder,
        },
    };

    use super::*;

    #[test]
    fn test_long_uuids() {
        // arrange
        let db = TestAttDatabase::new(vec![
            (
                AttAttribute {
                    handle: AttHandle(3),
                    uuid: AttUuid::new([1, 2, 3, 4]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(4),
                    uuid: AttUuid::new([1, 2, 3, 5]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(5),
                    uuid: AttUuid::new([1, 2, 3, 6]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
        ]);
        let att_view = build_view(AttFindInformationRequestBuilder {
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
                    Uuid128Builder::from(AttUuid::from([1, 2, 3, 4])).data.to_vec()
                );
                assert_eq!(response.data[1].handle.handle, 4);
                assert_eq!(
                    response.data[1].uuid.data.to_vec(),
                    Uuid128Builder::from(AttUuid::from([1, 2, 3, 5])).data.to_vec()
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
                    uuid: AttUuid::from([1, 2]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(4),
                    uuid: AttUuid::from([1, 3]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(5),
                    uuid: AttUuid::new([1, 2, 3, 6]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
        ]);
        let att_view = build_view(AttFindInformationRequestBuilder {
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
                    response.data[0].uuid.data.to_vec(),
                    Uuid16Builder::try_from(AttUuid::from([1, 2])).unwrap().data.to_vec()
                );
                assert_eq!(response.data[1].handle.handle, 4);
                assert_eq!(
                    response.data[1].uuid.data.to_vec(),
                    Uuid16Builder::try_from(AttUuid::from([1, 3])).unwrap().data.to_vec()
                );
            } else {
                unreachable!()
            }
        } else {
            unreachable!()
        }
    }
}
