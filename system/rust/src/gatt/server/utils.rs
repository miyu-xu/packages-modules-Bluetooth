use crate::packets::{AttAttributeDataChild, Builder};

pub fn truncate_att_data(data: AttAttributeDataChild, len: usize) -> AttAttributeDataChild {
    match data {
        AttAttributeDataChild::RawData(data) => {
            let mut data = Vec::from(data);
            data.truncate(len);
            AttAttributeDataChild::RawData(data.into_boxed_slice())
        }
        _ => data,
    }
}

pub struct PayloadAccumulator<T: Builder> {
    curr: usize,
    lim: usize,
    elems: Vec<T>,
}

impl<T: Builder> PayloadAccumulator<T> {
    pub fn new(size: usize) -> Self {
        Self { curr: 0, lim: size * 8, elems: vec![] }
    }

    pub fn push(&mut self, builder: T) -> bool {
        // if serialization fails we WANT to continue, to get a clean SerializeError at the end
        let elem_size = builder.size_in_bits().unwrap_or(0);
        if elem_size + self.curr > self.lim {
            return false;
        }
        self.elems.push(builder);
        self.curr += elem_size;
        true
    }

    pub fn into_boxed_slice(self) -> Box<[T]> {
        self.elems.into_boxed_slice()
    }

    pub fn is_empty(&self) -> bool {
        self.elems.is_empty()
    }
}
