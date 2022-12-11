#[derive(Debug)]
struct BarData {
    x: [Foo; 7],
}
#[derive(Debug, Clone)]
pub struct BarPacket {
    bar: Arc<BarData>,
}
#[derive(Debug)]
pub struct BarBuilder {
    pub x: [Foo; 7],
}
impl BarData {
    fn conforms(bytes: &[u8]) -> bool {
        true
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        let x = std::array::from_fn(|_| Foo::from_u32(bytes.get_uint_le(3) as u32).unwrap());
        Ok(Self { x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        for elem in self.x {
            buffer.put_uint_le(elem.to_u24().unwrap() as u64, 3);
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        0
    }
}
impl Packet for BarPacket {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.bar.get_total_size());
        self.bar.write_to(&mut buffer);
        buffer.freeze()
    }
    fn to_vec(self) -> Vec<u8> {
        self.to_bytes().to_vec()
    }
}
impl From<BarPacket> for Bytes {
    fn from(packet: BarPacket) -> Self {
        packet.to_bytes()
    }
}
impl From<BarPacket> for Vec<u8> {
    fn from(packet: BarPacket) -> Self {
        packet.to_vec()
    }
}
impl BarPacket {
    pub fn parse(mut bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(BarData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<BarData>) -> std::result::Result<Self, &'static str> {
        let bar = root;
        Ok(Self { bar })
    }
    pub fn get_x(&self) -> &[Foo; 7] {
        &self.bar.as_ref().x
    }
}
impl BarBuilder {
    pub fn build(self) -> BarPacket {
        let bar = Arc::new(BarData { x: self.x });
        BarPacket::new(bar).unwrap()
    }
}
