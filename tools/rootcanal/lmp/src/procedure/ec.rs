use num_bigint::{BigInt, Sign};
use num_integer::Integer;
use num_traits::{One, Signed, Zero};
use std::convert::TryInto;
use std::marker::PhantomData;

use crate::procedure::Context;

// Modular Inverse
fn mod_inv(x: &BigInt, m: &BigInt) -> Option<BigInt> {
    let egcd = x.extended_gcd(m);
    if !egcd.gcd.is_one() {
        None
    } else {
        Some(egcd.x % m)
    }
}

const P192_PRIVATE_KEY_SIZE: usize = 24;
const P256_PRIVATE_KEY_SIZE: usize = 32;
const P192_PUBLIC_KEY_SIZE: usize = 48;
const P256_PUBLIC_KEY_SIZE: usize = 64;

pub enum PublicKey {
    P192([u8; P192_PUBLIC_KEY_SIZE]),
    P256([u8; P256_PUBLIC_KEY_SIZE]),
}

impl PublicKey {
    pub fn new(size: usize) -> Option<Self> {
        match size {
            P192_PUBLIC_KEY_SIZE => Some(Self::P192([0; P192_PUBLIC_KEY_SIZE])),
            P256_PUBLIC_KEY_SIZE => Some(Self::P256([0; P256_PUBLIC_KEY_SIZE])),
            _ => panic!(),
        }
    }

    fn from_bytes(bytes: &[u8]) -> Option<Self> {
        match bytes.len() {
            P192_PUBLIC_KEY_SIZE => Some(PublicKey::P192(bytes.try_into().unwrap())),
            P256_PUBLIC_KEY_SIZE => Some(PublicKey::P256(bytes.try_into().unwrap())),
            _ => panic!(),
        }
    }

    pub fn as_slice(&self) -> &[u8] {
        match self {
            PublicKey::P192(inner) => inner,
            PublicKey::P256(inner) => inner,
        }
    }

    pub fn get_size(&self) -> usize {
        self.as_slice().len()
    }

    pub fn as_mut_slice(&mut self) -> &mut [u8] {
        match self {
            PublicKey::P192(inner) => inner,
            PublicKey::P256(inner) => inner,
        }
    }

    fn get_x(&self) -> BigInt {
        BigInt::from_signed_bytes_le(&self.as_slice()[0..self.get_size() / 2])
    }

    fn get_y(&self) -> BigInt {
        BigInt::from_signed_bytes_le(&self.as_slice()[self.get_size() / 2..self.get_size()])
    }

    fn to_point<Curve: EllipticCurve + Clone>(&self) -> Point<Curve> {
        Point::new(self.get_x(), self.get_y())
    }
}

pub enum PrivateKey {
    P192([u8; P192_PRIVATE_KEY_SIZE]),
    P256([u8; P256_PRIVATE_KEY_SIZE]),
}

pub enum DhKey {
    P192([u8; P192_PUBLIC_KEY_SIZE]),
    P256([u8; P256_PUBLIC_KEY_SIZE]),
}

impl DhKey {
    fn from_bytes(bytes: &[u8]) -> Option<Self> {
        match bytes.len() {
            P192_PUBLIC_KEY_SIZE => Some(DhKey::P192(bytes.try_into().unwrap())),
            P256_PUBLIC_KEY_SIZE => Some(DhKey::P256(bytes.try_into().unwrap())),
            _ => panic!(),
        }
    }
}

impl PrivateKey {
    pub fn generate_p192(ctx: &impl Context) -> Self {
        let mut key =
            BigInt::from_signed_bytes_le(&ctx.generate_random_bytes(P192_PRIVATE_KEY_SIZE));
        // Avoid zero and return 1 for tests
        key += BigInt::one();
        if key.is_negative() {
            key = -key;
        } else if key < BigInt::one() {
            key = BigInt::one();
        }
        let mut buf = key.to_signed_bytes_le();
        buf.resize(P192_PRIVATE_KEY_SIZE, 0);
        Self::P192(buf.try_into().unwrap())
    }

