
from mmi2grpc import IUT

PTS_ADDRESS = "00:1B:DC:F4:B1:89"

pts_address = address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
profile = "RFCOMM"

#
# test_name = "RFCOMM/DEVA/RFC/BV-01-C"  # PASS
# interaction = [
#     "TSC_RFCOMM_mmi_iut_initiate_slc",
# ]
#
# description = [
#     ("Take action to initiate an RFCOMM service level connection (l2cap)."),
# ]

##################################

test_name = ("RFCOMM/DEVA-DEVB/RFC/BV-03-C")  # PASS
interaction = [
    "TSC_RFCOMM_mmi_iut_accept_slc",
]

description = [
    ("Take action to accept the RFCOMM service level connection from the\ntester."),
]

# test_name = "L2CAP/LE/CPU/BV-02-C" #fail
# interaction = [
#     "MMI_IUT_ENABLE_LE_CONNECTION",
#     "MMI_IUT_SEND_ACL_DISCONNECTION",
# ]
#
# description = [
#     ("Initiate or create LE ACL connection to the PTS."),
#     ("Initiate an ACL disconnection from the IUT to the PTS.\nDescription :\nThe Implementation Under Test(IUT) should disconnect ACL channel by\nsending a disconnect request to PTS.")
# ]

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

