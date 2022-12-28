use pdl_rust_no_allocation_tests::*;

fn hex_to_word(hex: u8) -> u8 {
    if b'0' <= hex && hex <= b'9' {
        hex - b'0'
    } else if b'A' <= hex && hex <= b'F' {
        hex - b'A' + 0xa
    } else {
        hex - b'a' + 0xa
    }
}

fn hex_to_byte_string(hex: &str) -> Vec<u8> {
    hex.as_bytes()
        .chunks_exact(2)
        .map(|chunk| hex_to_word(chunk[0]) + (hex_to_word(chunk[1]) << 4))
        .collect()
}
#[test]
fn test_Packet_Scalar_Field_0() {
    let base = hex_to_byte_string("0000000000000000");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 0u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 0u64);
    }
}

#[test]
fn test_Packet_Scalar_Field_1() {
    let base = hex_to_byte_string("80ffffffffffffff");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 144115188075855871u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Scalar_Field_2() {
    let base = hex_to_byte_string("8003830282018100");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 283686952306183u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Scalar_Field_3() {
    let base = hex_to_byte_string("7f00000000000000");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 127u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 0u64);
    }
}

#[test]
fn test_Packet_Scalar_Field_4() {
    let base = hex_to_byte_string("ffffffffffffffff");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 127u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 144115188075855871u64);
    }
}

#[test]
fn test_Packet_Scalar_Field_5() {
    let base = hex_to_byte_string("ff03830282018100");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 127u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 283686952306183u64);
    }
}

#[test]
fn test_Packet_Scalar_Field_6() {
    let base = hex_to_byte_string("0000000000000000");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 0u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Scalar_Field_7() {
    let base = hex_to_byte_string("80ffffffffffffff");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 144115188075855871u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Scalar_Field_8() {
    let base = hex_to_byte_string("8003830282018100");
    let Packet_Scalar_Field_instance =
        Packet_Scalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_c()), 283686952306183u64);
        assert_eq!(u64::from(Packet_Scalar_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Enum_Field_0() {
    let base = hex_to_byte_string("0100000000000000");
    let Packet_Enum_Field_instance =
        Packet_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_a()), 1u64);
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_c()), 0u64);
    }
}

#[test]
fn test_Packet_Enum_Field_1() {
    let base = hex_to_byte_string("81ffffffffffffff");
    let Packet_Enum_Field_instance =
        Packet_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_a()), 1u64);
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_c()), 144115188075855871u64);
    }
}

#[test]
fn test_Packet_Enum_Field_2() {
    let base = hex_to_byte_string("810e0d0c0b0a0908");
    let Packet_Enum_Field_instance =
        Packet_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_a()), 1u64);
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_c()), 4523477106694685u64);
    }
}

#[test]
fn test_Packet_Enum_Field_3() {
    let base = hex_to_byte_string("0200000000000000");
    let Packet_Enum_Field_instance =
        Packet_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_c()), 0u64);
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_a()), 2u64);
    }
}

#[test]
fn test_Packet_Enum_Field_4() {
    let base = hex_to_byte_string("82ffffffffffffff");
    let Packet_Enum_Field_instance =
        Packet_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_a()), 2u64);
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_c()), 144115188075855871u64);
    }
}

#[test]
fn test_Packet_Enum_Field_5() {
    let base = hex_to_byte_string("820e0d0c0b0a0908");
    let Packet_Enum_Field_instance =
        Packet_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_a()), 2u64);
        assert_eq!(u64::from(Packet_Enum_Field_instance.get_c()), 4523477106694685u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_0() {
    let base = hex_to_byte_string("0000000000000000");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 0u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_1() {
    let base = hex_to_byte_string("00feffffffffffff");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 0u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 36028797018963967u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_2() {
    let base = hex_to_byte_string("002c151413121110");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 2261184477268630u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_3() {
    let base = hex_to_byte_string("7f00000000000000");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 127u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 0u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_4() {
    let base = hex_to_byte_string("7ffeffffffffffff");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 127u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 36028797018963967u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_5() {
    let base = hex_to_byte_string("7f2c151413121110");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 127u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 2261184477268630u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_6() {
    let base = hex_to_byte_string("0700000000000000");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 7u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 0u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_7() {
    let base = hex_to_byte_string("07feffffffffffff");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 7u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 36028797018963967u64);
    }
}

#[test]
fn test_Packet_Reserved_Field_8() {
    let base = hex_to_byte_string("072c151413121110");
    let Packet_Reserved_Field_instance =
        Packet_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_a()), 7u64);
        assert_eq!(u64::from(Packet_Reserved_Field_instance.get_c()), 2261184477268630u64);
    }
}

#[test]
fn test_Packet_Size_Field_0() {
    let base = hex_to_byte_string("0000000000000000");
    let Packet_Size_Field_instance =
        Packet_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Size_Field_instance.get_a()), 0u64);
        let b_vec = Packet_Size_Field_instance.get_b_iter().collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Size_Field_1() {
    let base = hex_to_byte_string("07000000000000001f102122232425");
    let Packet_Size_Field_instance =
        Packet_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        let b_vec = Packet_Size_Field_instance.get_b_iter().collect::<Vec<_>>();
        assert_eq!(u64::from(b_vec[0usize]), 31u64);
        assert_eq!(u64::from(b_vec[1usize]), 16u64);
        assert_eq!(u64::from(b_vec[2usize]), 33u64);
        assert_eq!(u64::from(b_vec[3usize]), 34u64);
        assert_eq!(u64::from(b_vec[4usize]), 35u64);
        assert_eq!(u64::from(b_vec[5usize]), 36u64);
        assert_eq!(u64::from(b_vec[6usize]), 37u64);
        assert_eq!(u64::from(Packet_Size_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Size_Field_2() {
    let base = hex_to_byte_string("f8ffffffffffffff");
    let Packet_Size_Field_instance =
        Packet_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Size_Field_instance.get_a()), 2305843009213693951u64);
        let b_vec = Packet_Size_Field_instance.get_b_iter().collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Size_Field_3() {
    let base = hex_to_byte_string("ffffffffffffffff1f102122232425");
    let Packet_Size_Field_instance =
        Packet_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Size_Field_instance.get_a()), 2305843009213693951u64);
        let b_vec = Packet_Size_Field_instance.get_b_iter().collect::<Vec<_>>();
        assert_eq!(u64::from(b_vec[0usize]), 31u64);
        assert_eq!(u64::from(b_vec[1usize]), 16u64);
        assert_eq!(u64::from(b_vec[2usize]), 33u64);
        assert_eq!(u64::from(b_vec[3usize]), 34u64);
        assert_eq!(u64::from(b_vec[4usize]), 35u64);
        assert_eq!(u64::from(b_vec[5usize]), 36u64);
        assert_eq!(u64::from(b_vec[6usize]), 37u64);
    }
}

#[test]
fn test_Packet_Size_Field_4() {
    let base = hex_to_byte_string("f00e8e0d8d0c8c0b");
    let Packet_Size_Field_instance =
        Packet_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        let b_vec = Packet_Size_Field_instance.get_b_iter().collect::<Vec<_>>();
        assert_eq!(u64::from(Packet_Size_Field_instance.get_a()), 104006728889254366u64);
    }
}

#[test]
fn test_Packet_Size_Field_5() {
    let base = hex_to_byte_string("f70e8e0d8d0c8c0b1f102122232425");
    let Packet_Size_Field_instance =
        Packet_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Size_Field_instance.get_a()), 104006728889254366u64);
        let b_vec = Packet_Size_Field_instance.get_b_iter().collect::<Vec<_>>();
        assert_eq!(u64::from(b_vec[0usize]), 31u64);
        assert_eq!(u64::from(b_vec[1usize]), 16u64);
        assert_eq!(u64::from(b_vec[2usize]), 33u64);
        assert_eq!(u64::from(b_vec[3usize]), 34u64);
        assert_eq!(u64::from(b_vec[4usize]), 35u64);
        assert_eq!(u64::from(b_vec[5usize]), 36u64);
        assert_eq!(u64::from(b_vec[6usize]), 37u64);
    }
}

#[test]
fn test_Packet_Count_Field_0() {
    let base = hex_to_byte_string("0000000000000000");
    let Packet_Count_Field_instance =
        Packet_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Count_Field_instance.get_a()), 0u64);
        let b_vec = Packet_Count_Field_instance.get_b_iter().collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Count_Field_1() {
    let base = hex_to_byte_string("07000000000000002c2f2e31303332");
    let Packet_Count_Field_instance =
        Packet_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        let b_vec = Packet_Count_Field_instance.get_b_iter().collect::<Vec<_>>();
        assert_eq!(u64::from(b_vec[0usize]), 44u64);
        assert_eq!(u64::from(b_vec[1usize]), 47u64);
        assert_eq!(u64::from(b_vec[2usize]), 46u64);
        assert_eq!(u64::from(b_vec[3usize]), 49u64);
        assert_eq!(u64::from(b_vec[4usize]), 48u64);
        assert_eq!(u64::from(b_vec[5usize]), 51u64);
        assert_eq!(u64::from(b_vec[6usize]), 50u64);
        assert_eq!(u64::from(Packet_Count_Field_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Count_Field_2() {
    let base = hex_to_byte_string("f8ffffffffffffff");
    let Packet_Count_Field_instance =
        Packet_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Count_Field_instance.get_a()), 2305843009213693951u64);
        let b_vec = Packet_Count_Field_instance.get_b_iter().collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Count_Field_3() {
    let base = hex_to_byte_string("ffffffffffffffff2c2f2e31303332");
    let Packet_Count_Field_instance =
        Packet_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Count_Field_instance.get_a()), 2305843009213693951u64);
        let b_vec = Packet_Count_Field_instance.get_b_iter().collect::<Vec<_>>();
        assert_eq!(u64::from(b_vec[0usize]), 44u64);
        assert_eq!(u64::from(b_vec[1usize]), 47u64);
        assert_eq!(u64::from(b_vec[2usize]), 46u64);
        assert_eq!(u64::from(b_vec[3usize]), 49u64);
        assert_eq!(u64::from(b_vec[4usize]), 48u64);
        assert_eq!(u64::from(b_vec[5usize]), 51u64);
        assert_eq!(u64::from(b_vec[6usize]), 50u64);
    }
}

#[test]
fn test_Packet_Count_Field_4() {
    let base = hex_to_byte_string("c8b2a29282726222");
    let Packet_Count_Field_instance =
        Packet_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_Count_Field_instance.get_a()), 309708581267330649u64);
        let b_vec = Packet_Count_Field_instance.get_b_iter().collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Count_Field_5() {
    let base = hex_to_byte_string("cfb2a292827262222c2f2e31303332");
    let Packet_Count_Field_instance =
        Packet_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        let b_vec = Packet_Count_Field_instance.get_b_iter().collect::<Vec<_>>();
        assert_eq!(u64::from(b_vec[0usize]), 44u64);
        assert_eq!(u64::from(b_vec[1usize]), 47u64);
        assert_eq!(u64::from(b_vec[2usize]), 46u64);
        assert_eq!(u64::from(b_vec[3usize]), 49u64);
        assert_eq!(u64::from(b_vec[4usize]), 48u64);
        assert_eq!(u64::from(b_vec[5usize]), 51u64);
        assert_eq!(u64::from(b_vec[6usize]), 50u64);
        assert_eq!(u64::from(Packet_Count_Field_instance.get_a()), 309708581267330649u64);
    }
}

#[test]
fn test_Packet_FixedScalar_Field_0() {
    let base = hex_to_byte_string("0700000000000000");
    let Packet_FixedScalar_Field_instance =
        Packet_FixedScalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_FixedScalar_Field_instance.get_b()), 0u64);
    }
}

#[test]
fn test_Packet_FixedScalar_Field_1() {
    let base = hex_to_byte_string("87ffffffffffffff");
    let Packet_FixedScalar_Field_instance =
        Packet_FixedScalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_FixedScalar_Field_instance.get_b()), 144115188075855871u64);
    }
}

#[test]
fn test_Packet_FixedScalar_Field_2() {
    let base = hex_to_byte_string("877572706e6c6a34");
    let Packet_FixedScalar_Field_instance =
        Packet_FixedScalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_FixedScalar_Field_instance.get_b()), 29507425461658859u64);
    }
}

#[test]
fn test_Packet_FixedEnum_Field_0() {
    let base = hex_to_byte_string("0100000000000000");
    let Packet_FixedEnum_Field_instance =
        Packet_FixedEnum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_FixedEnum_Field_instance.get_b()), 0u64);
    }
}

