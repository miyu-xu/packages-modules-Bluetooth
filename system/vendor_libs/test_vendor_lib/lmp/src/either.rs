use std::convert::TryFrom;

use crate::packets::{hci, lmp};

pub enum Either<L, R> {
    Left(L),
    Right(R),
}

impl<L, R> TryFrom<hci::CommandPacket> for Either<L, R>
where
    L: TryFrom<hci::CommandPacket>,
    R: TryFrom<hci::CommandPacket>,
{
    type Error = ();

    fn try_from(value: hci::CommandPacket) -> Result<Self, Self::Error> {
        let left = L::try_from(value.clone());
        if let Ok(left) = left {
            return Ok(Either::Left(left));
        }
        let right = R::try_from(value);
        if let Ok(right) = right {
            return Ok(Either::Right(right));
        }
        Err(())
    }
}

impl<L, R> TryFrom<lmp::PacketPacket> for Either<L, R>
where
    L: TryFrom<lmp::PacketPacket>,
    R: TryFrom<lmp::PacketPacket>,
{
    type Error = ();

    fn try_from(value: lmp::PacketPacket) -> Result<Self, Self::Error> {
        let left = L::try_from(value.clone());
        if let Ok(left) = left {
            return Ok(Either::Left(left));
        }
        let right = R::try_from(value);
        if let Ok(right) = right {
            return Ok(Either::Right(right));
        }
        Err(())
    }
}
