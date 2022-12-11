#[derive(Debug)]
struct BarData {
    x: Vec<Foo>,
}
#[derive(Debug, Clone)]
pub struct Bar {
    bar: Arc<BarData>,
}
#[derive(Debug)]
pub struct BarBuilder {
    pub x: Vec<Foo>,
}
impl BarData {
    fn conforms(bytes: &[u8]) -> bool {
        bytes.len() >= 1
    }
    fn parse(mut bytes: &[u8]) -> Result<Self> {
        if bytes.remaining() < 1 {
            return Err(Error::InvalidLengthError {
                obj: "Bar".to_string(),
                wanted: 1,
                got: bytes.remaining(),
            });
        }
        let x_size = bytes.get_u8();
        if bytes.remaining() < x_size {
            panic!(
                "Invalid packet size for {}::{}: expected {} bytes, got {}",
                "Bar",
                "x",
                bytes.remaining(),
                x_size
            );
        }
        let array_span = bytes.split_to(x_size);
        let x = Vec::new();
        while !array_span.is_empty() {
            parse_array_element_dynamic()
        }
        Ok(Self { x })
    }
    fn write_to(&self, buffer: &mut BytesMut) {
        buffer.put_u8(self.x.len());
        for elem in &self.x {
            buffer.put(elem.serialize());
        }
    }
    fn get_total_size(&self) -> usize {
        self.get_size()
    }
    fn get_size(&self) -> usize {
        1 + self.x.iter().map(|elem| elem.get_size()).sum::<usize>()
    }
}
impl Packet for Bar {
    fn to_bytes(self) -> Bytes {
        let mut buffer = BytesMut::with_capacity(self.bar.get_total_size());
        self.bar.write_to(&mut buffer);
        buffer.freeze()
    }
    fn to_vec(self) -> Vec<u8> {
        self.to_bytes().to_vec()
    }
}
impl From<Bar> for Bytes {
    fn from(packet: Bar) -> Self {
        packet.to_bytes()
    }
}
impl From<Bar> for Vec<u8> {
    fn from(packet: Bar) -> Self {
        packet.to_vec()
    }
}
impl Bar {
    pub fn parse(mut bytes: &[u8]) -> Result<Self> {
        Ok(Self::new(Arc::new(BarData::parse(bytes)?)).unwrap())
    }
    fn new(root: Arc<BarData>) -> std::result::Result<Self, &'static str> {
        let bar = root;
        Ok(Self { bar })
    }
    pub fn get_x(&self) -> &Vec<Foo> {
        &self.bar.as_ref().x
    }
}
impl BarBuilder {
    pub fn build(self) -> Bar {
        let bar = Arc::new(BarData { x: self.x });
        Bar::new(bar).unwrap()
    }
}