#[test]
fn test_Packet_FixedEnum_Field_1() {
    let base = hex_to_byte_string("81ffffffffffffff");
    let Packet_FixedEnum_Field_instance =
        Packet_FixedEnum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_FixedEnum_Field_instance.get_b()), 144115188075855871u64);
    }
}

#[test]
fn test_Packet_FixedEnum_Field_2() {
    let base = hex_to_byte_string("010501fdf8f4f038");
    let Packet_FixedEnum_Field_instance =
        Packet_FixedEnum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        assert_eq!(u64::from(Packet_FixedEnum_Field_instance.get_b()), 32055067271627274u64);
    }
}

#[test]
fn test_Packet_Payload_Field_VariableSize_0() {
    let base = hex_to_byte_string("00");
    let Packet_Payload_Field_VariableSize_instance =
        Packet_Payload_Field_VariableSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {}
}

#[test]
fn test_Packet_Payload_Field_VariableSize_1() {
    let base = hex_to_byte_string("0743444546474049");
    let Packet_Payload_Field_VariableSize_instance =
        Packet_Payload_Field_VariableSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {}
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_0() {
    let base = hex_to_byte_string("0000");
    let Packet_Payload_Field_UnknownSize_instance =
        Packet_Payload_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_1() {
    let base = hex_to_byte_string("ffff");
    let Packet_Payload_Field_UnknownSize_instance =
        Packet_Payload_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_instance.get_a()), 65535u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_2() {
    let base = hex_to_byte_string("a552");
    let Packet_Payload_Field_UnknownSize_instance =
        Packet_Payload_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_instance.get_a()), 21157u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_3() {
    let base = hex_to_byte_string("4f485152530000");
    let Packet_Payload_Field_UnknownSize_instance =
        Packet_Payload_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_4() {
    let base = hex_to_byte_string("4f48515253ffff");
    let Packet_Payload_Field_UnknownSize_instance =
        Packet_Payload_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_instance.get_a()), 65535u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_5() {
    let base = hex_to_byte_string("4f48515253a552");
    let Packet_Payload_Field_UnknownSize_instance =
        Packet_Payload_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_instance.get_a()), 21157u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_Terminal_0() {
    let base = hex_to_byte_string("0000");
    let Packet_Payload_Field_UnknownSize_Terminal_instance =
        Packet_Payload_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_Terminal_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_Terminal_1() {
    let base = hex_to_byte_string("000050595a5b5c");
    let Packet_Payload_Field_UnknownSize_Terminal_instance =
        Packet_Payload_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_Terminal_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_Terminal_2() {
    let base = hex_to_byte_string("ffff");
    let Packet_Payload_Field_UnknownSize_Terminal_instance =
        Packet_Payload_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_Terminal_instance.get_a()), 65535u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_Terminal_3() {
    let base = hex_to_byte_string("ffff50595a5b5c");
    let Packet_Payload_Field_UnknownSize_Terminal_instance =
        Packet_Payload_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_Terminal_instance.get_a()), 65535u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_Terminal_4() {
    let base = hex_to_byte_string("b752");
    let Packet_Payload_Field_UnknownSize_Terminal_instance =
        Packet_Payload_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_Terminal_instance.get_a()), 21175u64);
    }
}

#[test]
fn test_Packet_Payload_Field_UnknownSize_Terminal_5() {
    let base = hex_to_byte_string("b75250595a5b5c");
    let Packet_Payload_Field_UnknownSize_Terminal_instance =
        Packet_Payload_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Payload_Field_UnknownSize_Terminal_instance.get_a()), 21175u64);
    }
}

#[test]
fn test_Packet_Body_Field_VariableSize_0() {
    let base = hex_to_byte_string("00");
    let Packet_Body_Field_VariableSize_instance =
        Packet_Body_Field_VariableSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {}
}

#[test]
fn test_Packet_Body_Field_VariableSize_1() {
    let base = hex_to_byte_string("075d5e5f58616263");
    let Packet_Body_Field_VariableSize_instance =
        Packet_Body_Field_VariableSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {}
}

#[test]
fn test_Packet_Body_Field_UnknownSize_0() {
    let base = hex_to_byte_string("0000");
    let Packet_Body_Field_UnknownSize_instance =
        Packet_Body_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_1() {
    let base = hex_to_byte_string("ffff");
    let Packet_Body_Field_UnknownSize_instance =
        Packet_Body_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_instance.get_a()), 65535u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_2() {
    let base = hex_to_byte_string("4a6b");
    let Packet_Body_Field_UnknownSize_instance =
        Packet_Body_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_instance.get_a()), 27466u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_3() {
    let base = hex_to_byte_string("64656667600000");
    let Packet_Body_Field_UnknownSize_instance =
        Packet_Body_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_4() {
    let base = hex_to_byte_string("6465666760ffff");
    let Packet_Body_Field_UnknownSize_instance =
        Packet_Body_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_instance.get_a()), 65535u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_5() {
    let base = hex_to_byte_string("64656667604a6b");
    let Packet_Body_Field_UnknownSize_instance =
        Packet_Body_Field_UnknownSizeView::try_parse(SizedBitSlice::from(&base[..]).into())
            .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_instance.get_a()), 27466u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_Terminal_0() {
    let base = hex_to_byte_string("0000");
    let Packet_Body_Field_UnknownSize_Terminal_instance =
        Packet_Body_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_Terminal_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_Terminal_1() {
    let base = hex_to_byte_string("00006d6e6f6871");
    let Packet_Body_Field_UnknownSize_Terminal_instance =
        Packet_Body_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_Terminal_instance.get_a()), 0u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_Terminal_2() {
    let base = hex_to_byte_string("ffff");
    let Packet_Body_Field_UnknownSize_Terminal_instance =
        Packet_Body_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_Terminal_instance.get_a()), 65535u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_Terminal_3() {
    let base = hex_to_byte_string("ffff6d6e6f6871");
    let Packet_Body_Field_UnknownSize_Terminal_instance =
        Packet_Body_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_Terminal_instance.get_a()), 65535u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_Terminal_4() {
    let base = hex_to_byte_string("5c6b");
    let Packet_Body_Field_UnknownSize_Terminal_instance =
        Packet_Body_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_Terminal_instance.get_a()), 27484u64);
    }
}

#[test]
fn test_Packet_Body_Field_UnknownSize_Terminal_5() {
    let base = hex_to_byte_string("5c6b6d6e6f6871");
    let Packet_Body_Field_UnknownSize_Terminal_instance =
        Packet_Body_Field_UnknownSize_TerminalView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        assert_eq!(u64::from(Packet_Body_Field_UnknownSize_Terminal_instance.get_a()), 27484u64);
    }
}

#[test]
fn test_Packet_Struct_Field_0() {
    let base = hex_to_byte_string("0000");
    let Packet_Struct_Field_instance =
        Packet_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Packet_Struct_Field_instance.get_a().get_a()), 0u64);
        }
        {
            let array_vec =
                Packet_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Packet_Struct_Field_1() {
    let base = hex_to_byte_string("0003788182");
    let Packet_Struct_Field_instance =
        Packet_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Packet_Struct_Field_instance.get_a().get_a()), 0u64);
        }
        {
            let array_vec =
                Packet_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 120u64);
            assert_eq!(u64::from(array_vec[1usize]), 129u64);
            assert_eq!(u64::from(array_vec[2usize]), 130u64);
        }
    }
}

#[test]
fn test_Packet_Struct_Field_2() {
    let base = hex_to_byte_string("ff00");
    let Packet_Struct_Field_instance =
        Packet_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Packet_Struct_Field_instance.get_a().get_a()), 255u64);
        }
        {
            let array_vec =
                Packet_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Packet_Struct_Field_3() {
    let base = hex_to_byte_string("ff03788182");
    let Packet_Struct_Field_instance =
        Packet_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Packet_Struct_Field_instance.get_a().get_a()), 255u64);
        }
        {
            let array_vec =
                Packet_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 120u64);
            assert_eq!(u64::from(array_vec[1usize]), 129u64);
            assert_eq!(u64::from(array_vec[2usize]), 130u64);
        }
    }
}

#[test]
fn test_Packet_Struct_Field_4() {
    let base = hex_to_byte_string("7f00");
    let Packet_Struct_Field_instance =
        Packet_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Packet_Struct_Field_instance.get_a().get_a()), 127u64);
        }
        {
            let array_vec =
                Packet_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Packet_Struct_Field_5() {
    let base = hex_to_byte_string("7f03788182");
    let Packet_Struct_Field_instance =
        Packet_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Packet_Struct_Field_instance.get_a().get_a()), 127u64);
        }
        {
            let array_vec =
                Packet_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 120u64);
            assert_eq!(u64::from(array_vec[1usize]), 129u64);
            assert_eq!(u64::from(array_vec[2usize]), 130u64);
        }
    }
}

#[test]
fn test_Packet_Array_Field_ByteElement_ConstantSize_0() {
    let base = hex_to_byte_string("83848586");
    let Packet_Array_Field_ByteElement_ConstantSize_instance =
        Packet_Array_Field_ByteElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ByteElement_ConstantSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 131u64);
        assert_eq!(u64::from(array_vec[1usize]), 132u64);
        assert_eq!(u64::from(array_vec[2usize]), 133u64);
        assert_eq!(u64::from(array_vec[3usize]), 134u64);
    }
}

#[test]
fn test_Packet_Array_Field_ByteElement_VariableSize_0() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_ByteElement_VariableSize_instance =
        Packet_Array_Field_ByteElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ByteElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_ByteElement_VariableSize_1() {
    let base = hex_to_byte_string("0f8780898a8b8c8d8e8f889192939495");
    let Packet_Array_Field_ByteElement_VariableSize_instance =
        Packet_Array_Field_ByteElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ByteElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 135u64);
        assert_eq!(u64::from(array_vec[1usize]), 128u64);
        assert_eq!(u64::from(array_vec[2usize]), 137u64);
        assert_eq!(u64::from(array_vec[3usize]), 138u64);
        assert_eq!(u64::from(array_vec[4usize]), 139u64);
        assert_eq!(u64::from(array_vec[5usize]), 140u64);
        assert_eq!(u64::from(array_vec[6usize]), 141u64);
        assert_eq!(u64::from(array_vec[7usize]), 142u64);
        assert_eq!(u64::from(array_vec[8usize]), 143u64);
        assert_eq!(u64::from(array_vec[9usize]), 136u64);
        assert_eq!(u64::from(array_vec[10usize]), 145u64);
        assert_eq!(u64::from(array_vec[11usize]), 146u64);
        assert_eq!(u64::from(array_vec[12usize]), 147u64);
        assert_eq!(u64::from(array_vec[13usize]), 148u64);
        assert_eq!(u64::from(array_vec[14usize]), 149u64);
    }
}

#[test]
fn test_Packet_Array_Field_ByteElement_VariableCount_0() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_ByteElement_VariableCount_instance =
        Packet_Array_Field_ByteElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ByteElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_ByteElement_VariableCount_1() {
    let base = hex_to_byte_string("0f969790999a9b9c9d9e9f98a1a2a3a4");
    let Packet_Array_Field_ByteElement_VariableCount_instance =
        Packet_Array_Field_ByteElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ByteElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 150u64);
        assert_eq!(u64::from(array_vec[1usize]), 151u64);
        assert_eq!(u64::from(array_vec[2usize]), 144u64);
        assert_eq!(u64::from(array_vec[3usize]), 153u64);
        assert_eq!(u64::from(array_vec[4usize]), 154u64);
        assert_eq!(u64::from(array_vec[5usize]), 155u64);
        assert_eq!(u64::from(array_vec[6usize]), 156u64);
        assert_eq!(u64::from(array_vec[7usize]), 157u64);
        assert_eq!(u64::from(array_vec[8usize]), 158u64);
        assert_eq!(u64::from(array_vec[9usize]), 159u64);
        assert_eq!(u64::from(array_vec[10usize]), 152u64);
        assert_eq!(u64::from(array_vec[11usize]), 161u64);
        assert_eq!(u64::from(array_vec[12usize]), 162u64);
        assert_eq!(u64::from(array_vec[13usize]), 163u64);
        assert_eq!(u64::from(array_vec[14usize]), 164u64);
    }
}

