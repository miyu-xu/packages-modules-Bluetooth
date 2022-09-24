pub mod hci {
    use std::convert::TryFrom;

    pub use bt_packets::custom_types::*;
    pub use bt_packets::hci::*;

    use paste::paste;

    pub enum Identifier {
        Address(Address),
        Handle(u16),
    }

    macro_rules! declare_link_commands {
        (
            security: {$($T1:ident),* $(,)?},
            connection_management: {$($T2:ident -> $E:expr,)* $(,)?},
        ) => {
            paste!{

                #[derive(Clone)]
                pub enum LinkHCICommandHolder {
                    $( $T1([<$T1 Packet>]), )*
                    $( $T2([<$T2 Packet>]), )*
                }

                impl LinkHCICommandHolder {

                    pub fn get_identifier(&self) -> Identifier {
                        match self {
                            $( Self::$T1(packet) => Identifier::Address(packet.get_bd_addr()), )*
                            $( Self::$T2(packet) => Identifier::Handle(packet.get_connection_handle()), )*
                        }
                    }

                    pub fn reject(self, send_hci_event: impl Fn(EventPacket)) {
                        match self {
                            $(Self::$T1(packet) => send_hci_event(
                                [<$T1 CompleteBuilder>] {
                                    num_hci_command_packets: 1,
                                    status: ErrorCode::CommandDisallowed, // FIXME
                                    bd_addr: packet.get_bd_addr(),
                                }.build().into()
                            ),)*
                            $(Self::$T2(packet) => {
                                send_hci_event(
                                    [<$T2 StatusBuilder>] {
                                        num_hci_command_packets: 1,
                                        status: ErrorCode::CommandDisallowed, // FIXME
                                    }.build().into()
                                );
                                // DO WE WANT THIS?
                                send_hci_event(
                                    $E(ErrorCode::Success, packet.get_connection_handle()).build().into()
                                );
                            },)*
                        }
                    }

                    pub fn new(packet: CommandPacket) -> Option<Self> {
                        match packet.specialize() {
                            CommandChild::SecurityCommand(command) => match command.specialize() {
                                $(SecurityCommandChild::$T1(packet) => Some(Self::$T1(packet)),)*
                                _ => None,
                            }
                            CommandChild::AclCommand(command) => match command.specialize() {
                                AclCommandChild::ConnectionManagementCommand(command) => {
                                    match command.specialize() {
                                        $(ConnectionManagementCommandChild::$T2(packet) => Some(Self::$T2(packet)),)*
                                        _ => None,
                                    }
                                }
                                _ => None,
                            },
                            _ => None,
                        }
                    }
                }

                $(
                    impl TryFrom<LinkHCICommandHolder> for [<$T1 Packet>] {
                        type Error = ();

                        fn try_from(holder: LinkHCICommandHolder) -> Result<Self, Self::Error> {
                            match holder {
                                LinkHCICommandHolder::$T1(packet) => Ok(packet),
                                _ => Err(()),
                            }
                        }
                    }
                )*

                $(
                    impl TryFrom<LinkHCICommandHolder> for [<$T2 Packet>] {
                        type Error = ();

                        fn try_from(holder: LinkHCICommandHolder) -> Result<Self, Self::Error> {
                            match holder {
                                LinkHCICommandHolder::$T2(packet) => Ok(packet),
                                _ => Err(()),
                            }
                        }
                    }
                )*
            }
        }
    }

    declare_link_commands!(
        security: {
            LinkKeyRequestReply,
            LinkKeyRequestNegativeReply,
            PinCodeRequestReply,
            PinCodeRequestNegativeReply,
            IoCapabilityRequestReply,
            IoCapabilityRequestNegativeReply,
            UserConfirmationRequestReply,
            UserConfirmationRequestNegativeReply,
            UserPasskeyRequestReply,
            UserPasskeyRequestNegativeReply,
            RemoteOobDataRequestReply,
            RemoteOobDataRequestNegativeReply,
            SendKeypressNotification,
        },
        connection_management: {
            AuthenticationRequested -> {
                |status, connection_handle| AuthenticationCompleteBuilder { status, connection_handle }
            },
            SetConnectionEncryption -> {
                |status, connection_handle| EncryptionChangeBuilder {
                    status,
                    connection_handle,
                    encryption_enabled: EncryptionEnabled::Off,
                }
            },
        },
    );
}

pub mod lmp {
    #![allow(clippy::all)]
    #![allow(unused)]
    #![allow(missing_docs)]

    include!(concat!(env!("OUT_DIR"), "/lmp_packets.rs"));
}
