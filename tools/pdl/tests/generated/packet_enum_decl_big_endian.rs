#[derive(Debug)]
struct Packet_Enum_FieldData {
    a: Enum7,
    c: u64,
}

#[derive(Debug, Clone)]
pub struct Packet_Enum_FieldPacket {
    packet_enum_field: Arc<Packet_Enum_FieldData>,
}

#[derive(Debug)]
pub struct Packet_Enum_FieldBuilder {
    pub a: Enum7,
    pub c: u64,
}

impl Packet_Enum_FieldData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 8
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        if bytes.remaining() < 8 {
            return Err(Error::InvalidLengthError {
                obj: "Packet_Enum_Field".to_string(),
                wanted: 8,
                got: bytes.remaining(),
            });
        }
        let chunk = bytes.get_u64();
        let a = Enum7::from_u64((chunk & 0x7f)).unwrap();
        let c = ((chunk >> 7) & 0x1ffffffffffffff);
        Ok(Self { a, c })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        let chunk = 0;
        let chunk = chunk | (self.a.to_u64().unwrap() & 0x7f);
        let chunk = chunk | ((self.c & 0x1ffffffffffffff) << 7);
        buffer.put_u64(chunk);
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        8
    }
}

impl Packet for Packet_Enum_FieldPacket {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.packet_enum_field.get_total_size());
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
    pub fn parse(mut bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(Packet_Enum_FieldData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<Packet_Enum_FieldData>) -> std::result::Result<Self, &'static str> {
        let packet_enum_field = root;
        Ok(Self { packet_enum_field })
    }
    pub fn get_a(&self) -> Enum7 {
        self.packet_enum_field.as_ref().a
    }
    pub fn get_c(&self) -> u64 {
        self.packet_enum_field.as_ref().c
    }
}

impl Packet_Enum_FieldBuilder {
    pub fn build(self) -> Packet_Enum_FieldPacket {
        let packet_enum_field = Arc::new(Packet_Enum_FieldData { a: self.a, c: self.c });
        Packet_Enum_FieldPacket::new(packet_enum_field).unwrap()
    }
}