#[test]
fn test_Packet_Array_Field_ByteElement_UnknownSize_0() {
    let base = hex_to_byte_string("");
    let Packet_Array_Field_ByteElement_UnknownSize_instance =
        Packet_Array_Field_ByteElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ByteElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_ByteElement_UnknownSize_1() {
    let base = hex_to_byte_string("a5a6a7");
    let Packet_Array_Field_ByteElement_UnknownSize_instance =
        Packet_Array_Field_ByteElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ByteElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 165u64);
        assert_eq!(u64::from(array_vec[1usize]), 166u64);
        assert_eq!(u64::from(array_vec[2usize]), 167u64);
    }
}

#[test]
fn test_Packet_Array_Field_ScalarElement_ConstantSize_0() {
    let base = hex_to_byte_string("41a553ad65ad77ad");
    let Packet_Array_Field_ScalarElement_ConstantSize_instance =
        Packet_Array_Field_ScalarElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ScalarElement_ConstantSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 42305u64);
        assert_eq!(u64::from(array_vec[1usize]), 44371u64);
        assert_eq!(u64::from(array_vec[2usize]), 44389u64);
        assert_eq!(u64::from(array_vec[3usize]), 44407u64);
    }
}

#[test]
fn test_Packet_Array_Field_ScalarElement_VariableSize_0() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_ScalarElement_VariableSize_instance =
        Packet_Array_Field_ScalarElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ScalarElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_ScalarElement_VariableSize_1() {
    let base = hex_to_byte_string("0e81ad93b5a5b5b7b5c1b5d3bde5bd");
    let Packet_Array_Field_ScalarElement_VariableSize_instance =
        Packet_Array_Field_ScalarElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ScalarElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 44417u64);
        assert_eq!(u64::from(array_vec[1usize]), 46483u64);
        assert_eq!(u64::from(array_vec[2usize]), 46501u64);
        assert_eq!(u64::from(array_vec[3usize]), 46519u64);
        assert_eq!(u64::from(array_vec[4usize]), 46529u64);
        assert_eq!(u64::from(array_vec[5usize]), 48595u64);
        assert_eq!(u64::from(array_vec[6usize]), 48613u64);
    }
}

#[test]
fn test_Packet_Array_Field_ScalarElement_VariableCount_0() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_ScalarElement_VariableCount_instance =
        Packet_Array_Field_ScalarElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ScalarElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_ScalarElement_VariableCount_1() {
    let base = hex_to_byte_string("0ff7bd01be13c625c637c641c653ce65ce77ce81ce93d6a5d6b7d6c1d6d3de");
    let Packet_Array_Field_ScalarElement_VariableCount_instance =
        Packet_Array_Field_ScalarElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ScalarElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 48631u64);
        assert_eq!(u64::from(array_vec[1usize]), 48641u64);
        assert_eq!(u64::from(array_vec[2usize]), 50707u64);
        assert_eq!(u64::from(array_vec[3usize]), 50725u64);
        assert_eq!(u64::from(array_vec[4usize]), 50743u64);
        assert_eq!(u64::from(array_vec[5usize]), 50753u64);
        assert_eq!(u64::from(array_vec[6usize]), 52819u64);
        assert_eq!(u64::from(array_vec[7usize]), 52837u64);
        assert_eq!(u64::from(array_vec[8usize]), 52855u64);
        assert_eq!(u64::from(array_vec[9usize]), 52865u64);
        assert_eq!(u64::from(array_vec[10usize]), 54931u64);
        assert_eq!(u64::from(array_vec[11usize]), 54949u64);
        assert_eq!(u64::from(array_vec[12usize]), 54967u64);
        assert_eq!(u64::from(array_vec[13usize]), 54977u64);
        assert_eq!(u64::from(array_vec[14usize]), 57043u64);
    }
}

#[test]
fn test_Packet_Array_Field_ScalarElement_UnknownSize_0() {
    let base = hex_to_byte_string("");
    let Packet_Array_Field_ScalarElement_UnknownSize_instance =
        Packet_Array_Field_ScalarElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ScalarElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_ScalarElement_UnknownSize_1() {
    let base = hex_to_byte_string("e5def7de01df");
    let Packet_Array_Field_ScalarElement_UnknownSize_instance =
        Packet_Array_Field_ScalarElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_ScalarElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 57061u64);
        assert_eq!(u64::from(array_vec[1usize]), 57079u64);
        assert_eq!(u64::from(array_vec[2usize]), 57089u64);
    }
}

#[test]
fn test_Packet_Array_Field_EnumElement_ConstantSize_0() {
    let base = hex_to_byte_string("bbaaddccbbaaddcc");
    let Packet_Array_Field_EnumElement_ConstantSize_instance =
        Packet_Array_Field_EnumElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_EnumElement_ConstantSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 43707u64);
        assert_eq!(u64::from(array_vec[1usize]), 52445u64);
        assert_eq!(u64::from(array_vec[2usize]), 43707u64);
        assert_eq!(u64::from(array_vec[3usize]), 52445u64);
    }
}

#[test]
fn test_Packet_Array_Field_EnumElement_VariableSize_0() {
    let base = hex_to_byte_string("0ebbaaddccbbaaddccbbaaddccbbaa");
    let Packet_Array_Field_EnumElement_VariableSize_instance =
        Packet_Array_Field_EnumElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_EnumElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 43707u64);
        assert_eq!(u64::from(array_vec[1usize]), 52445u64);
        assert_eq!(u64::from(array_vec[2usize]), 43707u64);
        assert_eq!(u64::from(array_vec[3usize]), 52445u64);
        assert_eq!(u64::from(array_vec[4usize]), 43707u64);
        assert_eq!(u64::from(array_vec[5usize]), 52445u64);
        assert_eq!(u64::from(array_vec[6usize]), 43707u64);
    }
}

#[test]
fn test_Packet_Array_Field_EnumElement_VariableSize_1() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_EnumElement_VariableSize_instance =
        Packet_Array_Field_EnumElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_EnumElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_EnumElement_VariableCount_0() {
    let base = hex_to_byte_string("0fbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaa");
    let Packet_Array_Field_EnumElement_VariableCount_instance =
        Packet_Array_Field_EnumElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_EnumElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 43707u64);
        assert_eq!(u64::from(array_vec[1usize]), 52445u64);
        assert_eq!(u64::from(array_vec[2usize]), 43707u64);
        assert_eq!(u64::from(array_vec[3usize]), 52445u64);
        assert_eq!(u64::from(array_vec[4usize]), 43707u64);
        assert_eq!(u64::from(array_vec[5usize]), 52445u64);
        assert_eq!(u64::from(array_vec[6usize]), 43707u64);
        assert_eq!(u64::from(array_vec[7usize]), 52445u64);
        assert_eq!(u64::from(array_vec[8usize]), 43707u64);
        assert_eq!(u64::from(array_vec[9usize]), 52445u64);
        assert_eq!(u64::from(array_vec[10usize]), 43707u64);
        assert_eq!(u64::from(array_vec[11usize]), 52445u64);
        assert_eq!(u64::from(array_vec[12usize]), 43707u64);
        assert_eq!(u64::from(array_vec[13usize]), 52445u64);
        assert_eq!(u64::from(array_vec[14usize]), 43707u64);
    }
}

#[test]
fn test_Packet_Array_Field_EnumElement_VariableCount_1() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_EnumElement_VariableCount_instance =
        Packet_Array_Field_EnumElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_EnumElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_EnumElement_UnknownSize_0() {
    let base = hex_to_byte_string ("bbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddcc") ;
    let Packet_Array_Field_EnumElement_UnknownSize_instance =
        Packet_Array_Field_EnumElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_EnumElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 43707u64);
        assert_eq!(u64::from(array_vec[1usize]), 52445u64);
        assert_eq!(u64::from(array_vec[2usize]), 43707u64);
        assert_eq!(u64::from(array_vec[3usize]), 52445u64);
        assert_eq!(u64::from(array_vec[4usize]), 43707u64);
        assert_eq!(u64::from(array_vec[5usize]), 52445u64);
        assert_eq!(u64::from(array_vec[6usize]), 43707u64);
        assert_eq!(u64::from(array_vec[7usize]), 52445u64);
        assert_eq!(u64::from(array_vec[8usize]), 43707u64);
        assert_eq!(u64::from(array_vec[9usize]), 52445u64);
        assert_eq!(u64::from(array_vec[10usize]), 43707u64);
        assert_eq!(u64::from(array_vec[11usize]), 52445u64);
        assert_eq!(u64::from(array_vec[12usize]), 43707u64);
        assert_eq!(u64::from(array_vec[13usize]), 52445u64);
        assert_eq!(u64::from(array_vec[14usize]), 43707u64);
        assert_eq!(u64::from(array_vec[15usize]), 52445u64);
        assert_eq!(u64::from(array_vec[16usize]), 43707u64);
        assert_eq!(u64::from(array_vec[17usize]), 52445u64);
        assert_eq!(u64::from(array_vec[18usize]), 43707u64);
        assert_eq!(u64::from(array_vec[19usize]), 52445u64);
        assert_eq!(u64::from(array_vec[20usize]), 43707u64);
        assert_eq!(u64::from(array_vec[21usize]), 52445u64);
        assert_eq!(u64::from(array_vec[22usize]), 43707u64);
        assert_eq!(u64::from(array_vec[23usize]), 52445u64);
        assert_eq!(u64::from(array_vec[24usize]), 43707u64);
        assert_eq!(u64::from(array_vec[25usize]), 52445u64);
        assert_eq!(u64::from(array_vec[26usize]), 43707u64);
        assert_eq!(u64::from(array_vec[27usize]), 52445u64);
        assert_eq!(u64::from(array_vec[28usize]), 43707u64);
        assert_eq!(u64::from(array_vec[29usize]), 52445u64);
        assert_eq!(u64::from(array_vec[30usize]), 43707u64);
        assert_eq!(u64::from(array_vec[31usize]), 52445u64);
    }
}

