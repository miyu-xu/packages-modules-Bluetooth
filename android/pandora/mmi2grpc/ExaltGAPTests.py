from mmi2grpc import IUT
PTS_ADDRESS = "00:1B:DC:F4:B1:89"

pts_address = address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
profile = "GAP"



test_name = "GAP/BOND/BON/BV-01-C"
interaction = [
    "TSC_MMI_iut_remove_bonding",
    # "_auto_confirm_requests",
    "TSC_MMI_iut_send_advertising_report_event_connectable_undirected",
    "TSC_MMI_iut_start_bonding_procedure_bondable",
    "",
    "",


]
description = [
    ("Please have Upper Tester remove the bonding information of the PTS.\nPress OK to continue."),
    # (""),
    ("Please send a connectable undirected advertising report."),
    ("Please start the Bonding Procedure in bondable mode."),
    ("The Secure ID is 816650"),
    (""),
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
