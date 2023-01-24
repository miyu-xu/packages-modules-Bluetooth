use crate::packets::{AttAttributeDataChild, Builder, Serializable};

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

#[cfg(test)]
mod test {
    use super::*;

    use crate::packets::{AttServiceDeclarationValueBuilder, Serializable, UuidBuilder};

    #[test]
    fn test_unaffected() {
        let data = AttAttributeDataChild::RawData(vec![1, 2, 3].into());
        let mtu = 21;

        let truncated = truncate_att_data(data, mtu);

        assert_eq!(truncated.to_vec().unwrap(), vec![1, 2, 3]);
    }

    #[test]
    fn test_truncated() {
        let data = AttAttributeDataChild::RawData(vec![1, 2, 3].into());
        let mtu = 2;

        let truncated = truncate_att_data(data, mtu);

        assert_eq!(truncated.to_vec().unwrap(), vec![1, 2]);
    }

    #[test]
    fn test_truncated_non_raw() {
        // Note: this behavior is non-ideal, but in practice it's OK
        // since anything except for RawData will NEVER exceed an MTU
        // We keep it since it makes writing tests way easier

        let data =
            AttServiceDeclarationValueBuilder { uuid: UuidBuilder { data: vec![1, 2, 3].into() } }
                .into();
        let mtu = 2;

        let truncated = truncate_att_data(data, mtu);

        assert_eq!(truncated.to_vec().unwrap(), vec![1, 2, 3]);
    }
}
