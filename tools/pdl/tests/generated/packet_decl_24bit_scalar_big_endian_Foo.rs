#[derive(Debug)]
struct FooData {
    x: u32,
}
#[derive(Debug, Clone)]
pub struct FooPacket {
    foo: Arc<FooData>,
}
#[derive(Debug)]
pub struct FooBuilder {
    pub x: u32,
}
impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 3
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        if bytes.remaining() < 3 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                wanted: 3,
                got: bytes.remaining(),
            });
        }
        let x = bytes.get_uint(3) as u32;
        Ok(Self { x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        if self.x > 0xffffff {
            panic!("Invalid value for {}::{}: {} > {}", "Foo", "x", self.x, 0xffffff);
        }
        buffer.put_uint(self.x as u64, 3);
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3
    }
}
impl Packet for FooPacket {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.foo.get_total_size());
        self.foo.write_to(&mut buffer);
        buffer.freeze()
    }
    fn to_vec(self) -> Vec<u8> {
        self.to_bytes().to_vec()
    }
}
impl From<FooPacket> for Bytes {
    fn from(packet: FooPacket) -> Self {
        packet.to_bytes()
    }
}
impl From<FooPacket> for Vec<u8> {
    fn from(packet: FooPacket) -> Self {
        packet.to_vec()
    }
}
impl FooPacket {
    pub fn parse(mut bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(FooData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<FooData>) -> std::result::Result<Self, &'static str> {
        let foo = root;
        Ok(Self { foo })
    }
    pub fn get_x(&self) -> u32 {
        self.foo.as_ref().x
    }
}
impl FooBuilder {
    pub fn build(self) -> FooPacket {
        let foo = Arc::new(FooData { x: self.x });
        FooPacket::new(foo).unwrap()
    }
}
