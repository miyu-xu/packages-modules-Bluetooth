#[derive(Debug)]
struct FooData {
    x: u8,
    y: u16,
    z: u32,
}

#[derive(Debug, Clone)]
pub struct FooPacket {
    foo: Arc<FooData>,
}

#[derive(Debug)]
pub struct FooBuilder {
    pub x: u8,
    pub y: u16,
    pub z: u32,
}

impl FooData {
    fn conforms(bytes: &[u8]) -> bool {
        if bytes.len() < 4 {
            return false;
        }
        true
    }
    fn parse(bytes: &[u8]) -> Result<Self> {
        if bytes.len() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                field: "x".to_string(),
                wanted: 1,
                got: bytes.len(),
            });
        }
        let x = u8::from_le_bytes([bytes[0]]);
        let x = x & 0x3;
        if bytes.len() < 2 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                field: "y".to_string(),
                wanted: 2,
                got: bytes.len(),
            });
        }
        let y = u16::from_le_bytes([bytes[0], bytes[1]]);
        let y = (y << 2);
        let y = y & 0x1ff;
        if bytes.len() < 4 {
            return Err(Error::InvalidLengthError {
                obj: "Foo".to_string(),
                field: "z".to_string(),
                wanted: 4,
                got: bytes.len(),
            });
        }
        let z = u32::from_le_bytes([bytes[1], bytes[2], bytes[3], 0]);
        let z = (z << 3);
        let z = z & 0x1fffff;
        Ok(Self { x, y, z })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        let x = self.x;
        let x = x & 0x3;
        buffer[0..1].copy_from_slice(&x.to_le_bytes()[0..1]);
        let y = self.y;
        let y = y & 0x1ff;
        let y = (y << 2) | ((buffer[0] as u16) & 0x3);
        buffer[0..2].copy_from_slice(&y.to_le_bytes()[0..2]);
        let z = self.z;
        let z = z & 0x1fffff;
        let z = (z << 3) | ((buffer[1] as u32) & 0x7);
        buffer[1..4].copy_from_slice(&z.to_le_bytes()[0..3]);
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        let ret = 0;
        let ret = ret + 4;
        ret
    }
}

impl Packet for FooPacket {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::new();
        buffer.resize(self.foo.get_total_size(), 0);
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
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(FooData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<FooData>) -> std::result::Result<Self, &'static str> {
        let foo = root;
        Ok(Self { foo })
    }
    pub fn get_x(&self) -> u8 {
        self.foo.as_ref().x
    }
    pub fn get_y(&self) -> u16 {
        self.foo.as_ref().y
    }
    pub fn get_z(&self) -> u32 {
        self.foo.as_ref().z
    }
}

impl FooBuilder {
    pub fn build(self) -> FooPacket {
        let foo = Arc::new(FooData { x: self.x, y: self.y, z: self.z });
        FooPacket::new(foo).unwrap()
    }
}
