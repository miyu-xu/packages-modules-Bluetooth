use crate::{
    gatt::server::att_database::AttDatabase,
    packets::{
        AttChild, AttErrorResponseBuilder, AttOpcode, AttWriteRequestView, AttWriteResponseBuilder,
    },
};

pub async fn handle_write_request<T: AttDatabase>(
    request: AttWriteRequestView<'_>,
    db: &T,
) -> AttChild {
    let handle = request.get_handle().into();
    match db.write_attribute(handle, request.get_value()).await {
        Ok(()) => AttWriteResponseBuilder {}.into(),
        Err(error_code) => AttErrorResponseBuilder {
            opcode_in_error: AttOpcode::WRITE_REQUEST,
            handle_in_error: handle.into(),
            error_code,
        }
        .into(),
    }
}
