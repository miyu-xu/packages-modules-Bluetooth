use std::future::Future;
use std::num::NonZeroU16;

use bytes::Buf;

use crate::uuid::{Uuid, UuidValue};

/// Vol 3, Part F - 3.2.1 Attribute type
#[derive(Debug, PartialEq, Eq)]
pub struct AttributeType16(pub u16);

impl AttributeType16 {
    pub fn to_le_bytes(&self) -> [u8; 2] {
        self.0.to_le_bytes()
    }
}

impl Into<crate::att_packets::Uuid16> for AttributeType16 {
    fn into(self) -> crate::att_packets::Uuid16 {
        crate::att_packets::Uuid16 { value: self.to_le_bytes() }
    }
}

impl Into<crate::att_packets::Uuid> for AttributeType16 {
    fn into(self) -> crate::att_packets::Uuid {
        /*let ref uuid: crate::att_packets::Uuid16 = self.into();
        // TODO: remove unwrap by changing PDL
        uuid.try_into().unwrap()*/
        crate::att_packets::Uuid { value: self.to_le_bytes().to_vec() }
    }
}

impl TryFrom<Uuid> for AttributeType16 {
    type Error = ();

    fn try_from(value: Uuid) -> std::result::Result<Self, Self::Error> {
        if let UuidValue::Uuid16(value) = value.get() {
            Ok(Self(value))
        } else {
            Err(())
        }
    }
}

/// Vol 3, Part F - 3.2.2 Attribute handle
/// Attribute handles on any given server shall have unique, non-zero values.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd)]
pub struct AttributeHandle(NonZeroU16);

impl AttributeHandle {
    pub const MIN: Self = Self(NonZeroU16::MIN);
    pub const MAX: Self = Self(NonZeroU16::MAX);

    pub fn new(value: u16) -> Option<Self> {
        NonZeroU16::new(value).map(Self)
    }

    pub fn checked_add(self, other: u16) -> Option<Self> {
        self.0.checked_add(other).map(Self)
    }

    pub fn checked_sub(self, other: u16) -> Option<Self> {
        self.0.get().checked_sub(other).and_then(Self::new)
    }

    pub fn to_le_bytes(&self) -> [u8; 2] {
        self.0.get().to_le_bytes()
    }
}

impl pdl_runtime::Packet for AttributeHandle {
    fn encoded_len(&self) -> usize {
        2
    }
    fn encode(
        &self,
        buf: &mut impl bytes::BufMut,
    ) -> std::result::Result<(), pdl_runtime::EncodeError> {
        buf.put_u16_le(self.0.get());
        Ok(())
    }
    fn decode(mut buf: &[u8]) -> std::result::Result<(Self, &[u8]), pdl_runtime::DecodeError> {
        if buf.remaining() < 2 {
            return Err(pdl_runtime::DecodeError::InvalidLengthError {
                obj: "AttributeHandle",
                wanted: 2,
                got: buf.remaining(),
            });
        }
        let inner = buf.get_u16_le();
        if let Some(value) = Self::new(inner) {
            Ok((value, buf))
        } else {
            Err(pdl_runtime::DecodeError::ConstraintOutOfBounds {
                field: "AttributeHandle",
                value: 0,
            })
        }
    }
}

/// Vol 3, Part F - 3.2.5 Attribute permissions
// enum AttributePermission {}
pub use crate::att_packets::ErrorCode;
pub use crate::att_packets::ErrorRsp;
pub use crate::att_packets::Opcode;

pub type Result<T> = std::result::Result<T, ErrorRsp>;

pub trait Request: 'static {
    type Response;
}

#[cfg(test)]
pub(crate) struct TestStep {
    request_type_name: &'static str,
    function: Box<dyn std::any::Any>,
}

/// Vol 3, Part F - 3.2.11 ATT bearers
pub enum Bearer {
    Channel,
    #[cfg(test)]
    Test {
        current_step: usize,
        steps: Vec<TestStep>,
    },
}

impl Bearer {
    pub fn new() -> Self {
        Self::Channel
    }