    pub fn generate_p256(ctx: &impl Context) -> Self {
        let mut key =
            BigInt::from_signed_bytes_le(&ctx.generate_random_bytes(P256_PRIVATE_KEY_SIZE));
        // Avoid zero and return 1 for tests
        if key.is_negative() {
            key = -key;
        } else if key < BigInt::one() {
            key = BigInt::one();
        }
        let mut buf = key.to_signed_bytes_le();
        buf.resize(P256_PRIVATE_KEY_SIZE, 0);
        Self::P256(buf.try_into().unwrap())
    }

    pub fn as_slice(&self) -> &[u8] {
        match self {
            PrivateKey::P192(inner) => inner,
            PrivateKey::P256(inner) => inner,
        }
    }

    fn to_bigint(&self) -> BigInt {
        BigInt::from_signed_bytes_le(self.as_slice())
    }

    pub fn derive(&self) -> PublicKey {
        let bytes = match self {
            PrivateKey::P192(_) => {
                Point::<P192r1>::generate_public_key(self.to_bigint()).to_bytes()
            }
            PrivateKey::P256(_) => {
                Point::<P256r1>::generate_public_key(self.to_bigint()).to_bytes()
            }
        }
        .unwrap();
        PublicKey::from_bytes(&bytes).unwrap()
    }

    pub fn shared_secret(&self, peer_public_key: PublicKey) -> DhKey {
        let bytes = match self {
            PrivateKey::P192(_) => {
                (peer_public_key.to_point::<P192r1>() * self.to_bigint()).to_bytes()
            }
            PrivateKey::P256(_) => {
                (peer_public_key.to_point::<P256r1>() * self.to_bigint()).to_bytes()
            }
        }
        .unwrap();
        DhKey::from_bytes(&bytes).unwrap()
    }
}

trait EllipticCurve {
    type Param: AsRef<[u8]>;
    const A: i32;
    const P: Self::Param;
    const G_X: Self::Param;
    const G_Y: Self::Param;
    const SIZE: usize;

    fn p() -> BigInt {
        BigInt::from_bytes_be(Sign::Plus, Self::P.as_ref())
    }
}

#[derive(Debug, Clone, PartialEq)]
struct P192r1;

impl EllipticCurve for P192r1 {
    type Param = [u8; 24];

    const A: i32 = -3;
    const P: Self::Param = *b"\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff\xfe\xff\xff\xff\xff\xff\xff\xff\xff";
    const G_X: Self::Param =
        *b"\x18\x8d\xa8\x0e\xb00\x90\xf6|\xbf \xebC\xa1\x88\x00\xf4\xff\n\xfd\x82\xff\x10\x12";
    const G_Y: Self::Param =
        *b"\x07\x19+\x95\xff\xc8\xdaxc\x10\x11\xedk$\xcd\xd5s\xf9w\xa1\x1eyH\x11";
    const SIZE: usize = P192_PRIVATE_KEY_SIZE;
}

#[derive(Debug, Clone, PartialEq)]
struct P256r1;

impl EllipticCurve for P256r1 {
    type Param = [u8; 32];

    const A: i32 = -3;
    const P: Self::Param = *b"\xff\xff\xff\xff\x00\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff\xff";
    const G_X: Self::Param = *b"k\x17\xd1\xf2\xe1,BG\xf8\xbc\xe6\xe5c\xa4@\xf2w\x03}\x81-\xeb3\xa0\xf4\xa19E\xd8\x98\xc2\x96";
    const G_Y: Self::Param =
        *b"O\xe3B\xe2\xfe\x1a\x7f\x9b\x8e\xe7\xebJ|\x0f\x9e\x16+\xce3Wk1^\xce\xcb\xb6@h7\xbfQ\xf5";
    const SIZE: usize = P256_PRIVATE_KEY_SIZE;
}

#[derive(Debug, Clone, PartialEq)]
enum Point<Curve>
where
    Curve: Clone,
{
    Infinite { _curve: PhantomData<Curve> },
    Finite { x: BigInt, y: BigInt, _curve: PhantomData<Curve> },
}

