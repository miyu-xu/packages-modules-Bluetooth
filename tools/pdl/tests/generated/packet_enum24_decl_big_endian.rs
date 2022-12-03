#[derive(Debug)]
struct Packet_Enum_FieldData {
    a: Enum24,
}

#[derive(Debug, Clone)]
pub struct Packet_Enum_FieldPacket {
    packet_enum_field: Arc<Packet_Enum_FieldData>,
}

#[derive(Debug)]
pub struct Packet_Enum_FieldBuilder {
    pub a: Enum24,
}

impl Packet_Enum_FieldData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 3
    }
    fn parse(bytes: &[u8]) -> Result<Self> {
        if bytes.len() < 3 {
            return Err(Error::InvalidLengthError {
                obj: "Packet_Enum_Field".to_string(),
                wanted: 3,
                got: bytes.len(),
            });
        }
        let a = u32::from_be_bytes([0, bytes[0], bytes[1], bytes[2]]);
        let a = Enum24::from_u32((a & 0xffffff)).unwrap();
        Ok(Self { a })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        let a = 0;
        let a = a | (self.a.to_u32().unwrap() & 0xffffff);
        buffer[0..3].copy_from_slice(&a.to_be_bytes()[0..3]);
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        3
    }
}

impl Packet for Packet_Enum_FieldPacket {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::new();
        buffer.resize(self.packet_enum_field.get_total_size(), 0);
        self.packet_enum_field.write_to(&mut buffer);
        buffer.freeze()
    }
    fn to_vec(self) -> Vec<u8> {
        self.to_bytes().to_vec()
    }
}
impl From<Packet_Enum_FieldPacket> for Bytes {
    fn from(packet: Packet_Enum_FieldPacket) -> Self {
        packet.to_bytes()
    }
}
impl From<Packet_Enum_FieldPacket> for Vec<u8> {
    fn from(packet: Packet_Enum_FieldPacket) -> Self {
        packet.to_vec()
    }
}

impl Packet_Enum_FieldPacket {
    pub fn parse(bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(Packet_Enum_FieldData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<Packet_Enum_FieldData>) -> std::result::Result<Self, &'static str> {
        let packet_enum_field = root;
        Ok(Self { packet_enum_field })
    }
    pub fn get_a(&self) -> Enum24 {
        self.packet_enum_field.as_ref().a
    }
}

impl Packet_Enum_FieldBuilder {
    pub fn build(self) -> Packet_Enum_FieldPacket {
        let packet_enum_field = Arc::new(Packet_Enum_FieldData { a: self.a });
        Packet_Enum_FieldPacket::new(packet_enum_field).unwrap()
    }
}