    #[cfg(test)]
    pub fn new_for_test() -> Self {
        Self::Test { current_step: 0, steps: vec![] }
    }

    #[cfg(test)]
    pub fn receive_transaction<R: Request>(
        mut self,
        function: fn(R) -> Result<R::Response>,
    ) -> Self {
        if let Self::Test { ref mut steps, .. } = self {
            steps.push(TestStep {
                request_type_name: std::any::type_name::<R>(),
                function: Box::new(function),
            });
        }

        self
    }

    #[cfg(test)]
    pub fn into_current_step(self) -> usize {
        match self {
            Self::Test { current_step, .. } => {
                std::mem::forget(self);
                current_step
            }
            Self::Channel => panic!("No steps for non test channels"),
        }
    }

    #[cfg(test)]
    pub fn is_complete(&self) -> bool {
        match self {
            Self::Test { current_step, steps, .. } => *current_step == steps.len(),
            Self::Channel => panic!("No steps for non test channels"),
        }
    }

    #[cfg(test)]
    pub fn reset(&mut self) {
        if let Self::Test { ref mut current_step, .. } = self {
            *current_step = 0;
        }
    }

    #[track_caller] // Get nicer location for test panics
    pub fn transaction<R: Request>(
        &mut self,
        request: R,
    ) -> impl Future<Output = Result<R::Response>> {
        match self {
            Self::Channel => todo!(),
            #[cfg(test)]
            Self::Test { steps, current_step } => {
                let Some(step) = steps.get(*current_step) else {
                    panic!("No more test steps (missing receive_transaction)");
                };
                *current_step += 1;
                if let Some(function) = step.function.downcast_ref::<fn(R) -> Result<R::Response>>()
                {
                    std::future::ready(function(request))
                } else {
                    panic!(
                        "Got {} but test expected {}",
                        std::any::type_name::<R>(),
                        step.request_type_name
                    )
                }
            }
        }
    }
}

#[cfg(test)]
impl Drop for Bearer {
    fn drop(&mut self) {
        // If the test is not currently failing
        if !std::thread::panicking() {
            if let Self::Test { steps, current_step } = self {
                assert_eq!(*current_step, steps.len(), "Not all steps have been executed")
            }
        }
    }
}

pub use crate::att_packets::AttributeValue;
pub use crate::att_packets::ExchangeMtuReq;
pub use crate::att_packets::ExchangeMtuRsp;
pub use crate::att_packets::ServiceAttributeValueUuid;
pub use crate::att_packets::ServiceAttributeValueUuid128;
pub use crate::att_packets::ServiceAttributeValueUuid16;

impl Request for ExchangeMtuReq {
    type Response = ExchangeMtuRsp;
}

pub use crate::att_packets::GroupAttributeData;
pub use crate::att_packets::ReadByGroupTypeReq;
pub use crate::att_packets::ReadByGroupTypeRsp;

impl Request for ReadByGroupTypeReq {
    type Response = ReadByGroupTypeRsp;
}

pub use crate::att_packets::FindByTypeValueReq;
pub use crate::att_packets::FindByTypeValueRsp;
pub use crate::att_packets::HandlesInformation;

impl Request for FindByTypeValueReq {
    type Response = FindByTypeValueRsp;
}

pub use crate::att_packets::AttributeData;
pub use crate::att_packets::ReadByTypeReq;
pub use crate::att_packets::ReadByTypeRsp;

impl Request for ReadByTypeReq {
    type Response = ReadByTypeRsp;
}

pub use crate::att_packets::ReadReq;
pub use crate::att_packets::ReadRsp;

impl Request for ReadReq {
    type Response = ReadRsp;
}

pub use crate::att_packets::FindInformationReq;
pub use crate::att_packets::FindInformationRsp;
pub use crate::att_packets::FindInformationRsp128;
pub use crate::att_packets::FindInformationRsp16;
pub use crate::att_packets::InformationData128;
pub use crate::att_packets::InformationData16;

impl Request for FindInformationReq {
    type Response = FindInformationRsp;
}
