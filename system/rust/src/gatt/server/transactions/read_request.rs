use crate::{
    gatt::server::{att_database::AttDatabase, utils::truncate_att_data},
    packets::{
        AttAttributeDataBuilder, AttChild, AttErrorResponseBuilder, AttOpcode, AttReadRequestView,
        AttReadResponseBuilder,
    },
};

pub async fn handle_read_request<T: AttDatabase>(
    request: AttReadRequestView<'_>,
    mtu: usize,
    db: &T,
) -> AttChild {
    let handle = request.get_attribute_handle().into();
    match db.read_attribute(handle).await {
        Ok(data) => AttReadResponseBuilder {
            value: AttAttributeDataBuilder { _child_: truncate_att_data(data, mtu - 1) },
        }
        .into(),
        Err(error_code) => AttErrorResponseBuilder {
            opcode_in_error: AttOpcode::READ_REQUEST,
            handle_in_error: handle.into(),
            error_code,
        }
        .into(),
    }
}
