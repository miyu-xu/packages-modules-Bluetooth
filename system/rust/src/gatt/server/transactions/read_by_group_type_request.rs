use crate::{
    gatt::{
        ids::AttHandle,
        server::{
            att_database::{AttAttribute, AttDatabase},
            gatt_database::AttUuid,
            utils::{truncate_att_data, PayloadAccumulator},
        },
    },
    packets::{
        AttAttributeDataBuilder, AttChild, AttErrorCode, AttErrorResponseBuilder, AttOpcode,
        AttReadByGroupTypeDataElementBuilder, AttReadByGroupTypeRequestView,
        AttReadByGroupTypeResponseBuilder, ParseError, Serializable,
    },
};

pub async fn handle_read_by_group_type_request<T: AttDatabase>(
    request: AttReadByGroupTypeRequestView<'_>,
    mtu: usize,
    db: &T,
) -> Result<AttChild, ParseError> {
    let mut matches = PayloadAccumulator::new(mtu - 2);

    let mut curr_elem: Option<AttReadByGroupTypeDataElementBuilder> = None;
    let group_type: AttUuid = request.get_attribute_group_type().try_into()?;

    // this is what we return if we don't return a full response
    // we override it depending on the cause of our failure
    let mut failure_response = AttErrorResponseBuilder {
        opcode_in_error: AttOpcode::READ_BY_GROUP_TYPE_REQUEST,
        handle_in_error: AttHandle::from(request.get_starting_handle()).into(),
        error_code: AttErrorCode::ATTRIBUTE_NOT_FOUND,
    };

    for AttAttribute { handle, uuid, permissions } in db.list_attributes() {
        if (handle.0 as u64) < request.get_starting_handle().get_handle()
            || (handle.0 as u64) > request.get_ending_handle().get_handle()
        {
            continue;
        }

        if uuid == group_type {
            // if the uuid matches, we will try reading it
            // thus, if it isn't readable, we should exit here with error READ_NOT_PERMITTED
            if !permissions.readable {
                failure_response = AttErrorResponseBuilder {
                    opcode_in_error: AttOpcode::READ_BY_GROUP_TYPE_REQUEST,
                    handle_in_error: handle.into(),
                    error_code: AttErrorCode::READ_NOT_PERMITTED,
                };
                break;
            }

            // starting the next group
            match db.read_attribute(handle).await {
                Err(read_error) => {
                    failure_response = AttErrorResponseBuilder {
                        opcode_in_error: AttOpcode::READ_BY_GROUP_TYPE_REQUEST,
                        handle_in_error: handle.into(),
                        error_code: read_error,
                    };
                }
                Ok(value) => {
                    let truncated_value = truncate_att_data(value, mtu - 6);
                    if let Some(curr_elem) = &curr_elem {
                        // serialization failure will lead to an error packet anyway, so it doesn't matter what happens
                        // here anyway
                        if curr_elem.value.size_in_bits().unwrap_or(0)
                            != truncated_value.size_in_bits().unwrap_or(0)
                        {
                            // value change, end packet here (we'll push in cleanup)
                            // no need to set error_code since we will return an actual response!
                            break;
                        }
                    }

                    if let Some(curr) = curr_elem {
                        // push and (later) overwrite
                        if !matches.push(curr) {
                            curr_elem = None;
                            break;
                        }
                    }

                    // advance to the next group
                    curr_elem = Some(AttReadByGroupTypeDataElementBuilder {
                        handle: handle.into(),
                        end_group_handle: handle.into(),
                        value: AttAttributeDataBuilder { _child_: truncated_value },
                    });
                }
            }
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

    let matches = matches.into_boxed_slice();

    Ok(if matches.is_empty() {
        failure_response.into()
    } else {
        // TODO: prevent MTU violation
        AttReadByGroupTypeResponseBuilder { data: matches }.into()
    })
}

#[cfg(test)]
mod test {
    use crate::{
        gatt::{
            ids::AttHandle,
            server::{
                gatt_database::AttPermissions, test::packet::build_view,
                test::test_att_db::TestAttDatabase,
            },
        },
        packets::AttReadByGroupTypeRequestBuilder,
    };

    use super::*;

    #[test]
    fn test_simple_grouping() {
        // arrange
        let grouping_uuid = AttUuid::new([1, 2, 3, 4]);
        let db = TestAttDatabase::new(vec![
            (
                AttAttribute {
                    handle: AttHandle(3),
                    uuid: grouping_uuid,
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
                vec![5, 6],
            ),
            (
                AttAttribute {
                    handle: AttHandle(5),
                    uuid: grouping_uuid,
                    permissions: AttPermissions { readable: true, writable: false },
                },
                vec![6, 7],
            ),
        ]);
        let att_view = build_view(AttReadByGroupTypeRequestBuilder {
            starting_handle: AttHandle(2).into(),
            ending_handle: AttHandle(6).into(),
            attribute_group_type: grouping_uuid.into(),
        });

        // act
        let response =
            tokio_test::block_on(handle_read_by_group_type_request((&att_view).into(), 31, &db))
                .unwrap();
        print!("{:?}", response);

        // assert
        // check it serializes
        response.to_vec().unwrap();
        if let AttChild::AttReadByGroupTypeResponse(response) = response {
            assert_eq!(response.data.len(), 2);
            assert_eq!(response.data[0].handle.handle, 3);
            assert_eq!(response.data[0].end_group_handle.handle, 4);
            assert_eq!(response.data[0].value.to_vec().unwrap(), vec![4, 5]);
            assert_eq!(response.data[1].handle.handle, 5);
            assert_eq!(response.data[1].end_group_handle.handle, 5);
            assert_eq!(response.data[1].value.to_vec().unwrap(), vec![6, 7]);
        } else {
            unreachable!()
        }
    }
}
