#[derive(Debug)]
struct FooData {
    a: [Enum8; 4],
    b: [Enum24; 5],
}

#[derive(Debug, Clone)]
pub struct FooPacket {
    foo: Arc<FooData>,
}

#[derive(Debug)]
pub struct FooBuilder {
    pub a: [Enum8; 4],
    pub b: [Enum24; 5],
}

impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 19
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        let mut a = [0; 4];
        for i in 0..4 {
            a[i] = bytes.get_u8();
        }
        let mut b = [0; 5];
        for i in 0..5 {
            b[i] = bytes.get_uint_le(3) as u32;
        }
        Ok(Self { a, b })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        for i in 0..4 {
            let a = self.a[i];
            let a = a | self.a.to_u8().unwrap();
            buffer.put_u8(a);
        }
        for i in 0..5 {
            let b = self.b[i];
            let b = b | (self.b.to_u32().unwrap() & 0xffffff);
            buffer.put_uint_le(b as u64, 3);
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        19
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
    pub fn get_a(&self) -> &[Enum8; 4] {
        &self.foo.as_ref().a
    }
    pub fn get_b(&self) -> &[Enum24; 5] {
        &self.foo.as_ref().b
    }
}

impl FooBuilder {
    pub fn build(self) -> FooPacket {
        let foo = Arc::new(FooData { a: self.a, b: self.b });
        FooPacket::new(foo).unwrap()
    }
}
