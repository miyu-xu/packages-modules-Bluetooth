use crate::{
    gatt::server::transaction_handler::HACK_child_to_opcode,
    packets::{AttBuilder, AttChild, Builder, OwnedAttView, OwnedPacket, Serializable},
};

pub fn build_att_view(child: impl Into<AttChild>) -> OwnedAttView {
    let child = child.into();
    let opcode = HACK_child_to_opcode(&child);
    let serialized = AttBuilder { _child_: child, opcode }.to_vec().unwrap();
    OwnedAttView::try_parse(serialized.into_boxed_slice()).unwrap()
}

pub fn build_view<'a, T: Builder>(builder: T) -> T::OwnedPacket {
    let buf = builder.to_vec().unwrap();
    T::OwnedPacket::try_parse(buf.into_boxed_slice()).unwrap()
}
