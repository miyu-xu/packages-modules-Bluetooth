#[derive(Debug)]
struct FooData {
    x: [u64; 7],
}
#[derive(Debug, Clone)]
pub struct FooPacket {
    foo: Arc<FooData>,
}
#[derive(Debug)]
pub struct FooBuilder {
    pub x: [u64; 7],
}
impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        let x = std::array::from_fn(|_| bytes.get_u64_le());
        Ok(Self { x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        for elem in self.x {
            buffer.put_u64_le(elem);
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
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
    pub fn get_x(&self) -> &[u64; 7] {
        &self.foo.as_ref().x
    }
}
impl FooBuilder {
    pub fn build(self) -> FooPacket {
        let foo = Arc::new(FooData { x: self.x });
        FooPacket::new(foo).unwrap()
    }
}