#[test]
fn test_Packet_Array_Field_EnumElement_UnknownSize_1() {
    let base = hex_to_byte_string("");
    let Packet_Array_Field_EnumElement_UnknownSize_instance =
        Packet_Array_Field_EnumElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_EnumElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_ConstantSize_0() {
    let base = hex_to_byte_string("00ffe200");
    let Packet_Array_Field_SizedElement_ConstantSize_instance =
        Packet_Array_Field_SizedElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_ConstantSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            assert_eq!(u64::from(array_vec[0usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[1usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[2usize].get_a()), 226u64);
        }
        {
            assert_eq!(u64::from(array_vec[3usize].get_a()), 0u64);
        }
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_VariableSize_0() {
    let base = hex_to_byte_string("0f00ffe400ffe500ffe600ffe700ffe0");
    let Packet_Array_Field_SizedElement_VariableSize_instance =
        Packet_Array_Field_SizedElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            assert_eq!(u64::from(array_vec[0usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[1usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[2usize].get_a()), 228u64);
        }
        {
            assert_eq!(u64::from(array_vec[3usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[4usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[5usize].get_a()), 229u64);
        }
        {
            assert_eq!(u64::from(array_vec[6usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[7usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[8usize].get_a()), 230u64);
        }
        {
            assert_eq!(u64::from(array_vec[9usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[10usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[11usize].get_a()), 231u64);
        }
        {
            assert_eq!(u64::from(array_vec[12usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[13usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[14usize].get_a()), 224u64);
        }
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_VariableSize_1() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_SizedElement_VariableSize_instance =
        Packet_Array_Field_SizedElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_VariableCount_0() {
    let base = hex_to_byte_string("0f00ffea00ffeb00ffec00ffed00ffee");
    let Packet_Array_Field_SizedElement_VariableCount_instance =
        Packet_Array_Field_SizedElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            assert_eq!(u64::from(array_vec[0usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[1usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[2usize].get_a()), 234u64);
        }
        {
            assert_eq!(u64::from(array_vec[3usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[4usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[5usize].get_a()), 235u64);
        }
        {
            assert_eq!(u64::from(array_vec[6usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[7usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[8usize].get_a()), 236u64);
        }
        {
            assert_eq!(u64::from(array_vec[9usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[10usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[11usize].get_a()), 237u64);
        }
        {
            assert_eq!(u64::from(array_vec[12usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[13usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[14usize].get_a()), 238u64);
        }
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_VariableCount_1() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_SizedElement_VariableCount_instance =
        Packet_Array_Field_SizedElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_UnknownSize_0() {
    let base =
        hex_to_byte_string("00ffe800fff100fff200fff300fff400fff500fff600fff700fff000fff900ff");
    let Packet_Array_Field_SizedElement_UnknownSize_instance =
        Packet_Array_Field_SizedElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            assert_eq!(u64::from(array_vec[0usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[1usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[2usize].get_a()), 232u64);
        }
        {
            assert_eq!(u64::from(array_vec[3usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[4usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[5usize].get_a()), 241u64);
        }
        {
            assert_eq!(u64::from(array_vec[6usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[7usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[8usize].get_a()), 242u64);
        }
        {
            assert_eq!(u64::from(array_vec[9usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[10usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[11usize].get_a()), 243u64);
        }
        {
            assert_eq!(u64::from(array_vec[12usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[13usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[14usize].get_a()), 244u64);
        }
        {
            assert_eq!(u64::from(array_vec[15usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[16usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[17usize].get_a()), 245u64);
        }
        {
            assert_eq!(u64::from(array_vec[18usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[19usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[20usize].get_a()), 246u64);
        }
        {
            assert_eq!(u64::from(array_vec[21usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[22usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[23usize].get_a()), 247u64);
        }
        {
            assert_eq!(u64::from(array_vec[24usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[25usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[26usize].get_a()), 240u64);
        }
        {
            assert_eq!(u64::from(array_vec[27usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[28usize].get_a()), 255u64);
        }
        {
            assert_eq!(u64::from(array_vec[29usize].get_a()), 249u64);
        }
        {
            assert_eq!(u64::from(array_vec[30usize].get_a()), 0u64);
        }
        {
            assert_eq!(u64::from(array_vec[31usize].get_a()), 255u64);
        }
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_UnknownSize_1() {
    let base = hex_to_byte_string("");
    let Packet_Array_Field_SizedElement_UnknownSize_instance =
        Packet_Array_Field_SizedElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_ConstantSize_0() {
    let base = hex_to_byte_string("0003fbfcfd0003fef801");
    let Packet_Array_Field_UnsizedElement_ConstantSize_instance =
        Packet_Array_Field_UnsizedElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_ConstantSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 251u64);
            assert_eq!(u64::from(array_vec[1usize]), 252u64);
            assert_eq!(u64::from(array_vec[2usize]), 253u64);
        }
        {
            let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 254u64);
            assert_eq!(u64::from(array_vec[1usize]), 248u64);
            assert_eq!(u64::from(array_vec[2usize]), 1u64);
        }
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_VariableSize_0() {
    let base = hex_to_byte_string("0f0003050607000300090a00030b0c0d");
    let Packet_Array_Field_UnsizedElement_VariableSize_instance =
        Packet_Array_Field_UnsizedElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 5u64);
            assert_eq!(u64::from(array_vec[1usize]), 6u64);
            assert_eq!(u64::from(array_vec[2usize]), 7u64);
        }
        {
            let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 0u64);
            assert_eq!(u64::from(array_vec[1usize]), 9u64);
            assert_eq!(u64::from(array_vec[2usize]), 10u64);
        }
        {
            let array_vec = array_vec[4usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[5usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 11u64);
            assert_eq!(u64::from(array_vec[1usize]), 12u64);
            assert_eq!(u64::from(array_vec[2usize]), 13u64);
        }
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_VariableSize_1() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_UnsizedElement_VariableSize_instance =
        Packet_Array_Field_UnsizedElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_VariableSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_VariableCount_0() {
    let base = hex_to_byte_string(
        "0f00031112130003141516000317101900031a1b1c00031d1e1f0003182122000323242500",
    );
    let Packet_Array_Field_UnsizedElement_VariableCount_instance =
        Packet_Array_Field_UnsizedElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 17u64);
            assert_eq!(u64::from(array_vec[1usize]), 18u64);
            assert_eq!(u64::from(array_vec[2usize]), 19u64);
        }
        {
            let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 20u64);
            assert_eq!(u64::from(array_vec[1usize]), 21u64);
            assert_eq!(u64::from(array_vec[2usize]), 22u64);
        }
        {
            let array_vec = array_vec[4usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[5usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 23u64);
            assert_eq!(u64::from(array_vec[1usize]), 16u64);
            assert_eq!(u64::from(array_vec[2usize]), 25u64);
        }
        {
            let array_vec = array_vec[6usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[7usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 26u64);
            assert_eq!(u64::from(array_vec[1usize]), 27u64);
            assert_eq!(u64::from(array_vec[2usize]), 28u64);
        }
        {
            let array_vec = array_vec[8usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[9usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 29u64);
            assert_eq!(u64::from(array_vec[1usize]), 30u64);
            assert_eq!(u64::from(array_vec[2usize]), 31u64);
        }
        {
            let array_vec = array_vec[10usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[11usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 24u64);
            assert_eq!(u64::from(array_vec[1usize]), 33u64);
            assert_eq!(u64::from(array_vec[2usize]), 34u64);
        }
        {
            let array_vec = array_vec[12usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[13usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 35u64);
            assert_eq!(u64::from(array_vec[1usize]), 36u64);
            assert_eq!(u64::from(array_vec[2usize]), 37u64);
        }
        {
            let array_vec = array_vec[14usize].get_array_iter().collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_VariableCount_1() {
    let base = hex_to_byte_string("00");
    let Packet_Array_Field_UnsizedElement_VariableCount_instance =
        Packet_Array_Field_UnsizedElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_VariableCount_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_UnknownSize_0() {
    let base = hex_to_byte_string ("0003292a2b00032c2d2e00032f283100033233340003353637000330393a00033b3c3d00033e3f3800034142430003444546000347404900034a4b4c00034d4e4f000348515200035354550003565750") ;
    let Packet_Array_Field_UnsizedElement_UnknownSize_instance =
        Packet_Array_Field_UnsizedElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 41u64);
            assert_eq!(u64::from(array_vec[1usize]), 42u64);
            assert_eq!(u64::from(array_vec[2usize]), 43u64);
        }
        {
            let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 44u64);
            assert_eq!(u64::from(array_vec[1usize]), 45u64);
            assert_eq!(u64::from(array_vec[2usize]), 46u64);
        }
        {
            let array_vec = array_vec[4usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[5usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 47u64);
            assert_eq!(u64::from(array_vec[1usize]), 40u64);
            assert_eq!(u64::from(array_vec[2usize]), 49u64);
        }
        {
            let array_vec = array_vec[6usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[7usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 50u64);
            assert_eq!(u64::from(array_vec[1usize]), 51u64);
            assert_eq!(u64::from(array_vec[2usize]), 52u64);
        }
        {
            let array_vec = array_vec[8usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[9usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 53u64);
            assert_eq!(u64::from(array_vec[1usize]), 54u64);
            assert_eq!(u64::from(array_vec[2usize]), 55u64);
        }
        {
            let array_vec = array_vec[10usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[11usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 48u64);
            assert_eq!(u64::from(array_vec[1usize]), 57u64);
            assert_eq!(u64::from(array_vec[2usize]), 58u64);
        }
        {
            let array_vec = array_vec[12usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[13usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 59u64);
            assert_eq!(u64::from(array_vec[1usize]), 60u64);
            assert_eq!(u64::from(array_vec[2usize]), 61u64);
        }
        {
            let array_vec = array_vec[14usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[15usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 62u64);
            assert_eq!(u64::from(array_vec[1usize]), 63u64);
            assert_eq!(u64::from(array_vec[2usize]), 56u64);
        }
        {
            let array_vec = array_vec[16usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[17usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 65u64);
            assert_eq!(u64::from(array_vec[1usize]), 66u64);
            assert_eq!(u64::from(array_vec[2usize]), 67u64);
        }
        {
            let array_vec = array_vec[18usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[19usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 68u64);
            assert_eq!(u64::from(array_vec[1usize]), 69u64);
            assert_eq!(u64::from(array_vec[2usize]), 70u64);
        }
        {
            let array_vec = array_vec[20usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[21usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 71u64);
            assert_eq!(u64::from(array_vec[1usize]), 64u64);
            assert_eq!(u64::from(array_vec[2usize]), 73u64);
        }
        {
            let array_vec = array_vec[22usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[23usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 74u64);
            assert_eq!(u64::from(array_vec[1usize]), 75u64);
            assert_eq!(u64::from(array_vec[2usize]), 76u64);
        }
        {
            let array_vec = array_vec[24usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[25usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 77u64);
            assert_eq!(u64::from(array_vec[1usize]), 78u64);
            assert_eq!(u64::from(array_vec[2usize]), 79u64);
        }
        {
            let array_vec = array_vec[26usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[27usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 72u64);
            assert_eq!(u64::from(array_vec[1usize]), 81u64);
            assert_eq!(u64::from(array_vec[2usize]), 82u64);
        }
        {
            let array_vec = array_vec[28usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[29usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 83u64);
            assert_eq!(u64::from(array_vec[1usize]), 84u64);
            assert_eq!(u64::from(array_vec[2usize]), 85u64);
        }
        {
            let array_vec = array_vec[30usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[31usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 86u64);
            assert_eq!(u64::from(array_vec[1usize]), 87u64);
            assert_eq!(u64::from(array_vec[2usize]), 80u64);
        }
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_UnknownSize_1() {
    let base = hex_to_byte_string("");
    let Packet_Array_Field_UnsizedElement_UnknownSize_instance =
        Packet_Array_Field_UnsizedElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_UnknownSize_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_VariableSize_Padded_0() {
    let base = hex_to_byte_string("0000000000000000000000000000000000");
    let Packet_Array_Field_SizedElement_VariableSize_Padded_instance =
        Packet_Array_Field_SizedElement_VariableSize_PaddedView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_VariableSize_Padded_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_Packet_Array_Field_SizedElement_VariableSize_Padded_1() {
    let base = hex_to_byte_string("0e2e6338634a6b5c6b6e6b786b8a730000");
    let Packet_Array_Field_SizedElement_VariableSize_Padded_instance =
        Packet_Array_Field_SizedElement_VariableSize_PaddedView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_SizedElement_VariableSize_Padded_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        assert_eq!(u64::from(array_vec[0usize]), 25390u64);
        assert_eq!(u64::from(array_vec[1usize]), 25400u64);
        assert_eq!(u64::from(array_vec[2usize]), 27466u64);
        assert_eq!(u64::from(array_vec[3usize]), 27484u64);
        assert_eq!(u64::from(array_vec[4usize]), 27502u64);
        assert_eq!(u64::from(array_vec[5usize]), 27512u64);
        assert_eq!(u64::from(array_vec[6usize]), 29578u64);
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_VariableCount_Padded_0() {
    let base = hex_to_byte_string("07000373747500037677700003797a7b00");
    let Packet_Array_Field_UnsizedElement_VariableCount_Padded_instance =
        Packet_Array_Field_UnsizedElement_VariableCount_PaddedView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_VariableCount_Padded_instance
            .get_array_iter()
            .collect::<Vec<_>>();
        {
            let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 115u64);
            assert_eq!(u64::from(array_vec[1usize]), 116u64);
            assert_eq!(u64::from(array_vec[2usize]), 117u64);
        }
        {
            let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 118u64);
            assert_eq!(u64::from(array_vec[1usize]), 119u64);
            assert_eq!(u64::from(array_vec[2usize]), 112u64);
        }
        {
            let array_vec = array_vec[4usize].get_array_iter().collect::<Vec<_>>();
        }
        {
            let array_vec = array_vec[5usize].get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 121u64);
            assert_eq!(u64::from(array_vec[1usize]), 122u64);
            assert_eq!(u64::from(array_vec[2usize]), 123u64);
        }
        {
            let array_vec = array_vec[6usize].get_array_iter().collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Packet_Array_Field_UnsizedElement_VariableCount_Padded_1() {
    let base = hex_to_byte_string("0000000000000000000000000000000000");
    let Packet_Array_Field_UnsizedElement_VariableCount_Padded_instance =
        Packet_Array_Field_UnsizedElement_VariableCount_PaddedView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        let array_vec = Packet_Array_Field_UnsizedElement_VariableCount_Padded_instance
            .get_array_iter()
            .collect::<Vec<_>>();
    }
}

#[test]
fn test_ScalarParent_0() {
    let base = hex_to_byte_string("000100");
    let ScalarParent_instance =
        ScalarParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let ScalarChild_A_instance = ScalarChild_AView::try_parse(ScalarParent_instance).unwrap();
    {
        assert_eq!(u64::from(ScalarChild_A_instance.get_b()), 0u64);
    }
}

#[test]
fn test_ScalarParent_1() {
    let base = hex_to_byte_string("0001ff");
    let ScalarParent_instance =
        ScalarParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let ScalarChild_A_instance = ScalarChild_AView::try_parse(ScalarParent_instance).unwrap();
    {
        assert_eq!(u64::from(ScalarChild_A_instance.get_b()), 255u64);
    }
}

#[test]
fn test_ScalarParent_2() {
    let base = hex_to_byte_string("00017f");
    let ScalarParent_instance =
        ScalarParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let ScalarChild_A_instance = ScalarChild_AView::try_parse(ScalarParent_instance).unwrap();
    {
        assert_eq!(u64::from(ScalarChild_A_instance.get_b()), 127u64);
    }
}

#[test]
fn test_ScalarParent_3() {
    let base = hex_to_byte_string("01020000");
    let ScalarParent_instance =
        ScalarParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let ScalarChild_B_instance = ScalarChild_BView::try_parse(ScalarParent_instance).unwrap();
    {
        assert_eq!(u64::from(ScalarChild_B_instance.get_c()), 0u64);
    }
}

#[test]
fn test_ScalarParent_4() {
    let base = hex_to_byte_string("0102ffff");
    let ScalarParent_instance =
        ScalarParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let ScalarChild_B_instance = ScalarChild_BView::try_parse(ScalarParent_instance).unwrap();
    {
        assert_eq!(u64::from(ScalarChild_B_instance.get_c()), 65535u64);
    }
}

#[test]
fn test_ScalarParent_5() {
    let base = hex_to_byte_string("0102017c");
    let ScalarParent_instance =
        ScalarParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let ScalarChild_B_instance = ScalarChild_BView::try_parse(ScalarParent_instance).unwrap();
    {
        assert_eq!(u64::from(ScalarChild_B_instance.get_c()), 31745u64);
    }
}

#[test]
fn test_EnumParent_0() {
    let base = hex_to_byte_string("bbaa0100");
    let EnumParent_instance =
        EnumParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let EnumChild_A_instance = EnumChild_AView::try_parse(EnumParent_instance).unwrap();
    {
        assert_eq!(u64::from(EnumChild_A_instance.get_b()), 0u64);
    }
}

#[test]
fn test_EnumParent_1() {
    let base = hex_to_byte_string("bbaa01ff");
    let EnumParent_instance =
        EnumParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let EnumChild_A_instance = EnumChild_AView::try_parse(EnumParent_instance).unwrap();
    {
        assert_eq!(u64::from(EnumChild_A_instance.get_b()), 255u64);
    }
}

#[test]
fn test_EnumParent_2() {
    let base = hex_to_byte_string("bbaa0182");
    let EnumParent_instance =
        EnumParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let EnumChild_A_instance = EnumChild_AView::try_parse(EnumParent_instance).unwrap();
    {
        assert_eq!(u64::from(EnumChild_A_instance.get_b()), 130u64);
    }
}

#[test]
fn test_EnumParent_3() {
    let base = hex_to_byte_string("ddcc020000");
    let EnumParent_instance =
        EnumParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let EnumChild_B_instance = EnumChild_BView::try_parse(EnumParent_instance).unwrap();
    {
        assert_eq!(u64::from(EnumChild_B_instance.get_c()), 0u64);
    }
}

#[test]
fn test_EnumParent_4() {
    let base = hex_to_byte_string("ddcc02ffff");
    let EnumParent_instance =
        EnumParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let EnumChild_B_instance = EnumChild_BView::try_parse(EnumParent_instance).unwrap();
    {
        assert_eq!(u64::from(EnumChild_B_instance.get_c()), 65535u64);
    }
}

#[test]
fn test_EnumParent_5() {
    let base = hex_to_byte_string("ddcc021c84");
    let EnumParent_instance =
        EnumParentView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let EnumChild_B_instance = EnumChild_BView::try_parse(EnumParent_instance).unwrap();
    {
        assert_eq!(u64::from(EnumChild_B_instance.get_c()), 33820u64);
    }
}

#[test]
fn test_PartialParent5_0() {
    let base = hex_to_byte_string("0000");
    let PartialParent5_instance =
        PartialParent5View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild5_A_instance = PartialChild5_AView::try_parse(PartialParent5_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild5_A_instance.get_b()), 0u64);
    }
}

#[test]
fn test_PartialParent5_1() {
    let base = hex_to_byte_string("e0ff");
    let PartialParent5_instance =
        PartialParent5View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild5_A_instance = PartialChild5_AView::try_parse(PartialParent5_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild5_A_instance.get_b()), 2047u64);
    }
}

#[test]
fn test_PartialParent5_2() {
    let base = hex_to_byte_string("0081");
    let PartialParent5_instance =
        PartialParent5View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild5_A_instance = PartialChild5_AView::try_parse(PartialParent5_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild5_A_instance.get_b()), 1032u64);
    }
}

#[test]
fn test_PartialParent5_3() {
    let base = hex_to_byte_string("01000000");
    let PartialParent5_instance =
        PartialParent5View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild5_B_instance = PartialChild5_BView::try_parse(PartialParent5_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild5_B_instance.get_c()), 0u64);
    }
}

#[test]
fn test_PartialParent5_4() {
    let base = hex_to_byte_string("e1ffffff");
    let PartialParent5_instance =
        PartialParent5View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild5_B_instance = PartialChild5_BView::try_parse(PartialParent5_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild5_B_instance.get_c()), 134217727u64);
    }
}

#[test]
fn test_PartialParent5_5() {
    let base = hex_to_byte_string("c1a262a2");
    let PartialParent5_instance =
        PartialParent5View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild5_B_instance = PartialChild5_BView::try_parse(PartialParent5_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild5_B_instance.get_c()), 85136662u64);
    }
}

#[test]
fn test_PartialParent12_0() {
    let base = hex_to_byte_string("0200");
    let PartialParent12_instance =
        PartialParent12View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild12_A_instance =
        PartialChild12_AView::try_parse(PartialParent12_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild12_A_instance.get_d()), 0u64);
    }
}

#[test]
fn test_PartialParent12_1() {
    let base = hex_to_byte_string("02f0");
    let PartialParent12_instance =
        PartialParent12View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild12_A_instance =
        PartialChild12_AView::try_parse(PartialParent12_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild12_A_instance.get_d()), 15u64);
    }
}

#[test]
fn test_PartialParent12_2() {
    let base = hex_to_byte_string("0260");
    let PartialParent12_instance =
        PartialParent12View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild12_A_instance =
        PartialChild12_AView::try_parse(PartialParent12_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild12_A_instance.get_d()), 6u64);
    }
}

#[test]
fn test_PartialParent12_3() {
    let base = hex_to_byte_string("03000000");
    let PartialParent12_instance =
        PartialParent12View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild12_B_instance =
        PartialChild12_BView::try_parse(PartialParent12_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild12_B_instance.get_e()), 0u64);
    }
}

#[test]
fn test_PartialParent12_4() {
    let base = hex_to_byte_string("03f0ffff");
    let PartialParent12_instance =
        PartialParent12View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild12_B_instance =
        PartialChild12_BView::try_parse(PartialParent12_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild12_B_instance.get_e()), 1048575u64);
    }
}

#[test]
fn test_PartialParent12_5() {
    let base = hex_to_byte_string("03d0b191");
    let PartialParent12_instance =
        PartialParent12View::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    let PartialChild12_B_instance =
        PartialChild12_BView::try_parse(PartialParent12_instance).unwrap();
    {
        assert_eq!(u64::from(PartialChild12_B_instance.get_e()), 596765u64);
    }
}

#[test]
fn test_Struct_Enum_Field_0() {
    let base = hex_to_byte_string("0100000000000000");
    let Struct_Enum_Field_instance =
        Struct_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_a()), 1u64);
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_c()), 0u64);
        }
    }
}

#[test]
fn test_Struct_Enum_Field_1() {
    let base = hex_to_byte_string("81ffffffffffffff");
    let Struct_Enum_Field_instance =
        Struct_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_a()), 1u64);
            assert_eq!(
                u64::from(Struct_Enum_Field_instance.get_s().get_c()),
                144115188075855871u64
            );
        }
    }
}

#[test]
fn test_Struct_Enum_Field_2() {
    let base = hex_to_byte_string("012b29272523218f");
    let Struct_Enum_Field_instance =
        Struct_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_a()), 1u64);
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_c()), 80574713001038422u64);
        }
    }
}

#[test]
fn test_Struct_Enum_Field_3() {
    let base = hex_to_byte_string("0200000000000000");
    let Struct_Enum_Field_instance =
        Struct_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_c()), 0u64);
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_a()), 2u64);
        }
    }
}

#[test]
fn test_Struct_Enum_Field_4() {
    let base = hex_to_byte_string("82ffffffffffffff");
    let Struct_Enum_Field_instance =
        Struct_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_a()), 2u64);
            assert_eq!(
                u64::from(Struct_Enum_Field_instance.get_s().get_c()),
                144115188075855871u64
            );
        }
    }
}

#[test]
fn test_Struct_Enum_Field_5() {
    let base = hex_to_byte_string("022b29272523218f");
    let Struct_Enum_Field_instance =
        Struct_Enum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_a()), 2u64);
            assert_eq!(u64::from(Struct_Enum_Field_instance.get_s().get_c()), 80574713001038422u64);
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_0() {
    let base = hex_to_byte_string("0000000000000000");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 0u64);
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_c()), 0u64);
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_1() {
    let base = hex_to_byte_string("00feffffffffffff");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 0u64);
            assert_eq!(
                u64::from(Struct_Reserved_Field_instance.get_s().get_c()),
                36028797018963967u64
            );
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_2() {
    let base = hex_to_byte_string("003a393735333197");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 0u64);
            assert_eq!(
                u64::from(Struct_Reserved_Field_instance.get_s().get_c()),
                21278408744606877u64
            );
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_3() {
    let base = hex_to_byte_string("7f00000000000000");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_c()), 0u64);
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 127u64);
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_4() {
    let base = hex_to_byte_string("7ffeffffffffffff");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_Reserved_Field_instance.get_s().get_c()),
                36028797018963967u64
            );
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 127u64);
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_5() {
    let base = hex_to_byte_string("7f3a393735333197");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 127u64);
            assert_eq!(
                u64::from(Struct_Reserved_Field_instance.get_s().get_c()),
                21278408744606877u64
            );
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_6() {
    let base = hex_to_byte_string("4b00000000000000");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_c()), 0u64);
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 75u64);
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_7() {
    let base = hex_to_byte_string("4bfeffffffffffff");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 75u64);
            assert_eq!(
                u64::from(Struct_Reserved_Field_instance.get_s().get_c()),
                36028797018963967u64
            );
        }
    }
}

