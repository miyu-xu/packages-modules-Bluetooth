#[derive(Debug)]
struct FooData {
    a: [u8; 4],
    b: [u16; 5],
    c: [u32; 6],
}

#[derive(Debug, Clone)]
pub struct FooPacket {
    foo: Arc<FooData>,
}

#[derive(Debug)]
pub struct FooBuilder {
    pub a: [u8; 4],
    pub b: [u16; 5],
    pub c: [u32; 6],
}

impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 32
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        let mut a = [0; 4];
        for i in 0..4 {
            a[i] = bytes.get_u8();
        }
        let mut b = [0; 5];
        for i in 0..5 {
            b[i] = bytes.get_u16();
        }
        let mut c = [0; 6];
        for i in 0..6 {
            c[i] = bytes.get_uint(3) as u32;
        }
        Ok(Self { a, b, c })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        for i in 0..4 {
            let a = self.a[i];
            let a = a | self.xxx;
            buffer.put_u8(a);
        }
        for i in 0..5 {
            let b = self.b[i];
            let b = b | self.xxx;
            buffer.put_u16(b);
        }
        for i in 0..6 {
            let c = self.c[i];
            let c = c | (self.xxx & 0xffffff);
            buffer.put_uint(c as u64, 3);
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        32
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
    pub fn get_a(&self) -> &[u8; 4] {
        &self.foo.as_ref().a
    }
    pub fn get_b(&self) -> &[u16; 5] {
        &self.foo.as_ref().b
    }
    pub fn get_c(&self) -> &[u32; 6] {
        &self.foo.as_ref().c
    }
}

impl FooBuilder {
    pub fn build(self) -> FooPacket {
        let foo = Arc::new(FooData { a: self.a, b: self.b, c: self.c });
        FooPacket::new(foo).unwrap()
    }
}
