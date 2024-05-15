use openssl::error::ErrorStack;
use openssl::pkey::PKey;
use openssl::sign::Signer;
use openssl::symm::Cipher;

use super::{Database, ServiceType};
use crate::gatt;

fn aes_cmac(
    body: impl FnOnce(&mut Signer) -> Result<(), ErrorStack>,
) -> Result<[u8; 16], ErrorStack> {
    let key: [u8; 16] = [0; 16];
    let key = PKey::cmac(&Cipher::aes_128_cbc(), &key)?;

    let mut signer = Signer::new_without_digest(&key)?;
    body(&mut signer)?;

    let mut result = [0; 16];
    signer.sign(&mut result)?;
    Ok(result)
}

fn hash(database: Database) -> Result<[u8; 16], ErrorStack> {
    aes_cmac(|signer| {
        for (service_type, service) in database.iter_services() {
            signer.update(&service.attribute_handle.to_le_bytes())?;
            signer.update(
                &match service_type {
                    ServiceType::Primary => gatt::PRIMARY_SERVICE,
                    ServiceType::Secondary => gatt::SECONDARY_SERVICE,
                }
                .to_le_bytes(),
            )?;
            signer.update(&service.uuid.to_le_bytes())?;

            for include in service.included_services.iter() {
                signer.update(&include.attribute_handle.to_le_bytes())?;
                signer.update(&gatt::INCLUDE.to_le_bytes())?;
                signer.update(&include.service.attribute_handle.to_le_bytes())?;
                signer.update(&include.service.end_group_handle.to_le_bytes())?;
                signer.update(&include.service.uuid.to_le_bytes())?;
            }

            for characteristic in service.characteristics.iter() {
                signer.update(&characteristic.attribute_handle.to_le_bytes())?;
                signer.update(&gatt::CHARACTERISTIC.to_le_bytes())?;
                signer.update(&characteristic.properties.to_le_bytes())?;
                signer.update(&characteristic.value_handle.to_le_bytes())?;
                signer.update(&characteristic.uuid.to_le_bytes())?;

                for descriptor in characteristic.descriptors.iter() {
                    match descriptor.uuid.try_into() {
                        Ok(
                            attribute_type @ (gatt::CHARACTERISTIC_EXTENDED_PROPERTIES
                            | gatt::CHARACTERISTIC_USER_DESCRIPTION
                            | gatt::CLIENT_CHARACTERISTIC_CONFIGURATION
                            | gatt::SERVER_CHARACTERISTIC_CONFIGURATION
                            | gatt::CHARACTERISTIC_PRESENTATION_FORMAT
                            | gatt::CHARACTERISTIC_AGGREGATE_FORMAT),
                        ) => {
                            signer.update(&descriptor.attribute_handle.to_le_bytes())?;
                            signer.update(&attribute_type.to_le_bytes())?;
                        }
                        _ => {}
                    }

                    if descriptor.uuid.try_into() == Ok(gatt::CHARACTERISTIC_EXTENDED_PROPERTIES) {
                        // TODO: write real value
                        signer.update(&[0x00, 0x00])?;
                    }
                }
            }
        }
        Ok(())
    })
}

mod tests {
    use crate::att;
    use crate::database::{self, Database};
    use crate::gatt;
    use crate::uuid::Uuid;

