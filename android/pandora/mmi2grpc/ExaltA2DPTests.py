from mmi2grpc import IUT
PTS_ADDRESS = "00:1B:DC:F4:B1:61"

pts_address = address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))

interaction = [
    "TSC_AVDTP_mmi_iut_initiate_connect",
    "TSC_AVDTP_mmi_iut_accept_connect",


]
description = [
    ("Create an AVDTP signaling channel.\n\nAction: Create an audio or video\nconnection with PTS."),

    ("If necessary, take action to accept the AVDTP Signaling Channel\nConnection initiated by the tester.\n\nDescription: Make sure the IUT\n(Implementation Under Test) is in a state to accept incoming Bluetooth\nconnections.  Some devices may need to be on a specific screen, like a\nBluetooth settings screen, in order to pair with PTS.  If the IUT is\nstill having problems pairing with PTS, try running a test case where\nthe IUT connects to PTS to establish pairing."),
]

#==============================================================================
profile = ['AVRCP', 'AVRCP']
test_name = 'A2DP/SRC/SET/BV-01-I'

dut = IUT(test_name, [])
for i in range(0, 50):
    user_input = input(
        f'Enter action required for: {interaction[i] if i < len(interaction) else "write command"} .')
    action = interaction[i] if i < len(interaction) else ""
    if user_input == 's':
        continue
    if user_input == 'exit':
        break
    if user_input == "enter":
        mmicommand = input(f'Enter mmi required.')
        action = mmicommand

    out = dut.interact(pts_address, profile[i], test_name, action,
                       description[i],
                       "")
    print(f"Output:{out}")
    print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
