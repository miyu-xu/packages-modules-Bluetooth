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

const SECP192R1_P: &[u8; 48] = b"fffffffffffffffffffffffffffffffeffffffffffffffff";
const SECP192R1_A: i32 = -3;
const SECP192R1_B: &[u8; 48] = b"64210519e59c80e70fa7e9ab72243049feb8deecc146b9b1";
const SECP192R1_G_X: &[u8; 48] = b"188da80eb03090f67cbf20eb43a18800f4ff0afd82ff1012";
const SECP192R1_G_Y: &[u8; 48] = b"07192b95ffc8da78631011ed6b24cdd573f977a11e794811";

const SECP256R1_P: &[u8; 48] = b"fffffffffffffffffffffffffffffffeffffffffffffffff";
const SECP256R1_A: i32 = -3;
const SECP256R1_B: &[u8; 48] = b"64210519e59c80e70fa7e9ab72243049feb8deecc146b9b1";
const SECP256R1_G_X: &[u8; 48] = b"188da80eb03090f67cbf20eb43a18800f4ff0afd82ff1012";
const SECP256R1_G_Y: &[u8; 48] = b"07192b95ffc8da78631011ed6b24cdd573f977a11e794811";

// E: y^2 = x^3 + a * x + b (mod p)
pub struct EcGroup {
    p: BigInt,
    a: BigInt,
    b: BigInt,
    // Generator point
    g: EcPoint,
}

impl EcGroup {
    pub fn p192() -> EcGroup {
        EcGroup {
            p: BigInt::parse_bytes(SECP192R1_P, 16).unwrap(),
            a: BigInt::from(SECP192R1_A),
            b: BigInt::parse_bytes(SECP192R1_B, 16).unwrap(),
            g: EcPoint {
                x: BigInt::parse_bytes(SECP192R1_G_X, 16).unwrap(),
                y: BigInt::parse_bytes(SECP192R1_G_Y, 16).unwrap(),
            },
        }
    }

    pub fn p256() -> EcGroup {
        EcGroup {
            p: BigInt::parse_bytes(SECP256R1_P, 16).unwrap(),
            a: BigInt::from(SECP256R1_A),
            b: BigInt::parse_bytes(SECP256R1_B, 16).unwrap(),
            g: EcPoint {
                x: BigInt::parse_bytes(SECP256R1_G_X, 16).unwrap(),
                y: BigInt::parse_bytes(SECP256R1_G_Y, 16).unwrap(),
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
