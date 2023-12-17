from mmi2grpc import IUT
PTS_ADDRESS = "00:1B:DC:F4:B1:89"


pts_address = address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
profile = "L2CAP"

# test_name = "L2CAP/COS/CFD/BV-11-C" #passed
# interaction = [
#     "MMI_TESTER_ENABLE_CONNECTION",
# ]
#
# description = [
#     ("Action: Place the IUT in connectable mode.\n\nDescription: PTS requires that the IUT be in connectable mode.\nThe PTS will attempt to establish an ACL connection."),
# ]
# #############################################
# test_name = "L2CAP/LE/CPU/BI-02-C" #passed
# interaction = [
#     "MMI_TESTER_ENABLE_LE_CONNECTION",
# ]
#
# description = [
# ("Place the IUT into LE connectable mode."),
# ]

########################################

# test_name = "L2CAP/LE/REJ/BI-01-C" #passed
# interaction = [
#     "MMI_TESTER_ENABLE_LE_CONNECTION",
# ]
#
# description = [
# ("Place the IUT into LE connectable mode."),
# ]
######################################

# test_name = "L2CAP/LE/CPU/BI-01-C" #passed
# interaction = [
#     "MMI_IUT_ENABLE_LE_CONNECTION",
# ]
#
# description = [
# ("Initiate or create LE ACL connection to the PTS."),
# ]

#######################################
#
# test_name = "L2CAP/COS/CFD/BV-08-C" # fail
# interaction = [
#     "MMI_IUT_INITIATE_ACL_CONNECTION",
#     "MMI_IUT_DISABLE_CONNECTION",
#     "MMI_IUT_SEND_ACL_DISCONNECTION"
# ]
#
# description = [
# ("Using the Implementation Under Test(IUT), initiate ACL Create Connection\nRequest to the PTS.\n\nDescription : The Implementation Under Test(IUT)\nshould create ACL connection request to PTS."),
#     ("Initiate an L2CAP disconnection from the IUT to the PTS.\n\nDescription :\nThe Implementation Under Test(IUT) should disconnect the active L2CAP\nchannel by sending a disconnect request to PTS."),
#     ("Initiate an ACL disconnection from the IUT to the PTS.\nDescription :\nThe Implementation Under Test(IUT) should disconnect ACL channel by\nsending a disconnect request to PTS.")
# ]

##################################

#
# test_name = "L2CAP/COS/CED/BV-01-C" #fail
# interaction = [
#     "MMI_IUT_INITIATE_ACL_CONNECTION",
#     "MMI_IUT_DISABLE_CONNECTION"
# ]
#
# description = [
#     ("Using the Implementation Under Test(IUT), initiate ACL Create Connection\nRequest to the PTS.\n\nDescription : The Implementation Under Test(IUT)\nshould create ACL connection request to PTS."),
#     ("Initiate an L2CAP disconnection from the IUT to the PTS.\n\nDescription :\nThe Implementation Under Test(IUT) should disconnect the active L2CAP\nchannel by sending a disconnect request to PTS.")
# ]

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

test_name = "L2CAP/COS/ECFC/BV-03-C" #fail
interaction = [
    "MMI_TESTER_ENABLE_LE_CONNECTION",

]

description = [
    ("Place the IUT into LE connectable mode."),
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
    
    out =dut.interact(pts_address, profile, test_name, action,
                 description[i],
                 "")
    print(f"Output:{out}")
    print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")