    #[test]
    fn test_aes_cmac() {
        let cmac = super::aes_cmac(|signer| {
            signer.update(&[0x01, 0x00, 0x00, 0x28, 0x00, 0x18])?;
            signer.update(&[0x02, 0x00, 0x03, 0x28, 0x0A, 0x03, 0x00, 0x00, 0x2A])?;
            signer.update(&[0x04, 0x00, 0x03, 0x28, 0x02, 0x05, 0x00, 0x01, 0x2A])?;
            signer.update(&[0x06, 0x00, 0x00, 0x28, 0x01, 0x18])?;
            signer.update(&[0x07, 0x00, 0x03, 0x28, 0x20, 0x08, 0x00, 0x05, 0x2A])?;
            signer.update(&[0x09, 0x00, 0x02, 0x29])?;
            signer.update(&[0x0A, 0x00, 0x03, 0x28, 0x0A, 0x0B, 0x00, 0x29, 0x2B])?;
            signer.update(&[0x0C, 0x00, 0x03, 0x28, 0x02, 0x0D, 0x00, 0x2A, 0x2B])?;
            signer.update(&[0x0E, 0x00, 0x00, 0x28, 0x08, 0x18])?;
            signer.update(&[0x0F, 0x00, 0x02, 0x28, 0x14, 0x00, 0x16, 0x00, 0x0F, 0x18])?;
            signer.update(&[0x10, 0x00, 0x03, 0x28, 0xA2, 0x11, 0x00, 0x18, 0x2A])?;
            signer.update(&[0x12, 0x00, 0x02, 0x29])?;
            signer.update(&[0x13, 0x00, 0x00, 0x29, 0x00, 0x00])?;
            signer.update(&[0x14, 0x00, 0x01, 0x28, 0x0F, 0x18])?;
            signer.update(&[0x15, 0x00, 0x03, 0x28, 0x02, 0x16, 0x00, 0x19, 0x2A])?;
            Ok(())
        })
        .unwrap();

        assert_eq!(
            cmac,
            [
                0xF1, 0xCA, 0x2D, 0x48, 0xEC, 0xF5, 0x8B, 0xAC, 0x8A, 0x88, 0x30, 0xBB, 0xB9, 0xFB,
                0xA9, 0x90
            ]
        );
    }

    #[test]
    fn test_hash_service() {
        let cmac = super::hash(Database {
            primary_services: [database::Service {
                inner: gatt::Service {
                    uuid: Uuid::uuid16(0x1800),
                    attribute_handle: att::AttributeHandle::new(0x0001).unwrap(),
                    end_group_handle: att::AttributeHandle::new(0x0005).unwrap(),
                },
                characteristics: [].into(),
                included_services: [].into(),
            }]
            .into(),
            secondary_services: [].into(),
            know_all_services: vec![],
        })
        .unwrap();

        let expected_cmac =
            super::aes_cmac(|signer| signer.update(&[0x01, 0x00, 0x00, 0x28, 0x00, 0x18])).unwrap();

        assert_eq!(cmac, expected_cmac);
    }

    #[test]
    fn test_hash_service_and_included_service() {
        let cmac = super::hash(Database {
            primary_services: [database::Service {
                inner: gatt::Service {
                    uuid: Uuid::uuid16(0x1808),
                    attribute_handle: att::AttributeHandle::new(0x000E).unwrap(),
                    end_group_handle: att::AttributeHandle::new(0x0013).unwrap(),
                },
                included_services: [gatt::Include {
                    attribute_handle: att::AttributeHandle::new(0x000F).unwrap(),
                    service: gatt::Service {
                        uuid: Uuid::uuid16(0x180F),
                        attribute_handle: att::AttributeHandle::new(0x0014).unwrap(),
                        end_group_handle: att::AttributeHandle::new(0x0016).unwrap(),
                    },
                }]
                .into(),
                characteristics: [].into(),
            }]
            .into(),
            secondary_services: [].into(),
            know_all_services: vec![],
        })
        .unwrap();

        let expected_cmac = super::aes_cmac(|signer| {
            signer.update(&[0x0E, 0x00, 0x00, 0x28, 0x08, 0x18])?;
            signer.update(&[0x0F, 0x00, 0x02, 0x28, 0x14, 0x00, 0x16, 0x00, 0x0F, 0x18])?;
            Ok(())
        })
        .unwrap();

        assert_eq!(cmac, expected_cmac);
    }

    #[test]
    fn test_hash_service_and_characteristic() {
        let cmac = super::hash(Database {
            primary_services: [database::Service {
                inner: gatt::Service {
                    uuid: Uuid::uuid16(0x1800),
                    attribute_handle: att::AttributeHandle::new(0x0001).unwrap(),
                    end_group_handle: att::AttributeHandle::new(0x0005).unwrap(),
                },
                included_services: [].into(),
                characteristics: [database::Characteristic {
                    inner: gatt::Characteristic {
                        uuid: Uuid::uuid16(0x2A00),
                        attribute_handle: att::AttributeHandle::new(0x0002).unwrap(),
                        properties: /* Read + Write */ 0x02 | 0x08,
                        value_handle: att::AttributeHandle::new(0x0003).unwrap(),
                        end_handle: att::AttributeHandle::new(0x0003).unwrap(),
                    },
                    descriptors: [].into(),
                }]
                .into(),
            }]
            .into(),
            secondary_services: [].into(),
            know_all_services: vec![],
        })
        .unwrap();

        let expected_cmac = super::aes_cmac(|signer| {
            signer.update(&[0x01, 0x00, 0x00, 0x28, 0x00, 0x18])?;
            signer.update(&[0x02, 0x00, 0x03, 0x28, 0x0A, 0x03, 0x00, 0x00, 0x2A])?;
            Ok(())
        })
        .unwrap();

        assert_eq!(cmac, expected_cmac);
    }

