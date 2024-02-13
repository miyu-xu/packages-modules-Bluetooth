from mmi2grpc import IUT
PTS_ADDRESS = "00:1B:DC:F4:B1:61"

pts_address = address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
test_name = 'A2DP/SRC/SET/BV-03-I'

interaction = [
    "TSC_AVDTP_mmi_iut_accept_connect",
    "TSC_AVDTP_mmi_iut_initiate_set_configuration",
    "TSC_AVDTP_mmi_iut_initiate_start"
]

description = [
    ("If necessary, take action to accept the AVDTP Signaling Channel\nConnection initiated by the tester.\n\nDescription: Make sure the IUT\n(Implementation Under Test) is in a state to accept incoming Bluetooth\nconnections.  Some devices may need to be on a specific screen, like a\nBluetooth settings screen, in order to pair with PTS.  If the IUT is\nstill having problems pairing with PTS, try running a test case where\nthe IUT connects to PTS to establish pairing."),
    ("Send a set configuration command to PTS.\n\nAction: If the IUT\n(Implementation Under Test) is already connected to PTS, attempting to\nsend or receive streaming media should trigger this action.  If the IUT\nis not connected to PTS, attempting to connect may trigger this action."),
    ("Send a start command to PTS.\n\nAction: If the IUT (Implementation Under\nTest) is already connected to PTS, attempting to send or receive\nstreaming media should trigger this action.  If the IUT is not connected\nto PTS, attempting to connect may trigger this action.")
]

#==============================================================================
# test_name = 'A2DP/SRC/SET/BV-04-I'
#
# interaction = [
#     "TSC_AVDTP_mmi_iut_accept_connect",
#     "TSC_AVDTP_mmi_iut_initiate_set_configuration",
#     "TSC_AVDTP_mmi_iut_begin_streaming"
# ]
#
# description = [
#     ("If necessary, take action to accept the AVDTP Signaling Channel\nConnection initiated by the tester.\n\nDescription: Make sure the IUT\n(Implementation Under Test) is in a state to accept incoming Bluetooth\nconnections.  Some devices may need to be on a specific screen, like a\nBluetooth settings screen, in order to pair with PTS.  If the IUT is\nstill having problems pairing with PTS, try running a test case where\nthe IUT connects to PTS to establish pairing."),
#     ("Send a set configuration command to PTS.\n\nAction: If the IUT\n(Implementation Under Test) is already connected to PTS, attempting to\nsend or receive streaming media should trigger this action.  If the IUT\nis not connected to PTS, attempting to connect may trigger this action."),
#     ("Begin streaming media ...\n\nNote: If the IUT has suspended the stream\nplease restart the stream to begin streaming media.")
# ]
dut = IUT(test_name, [])
for i in range(0, 50):
    user_input = input(
        f'Enter action required for: {interaction[i] if i < len(interaction) else "write command"} .')
    action = interaction[i] if i < len(interaction) else ""
    test_description = description[i]

    if user_input == 's':
        continue
    elif user_input == 'exit':
        break
    elif user_input == 'm':
        des_user_input = input(f'Enter desc: .')
        test_description = des_user_input

    else:
        pass
    out = dut.interact(pts_address, "A2DP", test_name, action,
                       test_description, "")
    print(f"Output:{out}")
    print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