#[test]
fn test_Struct_Reserved_Field_8() {
    let base = hex_to_byte_string("4b3a393735333197");
    let Struct_Reserved_Field_instance =
        Struct_Reserved_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_Reserved_Field_instance.get_s().get_c()),
                21278408744606877u64
            );
            assert_eq!(u64::from(Struct_Reserved_Field_instance.get_s().get_a()), 75u64);
        }
    }
}

#[test]
fn test_Struct_Size_Field_0() {
    let base = hex_to_byte_string("0000000000000000");
    let Struct_Size_Field_instance =
        Struct_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Size_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(Struct_Size_Field_instance.get_s().get_a()), 0u64);
        }
    }
}

#[test]
fn test_Struct_Size_Field_1() {
    let base = hex_to_byte_string("0700000000000000a6a7a8a9aaabac");
    let Struct_Size_Field_instance =
        Struct_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Size_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(b_vec[0usize]), 166u64);
            assert_eq!(u64::from(b_vec[1usize]), 167u64);
            assert_eq!(u64::from(b_vec[2usize]), 168u64);
            assert_eq!(u64::from(b_vec[3usize]), 169u64);
            assert_eq!(u64::from(b_vec[4usize]), 170u64);
            assert_eq!(u64::from(b_vec[5usize]), 171u64);
            assert_eq!(u64::from(b_vec[6usize]), 172u64);
            assert_eq!(u64::from(Struct_Size_Field_instance.get_s().get_a()), 0u64);
        }
    }
}

#[test]
fn test_Struct_Size_Field_2() {
    let base = hex_to_byte_string("f8ffffffffffffff");
    let Struct_Size_Field_instance =
        Struct_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Size_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(
                u64::from(Struct_Size_Field_instance.get_s().get_a()),
                2305843009213693951u64
            );
        }
    }
}

