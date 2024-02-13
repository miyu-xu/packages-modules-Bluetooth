from mmi2grpc import IUT
PTS_ADDRESS = "00:1B:DC:F4:B1:89"

pts_address = address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
profile = "HFP"

#==============================================================================
# test_name = "HFP/AG/IIC/BV-03-I" #PASSED
# interaction = [
#     "test_started",
#     "TSC_iut_enable_slc",
#     "TSC_disable_ag_cellular_network_expect_no_notification",
#     "TSC_impair_ag_signal_expect_no_notification",
#     "TSC_iut_disable_slc"
# ]
# description = [
#     (""),
#     ("Click Ok, then initiate a service level connection from the\nImplementation Under Test (IUT) to the PTS."),
#     ("Disable the control channel, such that the AG is de-registered. Then,\nclick OK."),
#     ("Impair the signal to the AG so that a reduction in signal strength can\nbe observed. Then, click OK."),
#     ("Click Ok, then disable the service level connection using the\nImplementation Under Test (IUT)."),
# ]

#==============================================================================
# test_name = "HFP/AG/ACR/BV-01-C" #
# interaction = [
#     "test_started",
#     "TSC_iut_enable_slc",
#     "TSC_ag_iut_enable_call",
#     "TSC_verify_audio",
#     "TSC_ag_iut_disable_call_external",
#     "TSC_iut_disable_slc",
#
# ]
# description = [
#     (""),
#     ("Click Ok, then initiate a service level connection from the\nImplementation Under Test (IUT) to the PTS."),
#     ("Click Ok, then place a call from an external line to the Implementation\nUnder Test (IUT). Do not answer the call unless prompted to do so."),
#     ("Verify the presence of an audio connection, then click Ok."),
#     ("Click Ok, then end the call using the external terminal."),
#     ("Click Ok, then disable the service level connection using the\nImplementation Under Test (IUT)."),
# ]

#==============================================================================

# test_name = "HFP/AG/ACR/BV-02-C" # PASSED
# interaction = [
#     "test_started",
#     "TSC_iut_enable_slc",
#     "TSC_ag_iut_enable_call",
#     "TSC_verify_audio",
#     "TSC_ag_iut_disable_call_external",
#     "TSC_iut_disable_slc",
#
# ]
# description = [
#     (""),
#     ("Click Ok, then initiate a service level connection from the\nImplementation Under Test (IUT) to the PTS."),
#     ("Click Ok, then place a call from an external line to the Implementation\nUnder Test (IUT). Do not answer the call unless prompted to do so."),
#     ("Verify the presence of an audio connection, then click Ok."),
#     ("Click Ok, then end the call using the external terminal."),
#     ("Click Ok, then disable the service level connection using the\nImplementation Under Test (IUT)."),
# ]


#==============================================================================

# test_name = "HFP/AG/ACS/BI-14-C" #PASSED
# interaction = [
#     "test_started",
#     "TSC_iut_enable_slc",
#     "TSC_ag_iut_enable_call",
#     "TSC_verify_audio",
#     "TSC_iut_disable_audio",
#     "TSC_verify_no_audio",
#     "TSC_verify_no_audio",
#     "TSC_ag_iut_disable_call_external",
#     "TSC_iut_disable_slc",
#
# ]
# description = [
#     (""),
#     ("Click Ok, then initiate a service level connection from the\nImplementation Under Test (IUT) to the PTS."),
#     ("Click Ok, then place a call from an external line to the Implementation\nUnder Test (IUT). Do not answer the call unless prompted to do so."),
#     ("Verify the presence of an audio connection, then click Ok."),
#     ("Click Ok, then close the audio connection (SCO) between the\nImplementation Under Test (IUT) and the PTS.  Do not close the serivice\nlevel connection (SLC) or power-off the IUT."),
#     ("Verify the absence of an audio connection (SCO), then click Ok."),
#     ("Verify the absence of an audio connection (SCO), then click Ok."),
#     ("Click Ok, then end the call using the external terminal."),
#     ("Click Ok, then disable the service level connection using the\nImplementation Under Test (IUT)."),
# ]
#==============================================================================

# test_name = "HFP/AG/ACS/BV-04-C" #PASSED
# interaction = [
#     "test_started",
#     "TSC_iut_enable_slc",
#     "TSC_ag_iut_enable_call",
#     "TSC_verify_audio",
#     "TSC_iut_disable_audio",
#     "TSC_verify_audio",
#     "TSC_ag_iut_disable_call_external",
#     "TSC_iut_disable_slc",
#
# ]
# description = [
#     (""),
#     ("Click Ok, then initiate a service level connection from the\nImplementation Under Test (IUT) to the PTS."),
#     ("Click Ok, then place a call from an external line to the Implementation\nUnder Test (IUT). Do not answer the call unless prompted to do so."),
#     ("Verify the presence of an audio connection, then click Ok."),
#     ("Click Ok, then close the audio connection (SCO) between the\nImplementation Under Test (IUT) and the PTS.  Do not close the serivice\nlevel connection (SLC) or power-off the IUT."),
#     ("Verify the presence of an audio connection, then click Ok."),
#
#     ("Click Ok, then end the call using the external terminal."),
#     ("Click Ok, then disable the service level connection using the\nImplementation Under Test (IUT)."),
# ]

#==============================================================================

# test_name = "HFP/AG/ACS/BV-08-C" #PASSED
# interaction = [
#     "test_started",
#     "TSC_iut_enable_slc",
#     "TSC_ag_iut_enable_call",
#     "TSC_verify_audio",
#     "TSC_iut_disable_audio",
#     "TSC_verify_audio",
#     "TSC_ag_iut_disable_call_external",
#     "TSC_iut_disable_slc",
#
# ]
# description = [
#     (""),
#     ("Click Ok, then initiate a service level connection from the\nImplementation Under Test (IUT) to the PTS."),
#     ("Click Ok, then place a call from an external line to the Implementation\nUnder Test (IUT). Do not answer the call unless prompted to do so."),
#     ("Verify the presence of an audio connection, then click Ok."),
#     ("Click Ok, then close the audio connection (SCO) between the\nImplementation Under Test (IUT) and the PTS.  Do not close the serivice\nlevel connection (SLC) or power-off the IUT."),
#     ("Verify the presence of an audio connection, then click Ok."),
#     ("Click Ok, then end the call using the external terminal."),
#     ("Click Ok, then disable the service level connection using the\nImplementation Under Test (IUT)."),
# ]

#==============================================================================

test_name = "HFP/AG/ACS/BV-11-C" #PASSED
interaction = [
    "test_started",
    "TSC_iut_enable_slc",
    "TSC_ag_iut_enable_call",
    "TSC_verify_audio",
    "TSC_iut_disable_audio",
    "TSC_verify_audio",
    "TSC_ag_iut_disable_call_external",
    "TSC_iut_disable_slc",

]
description = [
    (""),
    ("Click Ok, then initiate a service level connection from the\nImplementation Under Test (IUT) to the PTS."),
    ("Click Ok, then place a call from an external line to the Implementation\nUnder Test (IUT). Do not answer the call unless prompted to do so."),
    ("Verify the presence of an audio connection, then click Ok."),
    ("Click Ok, then close the audio connection (SCO) between the\nImplementation Under Test (IUT) and the PTS.  Do not close the serivice\nlevel connection (SLC) or power-off the IUT."),
    ("Verify the presence of an audio connection, then click Ok."),
    ("Click Ok, then end the call using the external terminal."),
    ("Click Ok, then disable the service level connection using the\nImplementation Under Test (IUT)."),
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
