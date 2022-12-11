#[derive(Debug)]
struct FooData {
    padding: u8,
    x: Vec<u32>,
}
#[derive(Debug, Clone)]
pub struct Foo {
    foo: Arc<FooData>,
}
#[derive(Debug)]
pub struct FooBuilder {
    pub padding: u8,
    pub x: Vec<u32>,
}
impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        if bytes.remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                wanted: 1,
                got: bytes.remaining(),
            });
        }
        let chunk = bytes.get_u8();
        let x_size = (chunk & 0x1f);
        let padding = ((chunk >> 5) & 0x7);
        let x = (0..x_size).map(|_| bytes.get_uint_le(3) as u32).collect();
        Ok(Self { padding, x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        if self.padding > 0x7 {
            panic!("Invalid value for {}::{}: {} > {}", "Foo", "padding", self.padding, 0x7);
        }
        let value = (self.x.len() as u8) | ((self.padding as u8) << 5);
        buffer.put_u8(value);
        for elem in &self.x {
            buffer.put_uint_le(*elem as u64, 3);
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1 + self.x.len() * 3
    }
}
impl Packet for Foo {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.foo.get_total_size());
        self.foo.write_to(&mut buffer);
        buffer.freeze()
    }
    fn to_vec(self) -> Vec<u8> {
        self.to_bytes().to_vec()
    }
}
impl From<Foo> for Bytes {
    fn from(packet: Foo) -> Self {
        packet.to_bytes()
    }
}
impl From<Foo> for Vec<u8> {
    fn from(packet: Foo) -> Self {
        packet.to_vec()
    }
}
impl Foo {
    pub fn parse(mut bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(FooData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<FooData>) -> std::result::Result<Self, &'static str> {
        let foo = root;
        Ok(Self { foo })
    }
    pub fn get_padding(&self) -> u8 {
        self.foo.as_ref().padding
    }
    pub fn get_x(&self) -> &Vec<u32> {
        &self.foo.as_ref().x
    }
}
impl FooBuilder {
    pub fn build(self) -> Foo {
        let foo = Arc::new(FooData { padding: self.padding, x: self.x });
        Foo::new(foo).unwrap()
    }
}