#[test]
fn test_Struct_Size_Field_3() {
    let base = hex_to_byte_string("ffffffffffffffffa6a7a8a9aaabac");
    let Struct_Size_Field_instance =
        Struct_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_Size_Field_instance.get_s().get_a()),
                2305843009213693951u64
            );
            let b_vec = Struct_Size_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(b_vec[0usize]), 166u64);
            assert_eq!(u64::from(b_vec[1usize]), 167u64);
            assert_eq!(u64::from(b_vec[2usize]), 168u64);
            assert_eq!(u64::from(b_vec[3usize]), 169u64);
            assert_eq!(u64::from(b_vec[4usize]), 170u64);
            assert_eq!(u64::from(b_vec[5usize]), 171u64);
            assert_eq!(u64::from(b_vec[6usize]), 172u64);
        }
    }
}

#[test]
fn test_Struct_Size_Field_4() {
    let base = hex_to_byte_string("28a4a3a2a1a09f9e");
    let Struct_Size_Field_instance =
        Struct_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Size_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(
                u64::from(Struct_Size_Field_instance.get_s().get_a()),
                1428753874421052549u64
            );
        }
    }
}

#[test]
fn test_Struct_Size_Field_5() {
    let base = hex_to_byte_string("2fa4a3a2a1a09f9ea6a7a8a9aaabac");
    let Struct_Size_Field_instance =
        Struct_Size_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Size_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(b_vec[0usize]), 166u64);
            assert_eq!(u64::from(b_vec[1usize]), 167u64);
            assert_eq!(u64::from(b_vec[2usize]), 168u64);
            assert_eq!(u64::from(b_vec[3usize]), 169u64);
            assert_eq!(u64::from(b_vec[4usize]), 170u64);
            assert_eq!(u64::from(b_vec[5usize]), 171u64);
            assert_eq!(u64::from(b_vec[6usize]), 172u64);
            assert_eq!(
                u64::from(Struct_Size_Field_instance.get_s().get_a()),
                1428753874421052549u64
            );
        }
    }
}

#[test]
fn test_Struct_Count_Field_0() {
    let base = hex_to_byte_string("0000000000000000");
    let Struct_Count_Field_instance =
        Struct_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Count_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(Struct_Count_Field_instance.get_s().get_a()), 0u64);
        }
    }
}

#[test]
fn test_Struct_Count_Field_1() {
    let base = hex_to_byte_string("0700000000000000b5b6b7b4b9babb");
    let Struct_Count_Field_instance =
        Struct_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Count_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(b_vec[0usize]), 181u64);
            assert_eq!(u64::from(b_vec[1usize]), 182u64);
            assert_eq!(u64::from(b_vec[2usize]), 183u64);
            assert_eq!(u64::from(b_vec[3usize]), 180u64);
            assert_eq!(u64::from(b_vec[4usize]), 185u64);
            assert_eq!(u64::from(b_vec[5usize]), 186u64);
            assert_eq!(u64::from(b_vec[6usize]), 187u64);
            assert_eq!(u64::from(Struct_Count_Field_instance.get_s().get_a()), 0u64);
        }
    }
}

#[test]
fn test_Struct_Count_Field_2() {
    let base = hex_to_byte_string("f8ffffffffffffff");
    let Struct_Count_Field_instance =
        Struct_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Count_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(
                u64::from(Struct_Count_Field_instance.get_s().get_a()),
                2305843009213693951u64
            );
        }
    }
}

#[test]
fn test_Struct_Count_Field_3() {
    let base = hex_to_byte_string("ffffffffffffffffb5b6b7b4b9babb");
    let Struct_Count_Field_instance =
        Struct_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_Count_Field_instance.get_s().get_a()),
                2305843009213693951u64
            );
            let b_vec = Struct_Count_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(b_vec[0usize]), 181u64);
            assert_eq!(u64::from(b_vec[1usize]), 182u64);
            assert_eq!(u64::from(b_vec[2usize]), 183u64);
            assert_eq!(u64::from(b_vec[3usize]), 180u64);
            assert_eq!(u64::from(b_vec[4usize]), 185u64);
            assert_eq!(u64::from(b_vec[5usize]), 186u64);
            assert_eq!(u64::from(b_vec[6usize]), 187u64);
        }
    }
}

#[test]
fn test_Struct_Count_Field_4() {
    let base = hex_to_byte_string("60563616f6d5b5b5");
    let Struct_Count_Field_instance =
        Struct_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_Count_Field_instance.get_s().get_a()),
                1636700843070114508u64
            );
            let b_vec = Struct_Count_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Count_Field_5() {
    let base = hex_to_byte_string("67563616f6d5b5b5b5b6b7b4b9babb");
    let Struct_Count_Field_instance =
        Struct_Count_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let b_vec = Struct_Count_Field_instance.get_s().get_b_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(b_vec[0usize]), 181u64);
            assert_eq!(u64::from(b_vec[1usize]), 182u64);
            assert_eq!(u64::from(b_vec[2usize]), 183u64);
            assert_eq!(u64::from(b_vec[3usize]), 180u64);
            assert_eq!(u64::from(b_vec[4usize]), 185u64);
            assert_eq!(u64::from(b_vec[5usize]), 186u64);
            assert_eq!(u64::from(b_vec[6usize]), 187u64);
            assert_eq!(
                u64::from(Struct_Count_Field_instance.get_s().get_a()),
                1636700843070114508u64
            );
        }
    }
}

#[test]
fn test_Struct_FixedScalar_Field_0() {
    let base = hex_to_byte_string("0700000000000000");
    let Struct_FixedScalar_Field_instance =
        Struct_FixedScalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_FixedScalar_Field_instance.get_s().get_b()), 0u64);
        }
    }
}

#[test]
fn test_Struct_FixedScalar_Field_1() {
    let base = hex_to_byte_string("87ffffffffffffff");
    let Struct_FixedScalar_Field_instance =
        Struct_FixedScalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_FixedScalar_Field_instance.get_s().get_b()),
                144115188075855871u64
            );
        }
    }
}

#[test]
fn test_Struct_FixedScalar_Field_2() {
    let base = hex_to_byte_string("070503fffaf6f2ba");
    let Struct_FixedScalar_Field_instance =
        Struct_FixedScalar_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_FixedScalar_Field_instance.get_s().get_b()),
                105242976510150154u64
            );
        }
    }
}

#[test]
fn test_Struct_FixedEnum_Field_0() {
    let base = hex_to_byte_string("0100000000000000");
    let Struct_FixedEnum_Field_instance =
        Struct_FixedEnum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_FixedEnum_Field_instance.get_s().get_b()), 0u64);
        }
    }
}

#[test]
fn test_Struct_FixedEnum_Field_1() {
    let base = hex_to_byte_string("81ffffffffffffff");
    let Struct_FixedEnum_Field_instance =
        Struct_FixedEnum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_FixedEnum_Field_instance.get_s().get_b()),
                144115188075855871u64
            );
        }
    }
}

#[test]
fn test_Struct_FixedEnum_Field_2() {
    let base = hex_to_byte_string("81443e362e261ec6");
    let Struct_FixedEnum_Field_instance =
        Struct_FixedEnum_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(
                u64::from(Struct_FixedEnum_Field_instance.get_s().get_b()),
                111530389443214473u64
            );
        }
    }
}

#[test]
fn test_Struct_Struct_Field_0() {
    let base = hex_to_byte_string("0000");
    let Struct_Struct_Field_instance =
        Struct_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let array_vec =
                Struct_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
        }
        {
            assert_eq!(u64::from(Struct_Struct_Field_instance.get_a().get_a()), 0u64);
        }
    }
}

#[test]
fn test_Struct_Struct_Field_1() {
    let base = hex_to_byte_string("0003d8d9da");
    let Struct_Struct_Field_instance =
        Struct_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Struct_Field_instance.get_a().get_a()), 0u64);
        }
        {
            let array_vec =
                Struct_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 216u64);
            assert_eq!(u64::from(array_vec[1usize]), 217u64);
            assert_eq!(u64::from(array_vec[2usize]), 218u64);
        }
    }
}

#[test]
fn test_Struct_Struct_Field_2() {
    let base = hex_to_byte_string("ff00");
    let Struct_Struct_Field_instance =
        Struct_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let array_vec =
                Struct_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
        }
        {
            assert_eq!(u64::from(Struct_Struct_Field_instance.get_a().get_a()), 255u64);
        }
    }
}

#[test]
fn test_Struct_Struct_Field_3() {
    let base = hex_to_byte_string("ff03d8d9da");
    let Struct_Struct_Field_instance =
        Struct_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Struct_Field_instance.get_a().get_a()), 255u64);
        }
        {
            let array_vec =
                Struct_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 216u64);
            assert_eq!(u64::from(array_vec[1usize]), 217u64);
            assert_eq!(u64::from(array_vec[2usize]), 218u64);
        }
    }
}

