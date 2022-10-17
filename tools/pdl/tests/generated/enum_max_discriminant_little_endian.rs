#[derive(FromPrimitive, ToPrimitive, Debug, Hash, Eq, PartialEq, Clone, Copy)]
#[repr(u64)]
pub enum MaxDiscriminantEnum {
    Max = 0xffffffffffffffff,
}
