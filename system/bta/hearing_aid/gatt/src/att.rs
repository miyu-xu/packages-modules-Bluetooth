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

impl Into<crate::att_packets::Uuid> for AttributeType16 {
    fn into(self) -> crate::att_packets::Uuid {
        // TODO: remove unwrap by changing PDL
        (&crate::att_packets::Uuid16 { value: self.to_le_bytes() }).try_into().unwrap()
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
enum AttributePermission {}

enum Opcode {}

pub struct ErrorRsp {
    //pub request_opcode_in_error: Opcode,
    pub attribute_handle_in_error: AttributeHandle,
    pub error_code: ErrorCode,
}

/// Vol 3, Part F - Table 3.4: Error codes
#[derive(Debug)]
pub enum ErrorCode {
    /// The attribute handle given was not valid on this server.
    InvalidHandle = 0x01,
    /// The attribute cannot be read.
    ReadNotPermitted = 0x02,
    /// The attribute cannot be written
    WriteNotPermitted = 0x03,
    /// The attribute PDU was invalid.
    InvalidPDU = 0x04,
    /// The attribute requires authentication before it can be read or written.
    InsufficientAuthentication = 0x05,
    /// ATT Server does not support the request received from the client.
    RequestNotSupported = 0x06,
    /// No attribute found within the given attribute handle range.
    AttributeNotFound = 0x0A,
}

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

pub use crate::att_packets::ExchangeMtuReq;
pub use crate::att_packets::ExchangeMtuRsp;

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

#[derive(Debug, PartialEq)]
pub struct FindInformationReq {
    pub starting_handle: AttributeHandle,
    pub ending_handle: AttributeHandle,
}

impl Request for FindInformationReq {
    type Response = FindInformationRes;
}

pub struct InformationData16 {
    pub attribute_handle: AttributeHandle,
    pub uuid: u16,
}

pub struct InformationData128 {
    pub attribute_handle: AttributeHandle,
    pub uuid: u128,
}

pub enum FindInformationRes {
    FindInformationRes16 { information_data: Box<[InformationData16]> },
    FindInformationRes128 { information_data: Box<[InformationData128]> },
}
