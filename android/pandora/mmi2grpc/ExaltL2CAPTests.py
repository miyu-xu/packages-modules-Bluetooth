from mmi2grpc import IUT
PTS_ADDRESS = "00:1B:DC:F4:B1:89"

pts_address = address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
profile = "L2CAP"

test_name = "L2CAP/COS/CED/BV-05-C"
interaction = [
    "MMI_TESTER_ENABLE_CONNECTION"

]
description = [
    ("Action: Place the IUT in connectable mode.\n\nDescription: PTS requires that the IUT be in connectable mode.\nThe PTS will attempt to establish an ACL connection.")
]
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

    out = dut.interact(pts_address, profile, test_name, action,
                       description[i],
                       "")
    print(f"Output:{out}")
    print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
