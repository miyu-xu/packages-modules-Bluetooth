use num_bigint::BigInt;
use num_integer::Integer;
use num_traits::{One, Zero};
use std::fmt;

pub struct EcPoint {
    pub x: BigInt,
    pub y: BigInt,
}

impl EcPoint {
    pub fn zero() -> EcPoint {
        EcPoint { x: BigInt::zero(), y: BigInt::zero() }
    }

    pub fn is_zero(&self) -> bool {
        self.x.is_zero() && self.y.is_zero()
    }
}

impl Clone for EcPoint {
    fn clone(&self) -> EcPoint {
        EcPoint { x: self.x.clone(), y: self.y.clone() }
    }
}

impl PartialEq for EcPoint {
    fn eq(&self, other: &Self) -> bool {
        self.x == other.x && self.y == other.y
    }
}

impl fmt::Display for EcPoint {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "({}, {})", self.x, self.y)
    }
}

// Modular Inverse
fn mod_inv(x: BigInt, m: BigInt) -> Option<BigInt> {
    let egcd = x.extended_gcd(&m);
    if !egcd.gcd.is_one() {
        None
    } else {
        Some(egcd.x % m)
    }
}

// 0xfffffffffffffffffffffffffffffffeffffffffffffffff
const SECP192R1_P: &[u8; 24] = &[
    255, 255, 255, 255, 255, 255, 255, 255, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
    255, 255, 255, 255, 255,
];
const SECP192R1_A: i32 = -3;
// (0x188da80eb03090f67cbf20eb43a18800f4ff0afd82ff1012, 0x07192b95ffc8da78631011ed6b24cdd573f977a11e794811)
const SECP192R1_G_X: &[u8; 24] = &[
    18, 16, 255, 130, 253, 10, 255, 244, 0, 136, 161, 67, 235, 32, 191, 124, 246, 144, 48, 176, 14,
    168, 141, 24,
];
const SECP192R1_G_Y: &[u8; 24] = &[
    17, 72, 121, 30, 161, 119, 249, 115, 213, 205, 36, 107, 237, 17, 16, 99, 120, 218, 200, 255,
    149, 43, 25, 7,
];

// 0xffffffff00000001000000000000000000000000ffffffffffffffffffffffff
const SECP256R1_P: &[u8; 32] = &[
    255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    1, 0, 0, 0, 255, 255, 255, 255,
];
const SECP256R1_A: i32 = -3;
// (0x6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296, 0x4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5)
const SECP256R1_G_X: &[u8; 32] = &[
    150, 194, 152, 216, 69, 57, 161, 244, 160, 51, 235, 45, 129, 125, 3, 119, 242, 64, 164, 99,
    229, 230, 188, 248, 71, 66, 44, 225, 242, 209, 23, 107,
];
const SECP256R1_G_Y: &[u8; 32] = &[
    245, 81, 191, 55, 104, 64, 182, 203, 206, 94, 49, 107, 87, 51, 206, 43, 22, 158, 15, 124, 74,
    235, 231, 142, 155, 127, 26, 254, 226, 66, 227, 79,
];

// E: y^2 = x^3 + a * x + b (mod p)
pub struct EcGroup {
    p: BigInt,
    a: BigInt,
    // Generator point
    g: EcPoint,
}

impl EcGroup {
    pub fn p192() -> EcGroup {
        EcGroup {
            p: BigInt::from_signed_bytes_le(SECP192R1_P),
            a: BigInt::from(SECP192R1_A),
            g: EcPoint {
                x: BigInt::from_signed_bytes_le(SECP192R1_G_X),
                y: BigInt::from_signed_bytes_le(SECP192R1_G_Y),
            },
        }
    }

    pub fn p256() -> EcGroup {
        EcGroup {
            p: BigInt::from_signed_bytes_le(SECP256R1_P),
            a: BigInt::from(SECP256R1_A),
            g: EcPoint {
                x: BigInt::from_signed_bytes_le(SECP256R1_G_X),
                y: BigInt::from_signed_bytes_le(SECP256R1_G_Y),
            },
        }
    }