    #[test]
    fn test_hash_service_characteristic_and_descriptor() {
        let cmac = super::hash(Database {
            primary_services: [database::Service {
                inner: gatt::Service {
                    uuid: Uuid::uuid16(0x1801),
                    attribute_handle: att::AttributeHandle::new(0x0006).unwrap(),
                    end_group_handle: att::AttributeHandle::new(0x000D).unwrap(),
                },
                included_services: [].into(),
                characteristics: [database::Characteristic {
                    inner: gatt::Characteristic {
                                uuid: Uuid::uuid16(0x2A05),
                                attribute_handle: att::AttributeHandle::new(0x0007).unwrap(),
                                properties: /* Indicate */ 0x20,
                                value_handle: att::AttributeHandle::new(0x0008).unwrap(),
                                end_handle: att::AttributeHandle::new(0x0009).unwrap(),
                            },
                    descriptors: [database::Descriptor {
                        inner: gatt::Descriptor {
                            uuid: Uuid::uuid16(0x2902),
                            attribute_handle: att::AttributeHandle::new(0x0009).unwrap(),
                        },
                    }]
                    .into(),
                }]
                .into(),
            }]
            .into(),
            secondary_services: [].into(),
            know_all_services: vec![],
        })
        .unwrap();

        let expected_cmac = super::aes_cmac(|signer| {
            signer.update(&[0x06, 0x00, 0x00, 0x28, 0x01, 0x18])?;
            signer.update(&[0x07, 0x00, 0x03, 0x28, 0x20, 0x08, 0x00, 0x05, 0x2A])?;
            signer.update(&[0x09, 0x00, 0x02, 0x29])?;
            Ok(())
        })
        .unwrap();

        assert_eq!(cmac, expected_cmac);
    }

