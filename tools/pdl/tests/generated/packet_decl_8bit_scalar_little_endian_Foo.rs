#[derive(Debug)]
struct FooData {
    x: u8,
}
#[derive(Debug, Clone)]
pub struct Foo {
    foo: Arc<FooData>,
}
#[derive(Debug)]
pub struct FooBuilder {
    pub x: u8,
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
        let x = bytes.get_u8();
        Ok(Self { x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u8(self.x);
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1
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
    pub fn get_x(&self) -> u8 {
        self.foo.as_ref().x
    }
}
impl FooBuilder {
    pub fn build(self) -> Foo {
        let foo = Arc::new(FooData { x: self.x });
        Foo::new(foo).unwrap()
    }
}