#[test]
fn test_Struct_Struct_Field_4() {
    let base = hex_to_byte_string("d700");
    let Struct_Struct_Field_instance =
        Struct_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            assert_eq!(u64::from(Struct_Struct_Field_instance.get_a().get_a()), 215u64);
        }
        {
            let array_vec =
                Struct_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Struct_Field_5() {
    let base = hex_to_byte_string("d703d8d9da");
    let Struct_Struct_Field_instance =
        Struct_Struct_FieldView::try_parse(SizedBitSlice::from(&base[..]).into()).unwrap();
    {
        {
            let array_vec =
                Struct_Struct_Field_instance.get_b().get_array_iter().collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 216u64);
            assert_eq!(u64::from(array_vec[1usize]), 217u64);
            assert_eq!(u64::from(array_vec[2usize]), 218u64);
        }
        {
            assert_eq!(u64::from(Struct_Struct_Field_instance.get_a().get_a()), 215u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_ByteElement_ConstantSize_0() {
    let base = hex_to_byte_string("dbdcddde");
    let Struct_Array_Field_ByteElement_ConstantSize_instance =
        Struct_Array_Field_ByteElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ByteElement_ConstantSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 219u64);
            assert_eq!(u64::from(array_vec[1usize]), 220u64);
            assert_eq!(u64::from(array_vec[2usize]), 221u64);
            assert_eq!(u64::from(array_vec[3usize]), 222u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_ByteElement_VariableSize_0() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_ByteElement_VariableSize_instance =
        Struct_Array_Field_ByteElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ByteElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_ByteElement_VariableSize_1() {
    let base = hex_to_byte_string("0fdfd0e1e2e3e4e5e6e7e8e9eaebeced");
    let Struct_Array_Field_ByteElement_VariableSize_instance =
        Struct_Array_Field_ByteElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ByteElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 223u64);
            assert_eq!(u64::from(array_vec[1usize]), 208u64);
            assert_eq!(u64::from(array_vec[2usize]), 225u64);
            assert_eq!(u64::from(array_vec[3usize]), 226u64);
            assert_eq!(u64::from(array_vec[4usize]), 227u64);
            assert_eq!(u64::from(array_vec[5usize]), 228u64);
            assert_eq!(u64::from(array_vec[6usize]), 229u64);
            assert_eq!(u64::from(array_vec[7usize]), 230u64);
            assert_eq!(u64::from(array_vec[8usize]), 231u64);
            assert_eq!(u64::from(array_vec[9usize]), 232u64);
            assert_eq!(u64::from(array_vec[10usize]), 233u64);
            assert_eq!(u64::from(array_vec[11usize]), 234u64);
            assert_eq!(u64::from(array_vec[12usize]), 235u64);
            assert_eq!(u64::from(array_vec[13usize]), 236u64);
            assert_eq!(u64::from(array_vec[14usize]), 237u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_ByteElement_VariableCount_0() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_ByteElement_VariableCount_instance =
        Struct_Array_Field_ByteElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ByteElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_ByteElement_VariableCount_1() {
    let base = hex_to_byte_string("0feeefe0f1f2f3f4f5f6f7f8f9fafbfc");
    let Struct_Array_Field_ByteElement_VariableCount_instance =
        Struct_Array_Field_ByteElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ByteElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 238u64);
            assert_eq!(u64::from(array_vec[1usize]), 239u64);
            assert_eq!(u64::from(array_vec[2usize]), 224u64);
            assert_eq!(u64::from(array_vec[3usize]), 241u64);
            assert_eq!(u64::from(array_vec[4usize]), 242u64);
            assert_eq!(u64::from(array_vec[5usize]), 243u64);
            assert_eq!(u64::from(array_vec[6usize]), 244u64);
            assert_eq!(u64::from(array_vec[7usize]), 245u64);
            assert_eq!(u64::from(array_vec[8usize]), 246u64);
            assert_eq!(u64::from(array_vec[9usize]), 247u64);
            assert_eq!(u64::from(array_vec[10usize]), 248u64);
            assert_eq!(u64::from(array_vec[11usize]), 249u64);
            assert_eq!(u64::from(array_vec[12usize]), 250u64);
            assert_eq!(u64::from(array_vec[13usize]), 251u64);
            assert_eq!(u64::from(array_vec[14usize]), 252u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_ByteElement_UnknownSize_0() {
    let base = hex_to_byte_string("");
    let Struct_Array_Field_ByteElement_UnknownSize_instance =
        Struct_Array_Field_ByteElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ByteElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_ByteElement_UnknownSize_1() {
    let base = hex_to_byte_string("fdfef0");
    let Struct_Array_Field_ByteElement_UnknownSize_instance =
        Struct_Array_Field_ByteElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ByteElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 253u64);
            assert_eq!(u64::from(array_vec[1usize]), 254u64);
            assert_eq!(u64::from(array_vec[2usize]), 240u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_ScalarElement_ConstantSize_0() {
    let base = hex_to_byte_string("1200340056007800");
    let Struct_Array_Field_ScalarElement_ConstantSize_instance =
        Struct_Array_Field_ScalarElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ScalarElement_ConstantSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 18u64);
            assert_eq!(u64::from(array_vec[1usize]), 52u64);
            assert_eq!(u64::from(array_vec[2usize]), 86u64);
            assert_eq!(u64::from(array_vec[3usize]), 120u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_ScalarElement_VariableSize_0() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_ScalarElement_VariableSize_instance =
        Struct_Array_Field_ScalarElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ScalarElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_ScalarElement_VariableSize_1() {
    let base = hex_to_byte_string("0e9a00bc00de00f000121134115611");
    let Struct_Array_Field_ScalarElement_VariableSize_instance =
        Struct_Array_Field_ScalarElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ScalarElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 154u64);
            assert_eq!(u64::from(array_vec[1usize]), 188u64);
            assert_eq!(u64::from(array_vec[2usize]), 222u64);
            assert_eq!(u64::from(array_vec[3usize]), 240u64);
            assert_eq!(u64::from(array_vec[4usize]), 4370u64);
            assert_eq!(u64::from(array_vec[5usize]), 4404u64);
            assert_eq!(u64::from(array_vec[6usize]), 4438u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_ScalarElement_VariableCount_0() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_ScalarElement_VariableCount_instance =
        Struct_Array_Field_ScalarElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ScalarElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_ScalarElement_VariableCount_1() {
    let base = hex_to_byte_string("0f78119a11bc11de11f01112223422562278229a22bc22de22f02212333433");
    let Struct_Array_Field_ScalarElement_VariableCount_instance =
        Struct_Array_Field_ScalarElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ScalarElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 4472u64);
            assert_eq!(u64::from(array_vec[1usize]), 4506u64);
            assert_eq!(u64::from(array_vec[2usize]), 4540u64);
            assert_eq!(u64::from(array_vec[3usize]), 4574u64);
            assert_eq!(u64::from(array_vec[4usize]), 4592u64);
            assert_eq!(u64::from(array_vec[5usize]), 8722u64);
            assert_eq!(u64::from(array_vec[6usize]), 8756u64);
            assert_eq!(u64::from(array_vec[7usize]), 8790u64);
            assert_eq!(u64::from(array_vec[8usize]), 8824u64);
            assert_eq!(u64::from(array_vec[9usize]), 8858u64);
            assert_eq!(u64::from(array_vec[10usize]), 8892u64);
            assert_eq!(u64::from(array_vec[11usize]), 8926u64);
            assert_eq!(u64::from(array_vec[12usize]), 8944u64);
            assert_eq!(u64::from(array_vec[13usize]), 13074u64);
            assert_eq!(u64::from(array_vec[14usize]), 13108u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_ScalarElement_UnknownSize_0() {
    let base = hex_to_byte_string("");
    let Struct_Array_Field_ScalarElement_UnknownSize_instance =
        Struct_Array_Field_ScalarElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ScalarElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_ScalarElement_UnknownSize_1() {
    let base = hex_to_byte_string("563378339a33");
    let Struct_Array_Field_ScalarElement_UnknownSize_instance =
        Struct_Array_Field_ScalarElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_ScalarElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 13142u64);
            assert_eq!(u64::from(array_vec[1usize]), 13176u64);
            assert_eq!(u64::from(array_vec[2usize]), 13210u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_EnumElement_ConstantSize_0() {
    let base = hex_to_byte_string("bbaaddccbbaaddcc");
    let Struct_Array_Field_EnumElement_ConstantSize_instance =
        Struct_Array_Field_EnumElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_EnumElement_ConstantSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 43707u64);
            assert_eq!(u64::from(array_vec[1usize]), 52445u64);
            assert_eq!(u64::from(array_vec[2usize]), 43707u64);
            assert_eq!(u64::from(array_vec[3usize]), 52445u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_EnumElement_VariableSize_0() {
    let base = hex_to_byte_string("0ebbaaddccbbaaddccbbaaddccbbaa");
    let Struct_Array_Field_EnumElement_VariableSize_instance =
        Struct_Array_Field_EnumElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_EnumElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 43707u64);
            assert_eq!(u64::from(array_vec[1usize]), 52445u64);
            assert_eq!(u64::from(array_vec[2usize]), 43707u64);
            assert_eq!(u64::from(array_vec[3usize]), 52445u64);
            assert_eq!(u64::from(array_vec[4usize]), 43707u64);
            assert_eq!(u64::from(array_vec[5usize]), 52445u64);
            assert_eq!(u64::from(array_vec[6usize]), 43707u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_EnumElement_VariableSize_1() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_EnumElement_VariableSize_instance =
        Struct_Array_Field_EnumElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_EnumElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_EnumElement_VariableCount_0() {
    let base = hex_to_byte_string("0fbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaa");
    let Struct_Array_Field_EnumElement_VariableCount_instance =
        Struct_Array_Field_EnumElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_EnumElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 43707u64);
            assert_eq!(u64::from(array_vec[1usize]), 52445u64);
            assert_eq!(u64::from(array_vec[2usize]), 43707u64);
            assert_eq!(u64::from(array_vec[3usize]), 52445u64);
            assert_eq!(u64::from(array_vec[4usize]), 43707u64);
            assert_eq!(u64::from(array_vec[5usize]), 52445u64);
            assert_eq!(u64::from(array_vec[6usize]), 43707u64);
            assert_eq!(u64::from(array_vec[7usize]), 52445u64);
            assert_eq!(u64::from(array_vec[8usize]), 43707u64);
            assert_eq!(u64::from(array_vec[9usize]), 52445u64);
            assert_eq!(u64::from(array_vec[10usize]), 43707u64);
            assert_eq!(u64::from(array_vec[11usize]), 52445u64);
            assert_eq!(u64::from(array_vec[12usize]), 43707u64);
            assert_eq!(u64::from(array_vec[13usize]), 52445u64);
            assert_eq!(u64::from(array_vec[14usize]), 43707u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_EnumElement_VariableCount_1() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_EnumElement_VariableCount_instance =
        Struct_Array_Field_EnumElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_EnumElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_EnumElement_UnknownSize_0() {
    let base = hex_to_byte_string ("bbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddccbbaaddcc") ;
    let Struct_Array_Field_EnumElement_UnknownSize_instance =
        Struct_Array_Field_EnumElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_EnumElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 43707u64);
            assert_eq!(u64::from(array_vec[1usize]), 52445u64);
            assert_eq!(u64::from(array_vec[2usize]), 43707u64);
            assert_eq!(u64::from(array_vec[3usize]), 52445u64);
            assert_eq!(u64::from(array_vec[4usize]), 43707u64);
            assert_eq!(u64::from(array_vec[5usize]), 52445u64);
            assert_eq!(u64::from(array_vec[6usize]), 43707u64);
            assert_eq!(u64::from(array_vec[7usize]), 52445u64);
            assert_eq!(u64::from(array_vec[8usize]), 43707u64);
            assert_eq!(u64::from(array_vec[9usize]), 52445u64);
            assert_eq!(u64::from(array_vec[10usize]), 43707u64);
            assert_eq!(u64::from(array_vec[11usize]), 52445u64);
            assert_eq!(u64::from(array_vec[12usize]), 43707u64);
            assert_eq!(u64::from(array_vec[13usize]), 52445u64);
            assert_eq!(u64::from(array_vec[14usize]), 43707u64);
            assert_eq!(u64::from(array_vec[15usize]), 52445u64);
            assert_eq!(u64::from(array_vec[16usize]), 43707u64);
            assert_eq!(u64::from(array_vec[17usize]), 52445u64);
            assert_eq!(u64::from(array_vec[18usize]), 43707u64);
            assert_eq!(u64::from(array_vec[19usize]), 52445u64);
            assert_eq!(u64::from(array_vec[20usize]), 43707u64);
            assert_eq!(u64::from(array_vec[21usize]), 52445u64);
            assert_eq!(u64::from(array_vec[22usize]), 43707u64);
            assert_eq!(u64::from(array_vec[23usize]), 52445u64);
            assert_eq!(u64::from(array_vec[24usize]), 43707u64);
            assert_eq!(u64::from(array_vec[25usize]), 52445u64);
            assert_eq!(u64::from(array_vec[26usize]), 43707u64);
            assert_eq!(u64::from(array_vec[27usize]), 52445u64);
            assert_eq!(u64::from(array_vec[28usize]), 43707u64);
            assert_eq!(u64::from(array_vec[29usize]), 52445u64);
            assert_eq!(u64::from(array_vec[30usize]), 43707u64);
            assert_eq!(u64::from(array_vec[31usize]), 52445u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_EnumElement_UnknownSize_1() {
    let base = hex_to_byte_string("");
    let Struct_Array_Field_EnumElement_UnknownSize_instance =
        Struct_Array_Field_EnumElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_EnumElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_ConstantSize_0() {
    let base = hex_to_byte_string("00ff3b00");
    let Struct_Array_Field_SizedElement_ConstantSize_instance =
        Struct_Array_Field_SizedElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_ConstantSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                assert_eq!(u64::from(array_vec[0usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[1usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[2usize].get_a()), 59u64);
            }
            {
                assert_eq!(u64::from(array_vec[3usize].get_a()), 0u64);
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_VariableSize_0() {
    let base = hex_to_byte_string("0f00ff3d00ff3e00ff3f00ff3000ff41");
    let Struct_Array_Field_SizedElement_VariableSize_instance =
        Struct_Array_Field_SizedElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                assert_eq!(u64::from(array_vec[0usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[1usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[2usize].get_a()), 61u64);
            }
            {
                assert_eq!(u64::from(array_vec[3usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[4usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[5usize].get_a()), 62u64);
            }
            {
                assert_eq!(u64::from(array_vec[6usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[7usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[8usize].get_a()), 63u64);
            }
            {
                assert_eq!(u64::from(array_vec[9usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[10usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[11usize].get_a()), 48u64);
            }
            {
                assert_eq!(u64::from(array_vec[12usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[13usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[14usize].get_a()), 65u64);
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_VariableSize_1() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_SizedElement_VariableSize_instance =
        Struct_Array_Field_SizedElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_VariableCount_0() {
    let base = hex_to_byte_string("0f00ff4300ff4400ff4500ff4600ff47");
    let Struct_Array_Field_SizedElement_VariableCount_instance =
        Struct_Array_Field_SizedElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                assert_eq!(u64::from(array_vec[0usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[1usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[2usize].get_a()), 67u64);
            }
            {
                assert_eq!(u64::from(array_vec[3usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[4usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[5usize].get_a()), 68u64);
            }
            {
                assert_eq!(u64::from(array_vec[6usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[7usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[8usize].get_a()), 69u64);
            }
            {
                assert_eq!(u64::from(array_vec[9usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[10usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[11usize].get_a()), 70u64);
            }
            {
                assert_eq!(u64::from(array_vec[12usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[13usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[14usize].get_a()), 71u64);
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_VariableCount_1() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_SizedElement_VariableCount_instance =
        Struct_Array_Field_SizedElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_UnknownSize_0() {
    let base =
        hex_to_byte_string("00ff4900ff4a00ff4b00ff4c00ff4d00ff4e00ff4f00ff4000ff5100ff5200ff");
    let Struct_Array_Field_SizedElement_UnknownSize_instance =
        Struct_Array_Field_SizedElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                assert_eq!(u64::from(array_vec[0usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[1usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[2usize].get_a()), 73u64);
            }
            {
                assert_eq!(u64::from(array_vec[3usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[4usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[5usize].get_a()), 74u64);
            }
            {
                assert_eq!(u64::from(array_vec[6usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[7usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[8usize].get_a()), 75u64);
            }
            {
                assert_eq!(u64::from(array_vec[9usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[10usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[11usize].get_a()), 76u64);
            }
            {
                assert_eq!(u64::from(array_vec[12usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[13usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[14usize].get_a()), 77u64);
            }
            {
                assert_eq!(u64::from(array_vec[15usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[16usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[17usize].get_a()), 78u64);
            }
            {
                assert_eq!(u64::from(array_vec[18usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[19usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[20usize].get_a()), 79u64);
            }
            {
                assert_eq!(u64::from(array_vec[21usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[22usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[23usize].get_a()), 64u64);
            }
            {
                assert_eq!(u64::from(array_vec[24usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[25usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[26usize].get_a()), 81u64);
            }
            {
                assert_eq!(u64::from(array_vec[27usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[28usize].get_a()), 255u64);
            }
            {
                assert_eq!(u64::from(array_vec[29usize].get_a()), 82u64);
            }
            {
                assert_eq!(u64::from(array_vec[30usize].get_a()), 0u64);
            }
            {
                assert_eq!(u64::from(array_vec[31usize].get_a()), 255u64);
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_UnknownSize_1() {
    let base = hex_to_byte_string("");
    let Struct_Array_Field_SizedElement_UnknownSize_instance =
        Struct_Array_Field_SizedElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_ConstantSize_0() {
    let base = hex_to_byte_string("00035455560003575859");
    let Struct_Array_Field_UnsizedElement_ConstantSize_instance =
        Struct_Array_Field_UnsizedElement_ConstantSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_ConstantSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 84u64);
                assert_eq!(u64::from(array_vec[1usize]), 85u64);
                assert_eq!(u64::from(array_vec[2usize]), 86u64);
            }
            {
                let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 87u64);
                assert_eq!(u64::from(array_vec[1usize]), 88u64);
                assert_eq!(u64::from(array_vec[2usize]), 89u64);
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_VariableSize_0() {
    let base = hex_to_byte_string("0f00035d5e5f00035061620003636465");
    let Struct_Array_Field_UnsizedElement_VariableSize_instance =
        Struct_Array_Field_UnsizedElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 93u64);
                assert_eq!(u64::from(array_vec[1usize]), 94u64);
                assert_eq!(u64::from(array_vec[2usize]), 95u64);
            }
            {
                let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 80u64);
                assert_eq!(u64::from(array_vec[1usize]), 97u64);
                assert_eq!(u64::from(array_vec[2usize]), 98u64);
            }
            {
                let array_vec = array_vec[4usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[5usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 99u64);
                assert_eq!(u64::from(array_vec[1usize]), 100u64);
                assert_eq!(u64::from(array_vec[2usize]), 101u64);
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_VariableSize_1() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_UnsizedElement_VariableSize_instance =
        Struct_Array_Field_UnsizedElement_VariableSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_VariableSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_VariableCount_0() {
    let base = hex_to_byte_string(
        "0f0003696a6b00036c6d6e00036f607100037273740003757677000378797a00037b7c7d00",
    );
    let Struct_Array_Field_UnsizedElement_VariableCount_instance =
        Struct_Array_Field_UnsizedElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 105u64);
                assert_eq!(u64::from(array_vec[1usize]), 106u64);
                assert_eq!(u64::from(array_vec[2usize]), 107u64);
            }
            {
                let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 108u64);
                assert_eq!(u64::from(array_vec[1usize]), 109u64);
                assert_eq!(u64::from(array_vec[2usize]), 110u64);
            }
            {
                let array_vec = array_vec[4usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[5usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 111u64);
                assert_eq!(u64::from(array_vec[1usize]), 96u64);
                assert_eq!(u64::from(array_vec[2usize]), 113u64);
            }
            {
                let array_vec = array_vec[6usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[7usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 114u64);
                assert_eq!(u64::from(array_vec[1usize]), 115u64);
                assert_eq!(u64::from(array_vec[2usize]), 116u64);
            }
            {
                let array_vec = array_vec[8usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[9usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 117u64);
                assert_eq!(u64::from(array_vec[1usize]), 118u64);
                assert_eq!(u64::from(array_vec[2usize]), 119u64);
            }
            {
                let array_vec = array_vec[10usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[11usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 120u64);
                assert_eq!(u64::from(array_vec[1usize]), 121u64);
                assert_eq!(u64::from(array_vec[2usize]), 122u64);
            }
            {
                let array_vec = array_vec[12usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[13usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 123u64);
                assert_eq!(u64::from(array_vec[1usize]), 124u64);
                assert_eq!(u64::from(array_vec[2usize]), 125u64);
            }
            {
                let array_vec = array_vec[14usize].get_array_iter().collect::<Vec<_>>();
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_VariableCount_1() {
    let base = hex_to_byte_string("00");
    let Struct_Array_Field_UnsizedElement_VariableCount_instance =
        Struct_Array_Field_UnsizedElement_VariableCountView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_VariableCount_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_UnknownSize_0() {
    let base = hex_to_byte_string ("00038182830003848586000387888900038a8b8c00038d8e8f0003809192000393949500039697980003999a9b00039c9d9e00039f90a10003a2a3a40003a5a6a70003a8a9aa0003abacad0003aeafa0") ;
    let Struct_Array_Field_UnsizedElement_UnknownSize_instance =
        Struct_Array_Field_UnsizedElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 129u64);
                assert_eq!(u64::from(array_vec[1usize]), 130u64);
                assert_eq!(u64::from(array_vec[2usize]), 131u64);
            }
            {
                let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 132u64);
                assert_eq!(u64::from(array_vec[1usize]), 133u64);
                assert_eq!(u64::from(array_vec[2usize]), 134u64);
            }
            {
                let array_vec = array_vec[4usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[5usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 135u64);
                assert_eq!(u64::from(array_vec[1usize]), 136u64);
                assert_eq!(u64::from(array_vec[2usize]), 137u64);
            }
            {
                let array_vec = array_vec[6usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[7usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 138u64);
                assert_eq!(u64::from(array_vec[1usize]), 139u64);
                assert_eq!(u64::from(array_vec[2usize]), 140u64);
            }
            {
                let array_vec = array_vec[8usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[9usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 141u64);
                assert_eq!(u64::from(array_vec[1usize]), 142u64);
                assert_eq!(u64::from(array_vec[2usize]), 143u64);
            }
            {
                let array_vec = array_vec[10usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[11usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 128u64);
                assert_eq!(u64::from(array_vec[1usize]), 145u64);
                assert_eq!(u64::from(array_vec[2usize]), 146u64);
            }
            {
                let array_vec = array_vec[12usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[13usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 147u64);
                assert_eq!(u64::from(array_vec[1usize]), 148u64);
                assert_eq!(u64::from(array_vec[2usize]), 149u64);
            }
            {
                let array_vec = array_vec[14usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[15usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 150u64);
                assert_eq!(u64::from(array_vec[1usize]), 151u64);
                assert_eq!(u64::from(array_vec[2usize]), 152u64);
            }
            {
                let array_vec = array_vec[16usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[17usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 153u64);
                assert_eq!(u64::from(array_vec[1usize]), 154u64);
                assert_eq!(u64::from(array_vec[2usize]), 155u64);
            }
            {
                let array_vec = array_vec[18usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[19usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 156u64);
                assert_eq!(u64::from(array_vec[1usize]), 157u64);
                assert_eq!(u64::from(array_vec[2usize]), 158u64);
            }
            {
                let array_vec = array_vec[20usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[21usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 159u64);
                assert_eq!(u64::from(array_vec[1usize]), 144u64);
                assert_eq!(u64::from(array_vec[2usize]), 161u64);
            }
            {
                let array_vec = array_vec[22usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[23usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 162u64);
                assert_eq!(u64::from(array_vec[1usize]), 163u64);
                assert_eq!(u64::from(array_vec[2usize]), 164u64);
            }
            {
                let array_vec = array_vec[24usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[25usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 165u64);
                assert_eq!(u64::from(array_vec[1usize]), 166u64);
                assert_eq!(u64::from(array_vec[2usize]), 167u64);
            }
            {
                let array_vec = array_vec[26usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[27usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 168u64);
                assert_eq!(u64::from(array_vec[1usize]), 169u64);
                assert_eq!(u64::from(array_vec[2usize]), 170u64);
            }
            {
                let array_vec = array_vec[28usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[29usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 171u64);
                assert_eq!(u64::from(array_vec[1usize]), 172u64);
                assert_eq!(u64::from(array_vec[2usize]), 173u64);
            }
            {
                let array_vec = array_vec[30usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[31usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 174u64);
                assert_eq!(u64::from(array_vec[1usize]), 175u64);
                assert_eq!(u64::from(array_vec[2usize]), 160u64);
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_UnknownSize_1() {
    let base = hex_to_byte_string("");
    let Struct_Array_Field_UnsizedElement_UnknownSize_instance =
        Struct_Array_Field_UnsizedElement_UnknownSizeView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_UnknownSize_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_VariableSize_Padded_0() {
    let base = hex_to_byte_string("0000000000000000000000000000000000");
    let Struct_Array_Field_SizedElement_VariableSize_Padded_instance =
        Struct_Array_Field_SizedElement_VariableSize_PaddedView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_VariableSize_Padded_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}

#[test]
fn test_Struct_Array_Field_SizedElement_VariableSize_Padded_1() {
    let base = hex_to_byte_string("0edebbf0bb12cc34cc56cc78cc9acc0000");
    let Struct_Array_Field_SizedElement_VariableSize_Padded_instance =
        Struct_Array_Field_SizedElement_VariableSize_PaddedView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_SizedElement_VariableSize_Padded_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            assert_eq!(u64::from(array_vec[0usize]), 48094u64);
            assert_eq!(u64::from(array_vec[1usize]), 48112u64);
            assert_eq!(u64::from(array_vec[2usize]), 52242u64);
            assert_eq!(u64::from(array_vec[3usize]), 52276u64);
            assert_eq!(u64::from(array_vec[4usize]), 52310u64);
            assert_eq!(u64::from(array_vec[5usize]), 52344u64);
            assert_eq!(u64::from(array_vec[6usize]), 52378u64);
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_VariableCount_Padded_0() {
    let base = hex_to_byte_string("070003cbcccd0003cecfc00003d1d2d300");
    let Struct_Array_Field_UnsizedElement_VariableCount_Padded_instance =
        Struct_Array_Field_UnsizedElement_VariableCount_PaddedView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_VariableCount_Padded_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
            {
                let array_vec = array_vec[0usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[1usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 203u64);
                assert_eq!(u64::from(array_vec[1usize]), 204u64);
                assert_eq!(u64::from(array_vec[2usize]), 205u64);
            }
            {
                let array_vec = array_vec[2usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[3usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 206u64);
                assert_eq!(u64::from(array_vec[1usize]), 207u64);
                assert_eq!(u64::from(array_vec[2usize]), 192u64);
            }
            {
                let array_vec = array_vec[4usize].get_array_iter().collect::<Vec<_>>();
            }
            {
                let array_vec = array_vec[5usize].get_array_iter().collect::<Vec<_>>();
                assert_eq!(u64::from(array_vec[0usize]), 209u64);
                assert_eq!(u64::from(array_vec[1usize]), 210u64);
                assert_eq!(u64::from(array_vec[2usize]), 211u64);
            }
            {
                let array_vec = array_vec[6usize].get_array_iter().collect::<Vec<_>>();
            }
        }
    }
}

#[test]
fn test_Struct_Array_Field_UnsizedElement_VariableCount_Padded_1() {
    let base = hex_to_byte_string("0000000000000000000000000000000000");
    let Struct_Array_Field_UnsizedElement_VariableCount_Padded_instance =
        Struct_Array_Field_UnsizedElement_VariableCount_PaddedView::try_parse(
            SizedBitSlice::from(&base[..]).into(),
        )
        .unwrap();
    {
        {
            let array_vec = Struct_Array_Field_UnsizedElement_VariableCount_Padded_instance
                .get_s()
                .get_array_iter()
                .collect::<Vec<_>>();
        }
    }
}