    #[test]
    fn test_hash() {
        let cmac = super::hash(Database {
            primary_services: [
                database::Service {
                    inner: gatt::Service {
                        uuid: Uuid::uuid16(0x1800),
                        attribute_handle: att::AttributeHandle::new(0x0001).unwrap(),
                        end_group_handle: att::AttributeHandle::new(0x0005).unwrap(),
                    },
                    included_services: [].into(),
                    characteristics: [
                        database::Characteristic {
                            inner: gatt::Characteristic {
                                uuid: Uuid::uuid16(0x2A00),
                                attribute_handle: att::AttributeHandle::new(0x0002).unwrap(),
                                properties: /* Read + Write */ 0x02 | 0x08,
                                value_handle: att::AttributeHandle::new(0x0003).unwrap(),
                                end_handle: att::AttributeHandle::new(0x0003).unwrap(),
                            },
                            descriptors: [].into(),
                        },
                        database::Characteristic {
                            inner: gatt::Characteristic {
                                uuid: Uuid::uuid16(0x2A01),
                                attribute_handle: att::AttributeHandle::new(0x0004).unwrap(),
                                properties: /* Read */ 0x02,
                                value_handle: att::AttributeHandle::new(0x0005).unwrap(),
                                end_handle: att::AttributeHandle::new(0x0005).unwrap(),
                            },
                            descriptors: [].into(),
                        },
                    ]
                    .into(),
                },
                database::Service {
                    inner: gatt::Service {
                        uuid: Uuid::uuid16(0x1801),
                        attribute_handle: att::AttributeHandle::new(0x0006).unwrap(),
                        end_group_handle: att::AttributeHandle::new(0x000D).unwrap(),
                    },
                    included_services: [].into(),
                    characteristics: [
                        database::Characteristic {
                            inner: gatt::Characteristic {
                                uuid: Uuid::uuid16(0x2A05),
                                attribute_handle: att::AttributeHandle::new(0x0007).unwrap(),
                                properties: /* Indicate */ 0x20,
                                value_handle: att::AttributeHandle::new(0x0008).unwrap(),
                                end_handle: att::AttributeHandle::new(0x0009).unwrap(),
                            },
                            descriptors: [database::Descriptor {
                                inner: gatt::Descriptor {
                                    uuid: Uuid::uuid16(0x2902),
                                    attribute_handle: att::AttributeHandle::new(0x0009).unwrap(),
                                },
                            }]
                            .into(),
                        },
                        database::Characteristic {
                            inner: gatt::Characteristic {
                                uuid: Uuid::uuid16(0x2B29),
                                attribute_handle: att::AttributeHandle::new(0x000A).unwrap(),
                                properties: /* Read + Write */ 0x02 | 0x08,
                                value_handle: att::AttributeHandle::new(0x000B).unwrap(),
                                end_handle: att::AttributeHandle::new(0x000B).unwrap(),
                            },
                            descriptors: [].into(),
                        },
                        database::Characteristic {
                            inner: gatt::Characteristic {
                                uuid: Uuid::uuid16(0x2B2A),
                                attribute_handle: att::AttributeHandle::new(0x000C).unwrap(),
                                properties: /* Read */ 0x02,
                                value_handle: att::AttributeHandle::new(0x000D).unwrap(),
                                end_handle: att::AttributeHandle::new(0x000D).unwrap(),
                            },
                            descriptors: [].into(),
                        },
                    ]
                    .into(),
                },
                database::Service {
                    inner: gatt::Service {
                        uuid: Uuid::uuid16(0x1808),
                        attribute_handle: att::AttributeHandle::new(0x000E).unwrap(),
                        end_group_handle: att::AttributeHandle::new(0x0013).unwrap(),
                    },
                    included_services: [gatt::Include {
                        attribute_handle: att::AttributeHandle::new(0x000F).unwrap(),
                        service: gatt::Service {
                            uuid: Uuid::uuid16(0x180F),
                            attribute_handle: att::AttributeHandle::new(0x0014).unwrap(),
                            end_group_handle: att::AttributeHandle::new(0x0016).unwrap(),
                        },
                    }]
                    .into(),
                    characteristics: [
                        database::Characteristic {
                            inner: gatt::Characteristic {
                                uuid: Uuid::uuid16(0x2A18),
                                attribute_handle: att::AttributeHandle::new(0x0010).unwrap(),
                                properties: /* Read + Indicate + Extended Properties */ 0x02 | 0x20 | 0x80,
                                value_handle: att::AttributeHandle::new(0x0011).unwrap(),
                                end_handle: att::AttributeHandle::new(0x0013).unwrap(),
                            },
                            descriptors: [
                              database::Descriptor {
                                inner: gatt::Descriptor {
                                    uuid: Uuid::uuid16(0x2902),
                                    attribute_handle: att::AttributeHandle::new(0x0012).unwrap(),
                                },
                              },
                              database::Descriptor {
                                inner: gatt::Descriptor {
                                    uuid: Uuid::uuid16(0x2900),
                                    attribute_handle: att::AttributeHandle::new(0x0013).unwrap(),
                                },
                              },
                            ].into(),
                        },
                    ].into(),
                },
            ]
            .into(),
            secondary_services: [
                database::Service {
                    inner: gatt::Service {
                        uuid: Uuid::uuid16(0x180F),
                        attribute_handle: att::AttributeHandle::new(0x0014).unwrap(),
                        end_group_handle: att::AttributeHandle::new(0x0016).unwrap(),
                    },
                    included_services: [].into(),
                    characteristics: [
                        database::Characteristic {
                            inner: gatt::Characteristic {
                                uuid: Uuid::uuid16(0x2A19),
                                attribute_handle: att::AttributeHandle::new(0x0015).unwrap(),
                                properties: /* Read */ 0x02,
                                value_handle: att::AttributeHandle::new(0x0016).unwrap(),
                                end_handle: att::AttributeHandle::new(0x0016).unwrap(),
                            },
                            descriptors: [].into(),
                        },
                    ]
                    .into(),
                },
            ].into(),
            know_all_services: vec![],
        })
        .unwrap();

        assert_eq!(
            cmac,
            [
                0xF1, 0xCA, 0x2D, 0x48, 0xEC, 0xF5, 0x8B, 0xAC, 0x8A, 0x88, 0x30, 0xBB, 0xB9, 0xFB,
                0xA9, 0x90
            ]
        );
    }
}