impl<Curve> Point<Curve>
where
    Curve: EllipticCurve + std::clone::Clone,
{
    fn o() -> Self {
        Point::Infinite { _curve: PhantomData }
    }

    fn generate_public_key(private_key: BigInt) -> Self {
        let g = Point::Finite {
            x: BigInt::from_bytes_be(Sign::Plus, Curve::G_X.as_ref()),
            y: BigInt::from_bytes_be(Sign::Plus, Curve::G_Y.as_ref()),
            _curve: PhantomData,
        };
        g * private_key
    }

    fn new(x: BigInt, y: BigInt) -> Self {
        Point::Finite { x, y, _curve: PhantomData }
    }

    #[cfg(test)]
    fn get_x(&self) -> Option<BigInt> {
        match self {
            Point::Infinite { _curve: _ } => None,
            Point::Finite { x, y: _, _curve: _ } => Some(x.clone()),
        }
    }

    fn to_bytes(&self) -> Option<Vec<u8>> {
        match self {
            Point::Infinite { _curve: _ } => None,
            Point::Finite { x, y, _curve: _ } => {
                let mut x = x.to_signed_bytes_le();
                x.resize(Curve::SIZE, 0);
                let mut y = y.to_signed_bytes_le();
                y.resize(Curve::SIZE, 0);
                x.append(&mut y);
                Some(x)
            }
        }
    }
}

impl<Curve> std::ops::Add<Point<Curve>> for Point<Curve>
where
    Curve: EllipticCurve + std::clone::Clone,
{
    type Output = Point<Curve>;

    fn add(self, rhs: Point<Curve>) -> Self::Output {
        let p = &Curve::p();
        // P + O = O + P = P
        let (x1, y1, x2, y2) = match (&self, &rhs) {
            (Point::Infinite { _curve: _ }, Point::Infinite { _curve: _ }) => {
                return Self::o();
            }
            (Point::Infinite { _curve: _ }, Point::Finite { _curve: _, x: _, y: _ }) => {
                return rhs;
            }
            (Point::Finite { _curve: _, x: _, y: _ }, Point::Infinite { _curve: _ }) => {
                return self;
            }
            (
                Point::Finite { _curve: _, x: x1, y: y1 },
                Point::Finite { _curve: _, x: x2, y: y2 },
            ) => (x1, y1, x2, y2),
        };
        // P + (-P) = O
        if x1 == x2 && y1 == &(-y2) {
            return Self::o();
        }
        // d(x^3 + ax + b) / dx = (3x^2 + a) / 2y
        let slope = if x1 == x2 {
            (&(3 * x1.pow(2) + Curve::A) * &mod_inv(&(2 * y1), p).unwrap()) % p
        } else {
            // dy/dx = (y2 - y1) / (x2 - x1)
            (&(y2 - y1) * &mod_inv(&(x2 - x1), p).unwrap()) % p
        };
        // Solving (x-p)(x-q)(x-r) = x^3 + ax + b
        // => x = d^2 - x1 - x2
        let x = (slope.pow(2) - x1 - x2) % p;
        let y = (slope * (x1 - &x) - y1) % p;
        Point::new(x, y)
    }
}

impl<Curve> std::ops::Mul<BigInt> for Point<Curve>
where
    Curve: EllipticCurve + std::clone::Clone,
{
    type Output = Point<Curve>;

    fn mul(self, rhs: BigInt) -> Self::Output {
        let mut addend = self;
        let mut result = Point::o();
        let mut i = rhs;

        // O(logN) double-and-add multiplication
        while !i.is_zero() {
            if i.is_odd() {
                result = result + addend.clone();
            }
            addend = addend.clone() + addend;
            i /= 2;
        }
        result
    }
}

#[cfg(test)]
mod tests {
    use crate::procedure::ec::*;
    use num_bigint::BigInt;

