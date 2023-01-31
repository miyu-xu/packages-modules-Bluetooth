use crate::packets::AttAttributeDataChild;

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
        // Verifies that non-Raw data is not truncated
        // Note: this behavior is non-ideal, but in practice it's OK
        // since anything except for RawData will NEVER exceed an MTU
        // Kept since it makes writing tests way easier

        let data =
            AttServiceDeclarationValueBuilder { uuid: UuidBuilder { data: vec![1, 2, 3].into() } }
                .into();
        let mtu = 2;

        let truncated = truncate_att_data(data, mtu);

        assert_eq!(truncated.to_vec().unwrap(), vec![1, 2, 3]);
    }
}
