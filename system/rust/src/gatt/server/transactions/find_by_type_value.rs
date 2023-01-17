use log::warn;

use crate::{
    gatt::{
        ids::AttHandle,
        server::{
            att_database::{AttAttribute, AttDatabase},
            gatt_database::AttUuid,
            utils::PayloadAccumulator,
        },
    },
    packets::{
        AttChild, AttErrorCode, AttErrorResponseBuilder, AttFindByTypeValueRequestView,
        AttFindByTypeValueResponseBuilder, AttOpcode, AttributeHandleRangeBuilder, Serializable,
    },
};

pub async fn handle_find_by_type_value_request<T: AttDatabase>(
    request: AttFindByTypeValueRequestView<'_>,
    mtu: usize,
    db: &T,
) -> AttChild {
    if request.get_starting_handle().get_handle() == 0
        || request.get_starting_handle().get_handle() > request.get_ending_handle().get_handle()
    {
        return AttErrorResponseBuilder {
            opcode_in_error: AttOpcode::FIND_BY_TYPE_VALUE_REQUEST,
            handle_in_error: AttHandle::from(request.get_starting_handle()).into(),
            error_code: AttErrorCode::INVALID_HANDLE,
        }
        .into();
    }

    let mut matches = PayloadAccumulator::new(mtu - 1);

    for AttAttribute { handle, uuid, permissions } in db.list_attributes() {
        if (handle.0 as u64) < request.get_starting_handle().get_handle()
            || (handle.0 as u64) > request.get_ending_handle().get_handle()
        {
            continue;
        }
        if AttUuid::from(&request.get_attribute_type()) != uuid || !permissions.readable {
            continue;
        }
        if let Ok(value) = db.read_attribute(handle).await {
            if let Ok(data) = value.to_vec() {
                if data == request.get_attribute_value().get_raw_payload().collect::<Vec<_>>() {
                    // match found
                    if !matches.push(AttributeHandleRangeBuilder {
                        found_attribute_handle: handle.into(),
                        group_end_handle: handle.into(), // FIXME
                    }) {
                        break;
                    }
                }
            }
        } else {
            warn!("skipping {handle:?} in FindByTypeRequest since read failed")
        }
    }

    if matches.is_empty() {
        AttErrorResponseBuilder {
            opcode_in_error: AttOpcode::FIND_BY_TYPE_VALUE_REQUEST,
            handle_in_error: AttHandle::from(request.get_starting_handle()).into(),
            error_code: AttErrorCode::ATTRIBUTE_NOT_FOUND,
        }
        .into()
    } else {
        AttFindByTypeValueResponseBuilder { handles_info: matches.into_boxed_slice() }.into()
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
            AttAttributeDataBuilder, AttAttributeDataChild, AttFindByTypeValueRequestBuilder,
            Serializable, Uuid16Builder,
        },
    };

    use super::*;

    #[test]
    fn test_basic() {
        // arrange
        let db = TestAttDatabase::new(vec![
            (
                AttAttribute {
                    handle: AttHandle(3),
                    uuid: AttUuid::new([1, 2, 0, 0]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![3, 4],
            ),
            (
                AttAttribute {
                    handle: AttHandle(4),
                    uuid: AttUuid::new([1, 3, 0, 0]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![4, 5],
            ),
            (
                AttAttribute {
                    handle: AttHandle(5),
                    uuid: AttUuid::new([1, 2, 0, 0]),
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![3, 4],
            ),
        ]);
        let att_view = build_view(AttFindByTypeValueRequestBuilder {
            starting_handle: AttHandle(3).into(),
            ending_handle: AttHandle(5).into(),
            attribute_type: Uuid16Builder { data: vec![1, 2].into_boxed_slice() },
            attribute_value: AttAttributeDataBuilder {
                _child_: AttAttributeDataChild::RawData(vec![3, 4].into_boxed_slice()),
            },
        });

        // act
        let response =
            tokio_test::block_on(handle_find_by_type_value_request((&att_view).into(), 128, &db));
        print!("{:?}", response);

        // assert
        // check it serializes
        response.to_vec().unwrap();
        if let AttChild::AttFindByTypeValueResponse(response) = response {
            assert_eq!(response.handles_info.len(), 2);
            assert_eq!(response.handles_info[0].found_attribute_handle.handle, 3);
            assert_eq!(response.handles_info[0].group_end_handle.handle, 3);
            assert_eq!(response.handles_info[1].found_attribute_handle.handle, 5);
            assert_eq!(response.handles_info[1].group_end_handle.handle, 5);
        } else {
            unreachable!("{response:?}")
        }
    }
}
