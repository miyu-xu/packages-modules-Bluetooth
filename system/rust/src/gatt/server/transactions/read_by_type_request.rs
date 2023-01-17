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
        AttReadByTypeDataElementBuilder, AttReadByTypeRequestView, AttReadByTypeResponseBuilder,
        ParseError, Serializable,
    },
};

pub async fn handle_read_by_type_request<T: AttDatabase>(
    request: AttReadByTypeRequestView<'_>,
    mtu: usize,
    db: &T,
) -> Result<AttChild, ParseError> {
    let request_type: AttUuid = request.get_attribute_type().try_into()?;
    let mut out = PayloadAccumulator::new(mtu - 2);

    // this is what we return if we don't return a full response
    // we override it depending on the cause of our failure
    let mut failure_response = AttErrorResponseBuilder {
        opcode_in_error: AttOpcode::READ_BY_TYPE_REQUEST,
        handle_in_error: AttHandle::from(request.get_starting_handle()).into(),
        error_code: AttErrorCode::ATTRIBUTE_NOT_FOUND,
    };

    let mut curr_elem_size = None;

    for AttAttribute { handle, uuid, permissions } in db.list_attributes() {
        if (handle.0 as u64) < request.get_starting_handle().get_handle()
            || (handle.0 as u64) > request.get_ending_handle().get_handle()
        {
            continue;
        }

        if !permissions.readable {
            failure_response = AttErrorResponseBuilder {
                opcode_in_error: AttOpcode::READ_BY_GROUP_TYPE_REQUEST,
                handle_in_error: handle.into(),
                error_code: AttErrorCode::READ_NOT_PERMITTED,
            };
            break;
        }

        if uuid == request_type {
            match db.read_attribute(handle).await {
                Ok(value) => {
                    let value = truncate_att_data(value, mtu - 4);
                    let value_size = value.size_in_bits().unwrap_or(0);
                    if let Some(curr_elem_size) = curr_elem_size {
                        if curr_elem_size != value_size {
                            // no more attributes of the same size
                            break;
                        }
                    } else {
                        curr_elem_size = Some(value_size)
                    }

                    let next_elem = AttReadByTypeDataElementBuilder {
                        handle: handle.into(),
                        value: AttAttributeDataBuilder { _child_: value },
                    };

                    if !out.push(next_elem) {
                        break;
                    }
                }
                Err(error_code) => {
                    failure_response = AttErrorResponseBuilder {
                        opcode_in_error: AttOpcode::READ_BY_TYPE_REQUEST,
                        handle_in_error: handle.into(),
                        error_code,
                    };
                    break;
                }
            }
        }
    }

    let out = out.into_boxed_slice();

    Ok(if out.is_empty() {
        failure_response.into()
    } else {
        AttReadByTypeResponseBuilder { data: out }.into()
    })
}