    fn add(&self, p1: EcPoint, p2: EcPoint) -> EcPoint {
        if p1.is_zero() {
            return p2;
        }
        if p2.is_zero() {
            return p1;
        }
        if p1.x == p2.x && p1.y != p2.y {
            return EcPoint::zero();
        }
        let l = if p1.x == p2.x {
            (3 * p1.x.pow(2) + self.a.clone()) * mod_inv(2 * p1.y.clone(), self.p.clone()).unwrap()
                % self.p.clone()
        } else {
            (p2.y - p1.y.clone()) * mod_inv(p2.x.clone() - p1.x.clone(), self.p.clone()).unwrap()
                % self.p.clone()
        };
        // else:
        let x = (l.pow(2) - p1.x.clone() - p2.x) % self.p.clone();
        let y = (l * (p1.x.clone() - x.clone()) - p1.y) % self.p.clone();
        EcPoint { x, y }
    }

    fn mul(&self, p: EcPoint, n: BigInt) -> EcPoint {
        let mut addend = p;
        let mut result = EcPoint::zero();
        let mut i = n;

        while !i.is_zero() {
            if i.bit(0) {
                result = self.add(result, addend.clone());
            }
            addend = self.add(addend.clone(), addend);
            i >>= 1;
        }
        result
    }

    pub fn generate(&self, private_key: BigInt) -> EcPoint {
        self.mul(self.g.clone(), private_key)
    }

    pub fn shared_secret(&self, private_key: BigInt, public_key: EcPoint) -> EcPoint {
        self.mul(public_key, private_key)
    }
}

#[cfg(test)]
mod tests {

    #[test]
    fn p192() {
        let group = EcGroup::p192();
        let priv_a =
            BigInt::parse_bytes(b"07915f86918ddc27005df1d6cf0c142b625ed2eff4a518ff", 16).unwrap();
        let priv_b =
            BigInt::parse_bytes(b"1e636ca790b50f68f15d8dbe86244e309211d635de00e16d", 16).unwrap();
        let pub_a = group.generate(priv_a);
        assert!(
            pub_a.x
                == BigInt::parse_bytes(b"15207009984421a6586f9fc3fe7e4329d2809ea51125f8ed", 16)
                    .unwrap()
        );
        assert!(
            pub_a.y
                == BigInt::parse_bytes(b"b09d42b81bc5bd009f79e4b59dbbaa857fca856fb9f7ea25", 16)
                    .unwrap()
        );
        let shared = group.shared_secret(priv_b, pub_a);
        assert!(
            shared.x
                == BigInt::parse_bytes(b"fb3ba2012c7e62466e486e229290175b4afebc13fdccee46", 16)
                    .unwrap()
        );
    }

    fn p256() {
        let group = EcGroup::p192();
        let priv_a = BigInt::parse_bytes(
            b"3f49f6d4a3c55f3874c9b3e3d2103f504aff607beb40b7995899b8a6cd3c1abd",
            16,
        )
        .unwrap();
        let priv_b = BigInt::parse_bytes(
            b"55188b3d32f6bb9a900afcfbeed4e72a59cb9ac2f19d7cfb6b4fdd49f47fc5fd",
            16,
        )
        .unwrap();
        let pub_a = group.generate(priv_a);
        assert!(
            pub_a.x
                == BigInt::parse_bytes(
                    b"20b003d2f297be2c5e2c83a7e9f9a5b9eff49111acf4fddbcc0301480e359de6",
                    16
                )
                .unwrap()
        );
        assert!(
            pub_a.y
                == BigInt::parse_bytes(
                    b"dc809c49652aeb6d63329abf5a52155c766345c28fed3024741c8ed01589d28b",
                    16
                )
                .unwrap()
        );
        let shared = group.shared_secret(priv_b, pub_a);
        assert!(
            shared.x
                == BigInt::parse_bytes(
                    b"ec0234a357c8ad05341010a60a397d9b99796b13b4f866f1868d34f373bfa698",
                    16
                )
                .unwrap()
        );
    }
}
