use std::fmt;
use std::ops::Deref;

#[derive(Copy, Clone, PartialEq, Eq)]
pub struct Uuid(u128);

const BLUETOOTH_BASE_UUID: u128 = 0x00000000_0000_1000_8000_00805F9B34FB;

pub enum UuidValue {
    Uuid16(u16),
    Uuid128(u128),
}

pub enum UuidBytes {
    Uuid16([u8; 2]),
    Uuid128([u8; 16]),
}

impl Uuid {
    pub const fn uuid16(value: u16) -> Self {
        // Vol 3, Part B - 2.5.1 UUID
        //
        // 128_bit_value = 16_bit_value * 2^96 + Bluetooth_Base_UUID
        Self((value as u128) * 2u128.pow(96) + BLUETOOTH_BASE_UUID)
    }

    pub const fn uuid128(value: u128) -> Self {
        Self(value)
    }

    pub fn get(&self) -> UuidValue {
        if self.0 % 2u128.pow(96) == BLUETOOTH_BASE_UUID {
            let value = self.0 / 2u128.pow(96);

            if let Ok(value) = value.try_into() {
                UuidValue::Uuid16(value)
            } else {
                // Vol 3, Part G, 2.5.4 UUIDs:
                //
                // All 32-bit UUIDs shall be converted to 128-bit UUIDs when the UUID is
                // contained in an ATT PDU.
                UuidValue::Uuid128(self.0)
            }
        } else {
            UuidValue::Uuid128(self.0)
        }
    }

    #[cfg(test)]
    pub fn get_as_128(&self) -> u128 {
        self.0
    }

    pub fn to_le_bytes(&self) -> UuidBytes {
        match self.get() {
            UuidValue::Uuid16(value) => UuidBytes::Uuid16(value.to_le_bytes()),
            UuidValue::Uuid128(value) => UuidBytes::Uuid128(value.to_le_bytes()),
        }
    }
}

impl Into<crate::att::packets::Uuid> for Uuid {
    fn into(self) -> crate::att::packets::Uuid {
        crate::att::packets::Uuid { value: self.to_le_bytes().to_vec() }
    }
}

impl From<crate::att::packets::Uuid16> for Uuid {
    fn from(uuid: crate::att::packets::Uuid16) -> Self {
        Self::uuid16(u16::from_le_bytes(uuid.value))
    }
}

impl From<crate::att::packets::Uuid128> for Uuid {
    fn from(uuid: crate::att::packets::Uuid128) -> Self {
        Self::uuid128(u128::from_le_bytes(uuid.value))
    }
}

impl Deref for UuidBytes {
    type Target = [u8];

    fn deref(&self) -> &Self::Target {
        match self {
            UuidBytes::Uuid16(value) => value,
            UuidBytes::Uuid128(value) => value,
        }
    }
}

impl fmt::Debug for Uuid {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        fmt::Debug::fmt(&self.get(), f)
    }
}

impl fmt::Debug for UuidValue {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            UuidValue::Uuid16(value) => write!(f, "Uuid16(0x{:04X})", value),
            UuidValue::Uuid128(value) => {
                let [b0, b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12, b13, b14, b15] =
                    value.to_be_bytes();
                write!(
                    f,
                    "Uuid128(0x{:08X}_{:04X}_{:04X}_{:04X}_{:012X})",
                    u32::from_be_bytes([b0, b1, b2, b3]),
                    u16::from_be_bytes([b4, b5]),
                    u16::from_be_bytes([b6, b7]),
                    u16::from_be_bytes([b8, b9]),
                    u64::from_be_bytes([0, 0, b10, b11, b12, b13, b14, b15]),
                )
            }
        }
    }
}