    // Private A, Private B, Public A(x), DHKey
    const P192_TEST_CASES: [(&[u8; 48], &[u8; 48], &[u8; 48], &[u8; 48]); 4] = [
        (
            b"07915f86918ddc27005df1d6cf0c142b625ed2eff4a518ff",
            b"1e636ca790b50f68f15d8dbe86244e309211d635de00e16d",
            b"15207009984421a6586f9fc3fe7e4329d2809ea51125f8ed",
            b"fb3ba2012c7e62466e486e229290175b4afebc13fdccee46",
        ),
        (
            b"52ec1ca6e0ec973c29065c3ca10be80057243002f09bb43e",
            b"57231203533e9efe18cc622fd0e34c6a29c6e0fa3ab3bc53",
            b"45571f027e0d690795d61560804da5de789a48f94ab4b07e",
            b"a20a34b5497332aa7a76ab135cc0c168333be309d463c0c0",
        ),
        (
            b"00a0df08eaf51e6e7be519d67c6749ea3f4517cdd2e9e821",
            b"2bf5e0d1699d50ca5025e8e2d9b13244b4d322a328be1821",
            b"2ed35b430fa45f9d329186d754eeeb0495f0f653127f613d",
            b"3b3986ba70790762f282a12a6d3bcae7a2ca01e25b87724e",
        ),
        (
            b"030a4af66e1a4d590a83e0284fca5cdf83292b84f4c71168",
            b"12448b5c69ecd10c0471060f2bf86345c5e83c03d16bae2c",
            b"f24a6899218fa912e7e4a8ba9357cb8182958f9fa42c968c",
            b"4a78f83fba757c35f94abea43e92effdd2bc700723c61939",
        ),
    ];

    const P256_TEST_CASES: [(&[u8; 64], &[u8; 64], &[u8; 64], &[u8; 64]); 2] = [
        (
            b"3f49f6d4a3c55f3874c9b3e3d2103f504aff607beb40b7995899b8a6cd3c1abd",
            b"55188b3d32f6bb9a900afcfbeed4e72a59cb9ac2f19d7cfb6b4fdd49f47fc5fd",
            b"20b003d2f297be2c5e2c83a7e9f9a5b9eff49111acf4fddbcc0301480e359de6",
            b"ec0234a357c8ad05341010a60a397d9b99796b13b4f866f1868d34f373bfa698",
        ),
        (
            b"06a516693c9aa31a6084545d0c5db641b48572b97203ddffb7ac73f7d0457663",
            b"529aa0670d72cd6497502ed473502b037e8803b5c60829a5a3caa219505530ba",
            b"2c31a47b5779809ef44cb5eaaf5c3e43d5f8faad4a8794cb987e9b03745c78dd",
            b"ab85843a2f6d883f62e5684b38e307335fe6e1945ecd19604105c6f23221eb69",
        ),
    ];

    #[test]
    fn p192() {
        for test_case in P192_TEST_CASES {
            let priv_a = BigInt::parse_bytes(test_case.0, 16).unwrap();
            let priv_b = BigInt::parse_bytes(test_case.1, 16).unwrap();
            let pub_a = Point::<P192r1>::generate_public_key(priv_a.clone());
            let pub_b = Point::<P192r1>::generate_public_key(priv_b.clone());
            assert_eq!(
                pub_a.clone().get_x().unwrap(),
                BigInt::parse_bytes(test_case.2, 16).unwrap()
            );
            let shared = pub_a.clone() * priv_b.clone();
            assert_eq!(shared.get_x().unwrap(), BigInt::parse_bytes(test_case.3, 16).unwrap());
            assert_eq!((pub_a * priv_b).get_x().unwrap(), (pub_b * priv_a).get_x().unwrap());
        }
    }

    #[test]
    fn p256() {
        for test_case in P256_TEST_CASES {
            let priv_a = BigInt::parse_bytes(test_case.0, 16).unwrap();
            let priv_b = BigInt::parse_bytes(test_case.1, 16).unwrap();
            let pub_a = Point::<P256r1>::generate_public_key(priv_a.clone());
            let pub_b = Point::<P256r1>::generate_public_key(priv_b.clone());
            assert_eq!(
                pub_a.clone().get_x().unwrap(),
                BigInt::parse_bytes(test_case.2, 16).unwrap()
            );
            let shared = pub_a.clone() * priv_b.clone();
            assert_eq!(shared.get_x().unwrap(), BigInt::parse_bytes(test_case.3, 16).unwrap());
            assert_eq!((pub_a * priv_b).get_x().unwrap(), (pub_b * priv_a).get_x().unwrap());
        }
    }
}
