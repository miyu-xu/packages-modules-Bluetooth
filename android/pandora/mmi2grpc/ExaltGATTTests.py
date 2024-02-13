from mmi2grpc import IUT
PTS_ADDRESS = "00:1B:DC:F4:B1:61"

pts_address = address = bytes.fromhex(PTS_ADDRESS.replace(':', ''))
profile = "GATT"

# test_name = "GATT/CL/GAC/BV-01-C"
# interaction = ["MMI_IUT_INITIATE_CONNECTION",
#                "MMI_IUT_MTU_EXCHANGE",
#                "MMI_IUT_INITIATE_DISCONNECTION"
#                ]
# first = ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS.")
# second = ("Please send exchange MTU command to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can send Exchange MTU command to the\ntester.")
# third = ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS.")
# description = [
#     first,
#     second,
#     third
#                ]

#========================================================

# test_name = "GATT/SR/GAR/BI-04-C"# This test passed
#
# interaction = ["MMI_IUT_NO_SECURITY",
#                "MMI_MAKE_IUT_CONNECTABLE",
#                ]
# description = [
#     ("Please make sure IUT does not initiate security procedure.\n\nDescription:\nPTS will delete bond information. Test case requires that no\nauthentication or authorization procedure has been performed between the\nIUT and the test system."),
#     ("Please prepare IUT into a connectable mode.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can accept GATT connect request from\nPTS."),
#                ]

#========================================================

# test_name = "GATT/CL/GAN/BV-01-C" #FAILED: NEED BR/EDR
#
# interaction = [
#                "MMI_IUT_INITIATE_CONNECTION",
#                "MMI_IUT_SEND_NOTIFICATION",
#                "MMI_IUT_RECEIVE_NOTIFICATION"
#                ]
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please write to client characteristic configuration handle = 'XXXX'O to\nenable notification to the PTS. Discover all characteristics if needed.\nDescription: Verify that the Implementation Under Test (IUT) can receive\nnotification sent from PTS."),
#     ("Please confirm IUT received notification from PTS. Click YES if\nreceived, otherwise NO.\n\nDescription: Verify that the Implementation\nUnder Test (IUT) can receive notification send from PTS.")
#                ]
#========================================================
#
# test_name = "GATT/SR/GAC/BV-01-C"# pass after send response in gatt_server.py
#
# interaction = ["MMI_MAKE_IUT_CONNECTABLE",
#                ]
# description = [
#     ("Please prepare IUT into a connectable mode.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can accept GATT connect request from\nPTS."),
#                ]
#
# =======================================================
# test_name = "GATT/CL/GAD/BV-03-C"
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_FIND_INCLUDED_SERVICES",
#     "MMI_CONFIRM_NO_INCLUDE_SERVICE",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_FIND_INCLUDED_SERVICES",
#     "MMI_CONFIRM_INCLUDE_SERVICE",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_FIND_INCLUDED_SERVICES",
#     "MMI_CONFIRM_INCLUDE_SERVICE",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_FIND_INCLUDED_SERVICES",
#     "MMI_CONFIRM_INCLUDE_SERVICE",
#     "MMI_IUT_INITIATE_DISCONNECTION",
# ]
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please send discover all include services to the PTS to discover all\nInclude Service supported in the PTS. Discover primary service if\nneeded.\n\nDescription: Verify that the Implementation Under Test (IUT)\ncan send Discover all include services command."),
#     ("There is no include service in the database file.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can send Discover all include\nservices in database."),
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please send discover all include services to the PTS to discover all\nInclude Service supported in the PTS. Discover primary service if\nneeded.\n\nDescription: Verify that the Implementation Under Test (IUT)\ncan send Discover all include services command."),
#     ("Please confirm IUT received include services:\n\nAttribute Handle = '0002'O, Included Service Attribute handle = '0080'O,\nEnd Group Handle = '0085'O, Service UUID = 'A00B'O\n\nClick Yes if IUT received it, otherwise click No.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can send Discover all include\nservices in database."),
#     (""),
#     (""),
#     (""),
#     (""),
#     (""),
#     (""),
#     (""),
#     (""),
#
#                ]

#============================================================

# test_name = "GATT/CL/GAI/BV-01-C" #FAILED, SABA already report that in proposal
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_SEND_INDICATION",
#     "MMI_IUT_RECEIVE_INDICATION",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#
# ]
#
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please write to client characteristic configuration handle = '0133'O to\nenable indication to the PTS. Discover all characteristics if needed.\nDescription: Verify that the Implementation Under Test (IUT) can receive\nindication sent from PTS."),
#     ("Please confirm IUT received indication from PTS. Click YES if received,\notherwise NO.\n\nDescription: Verify that the Implementation Under Test\n(IUT) can receive indication send from PTS."),
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
#
# ]
# ==================================

# test_name = "GATT/CL/GAN/BV-01-C" # The callback exist before its registered in  RECEIVE_NOTIFICATION
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_SEND_NOTIFICATION",
#     "MMI_IUT_RECEIVE_NOTIFICATION",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#
# ]
#
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please write to client characteristic configuration handle = '0134'O to\nenable notification to the PTS. Discover all characteristics if needed.\nDescription: Verify that the Implementation Under Test (IUT) can receive\nnotification sent from PTS."),
#     ("Please confirm IUT received notification from PTS. Click YES if\nreceived, otherwise NO.\n\nDescription: Verify that the Implementation\nUnder Test (IUT) can receive notification send from PTS."),
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
# ]

# ==================================
# test_name = "GATT/CL/GAR/BI-07-C"
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_SEND_READ_CHARACTERISTIC_UUID",
#     "MMI_IUT_CONFIRM_ATTRIBUTE_NOT_FOUND",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#
# ]
# #
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please send read using characteristic UUID = '1D27'O handle range =\n'0001'O to 'FFFF'O to the PTS.\n\nDescription: Verify that the\nImplementation Under Test (IUT) can send Read characteristic by UUID."),
#     ("Please confirm IUT received attribute not found error. Click Yes if IUT\nreceived it, otherwise click No.\n\nDescription: Verify that the\nImplementation Under Test (IUT) indicate attribute not found error when\nread a characteristic."),
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
# ]

# ==================================
# test_name = "GATT/CL/GAC/BV-01-C"  #Exchange mtu
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_MTU_EXCHANGE",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#
# ]
# #
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please send exchange MTU command to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can send Exchange MTU command to the\ntester."),
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
# ]

# ==================================
# test_name = "GATT/CL/GAW/BI-33-C" # Passed with second method disc fix
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_SEND_WRITE_REQUEST_GREATER",
#     "MMI_IUT_CONFIRM_WRITE_INVALID_LENGTH",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#
# ]
# #
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please send write request with characteristic handle = '0064'O with greater than '2' byte of any octet value to the PTS.\n\nDescription:\nVerify that the Implementation Under Test (IUT) can send write request."),
#     ("Please confirm IUT received Invalid attribute value length error. Click\nYes if IUT received it, otherwise click No.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) indicate Invalid attribute value\nlength error when write a characteristic."),
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
# ]

# ####################################################################
# test_name = "GATT/CL/GAW/BI-03-C" #FAILED, SABA already report that in proposal
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_SEND_WRITE_REQUEST", #Need to fix implementation
#     "MMI_IUT_CONFIRM_WRITE_NOT_PERMITTED",
#     "MMI_IUT_INITIATE_DISCONNECTION",
# ]
#
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please send write request with characteristic handle = 'XXXX'O with <= 'X' byte of any octet value to the PTS.\n\nDescription: Verify that theImplementation Under Test (IUT) can send write request."),
#     (
#         "Please confirm IUT received write is not permitted error. Click Yes if\nIUT received it, otherwise click No.\n\nDescription: Verify that the\nImplementation Under Test (IUT) indicate write is not permitted error\nwhen write a characteristic."),
#
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
# ]

# ####################################################################
# test_name = "GATT/CL/GAW/BV-08-C" #FAILED, SABA already report that in proposal
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_SEND_WRITE_REQUEST", #Need to fix implementation
#     "MMI_IUT_INITIATE_DISCONNECTION",
# ]
#
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please send write request with characteristic handle = 'XXXX'O with <= 'X' byte of any octet value to the PTS.\n\nDescription: Verify that theImplementation Under Test (IUT) can send write request."),
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
# ]

# # # #############################################################
# test_name = "GATT/SR/GAR/BV-04-C"# This test passed after fix server
#
# interaction = [
#                "MMI_MAKE_IUT_CONNECTABLE",
#                "MMI_IUT_CONFIRM_READ_HANDLE_VALUE",
#
#                ]
# description = [
#     ("Please prepare IUT into a connectable mode.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can accept GATT connect request from\nPTS."),
#     ("Please confirm IUT Handle='XX'O characteristic\nvalue='XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'O in random\nelected adopted database. Click Yes if it matches the IUT, otherwise\nclick No.\n\nDescription: Verify that the Implementation Under Test (IUT)\ncan send Read long characteristic to PTS random select adopted database."),
#                ]

# #############################################################
# test_name = "GATT/SR/GAR/BI-05-C"# Failed with pairing
# interaction = [
#                "MMI_MAKE_IUT_CONNECTABLE",
#                "MMI_CONFIRM_PASSKEY",
#
#                ]
# description = [
#     ("Please prepare IUT into a connectable mode.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can accept GATT connect request from\nPTS."),
#     ("The secureId is (?P<passkey>[0-9]+)."),
#                ]
# #############################################################
# test_name = "GATT/SR/GAR/BI-22-C"
# interaction = [
#                "MMI_MAKE_IUT_CONNECTABLE",
#                "MMI_CONFIRM_PASSKEY",
#
#                ]
# description = [
#     ("Please prepare IUT into a connectable mode.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can accept GATT connect request from\nPTS."),
#     ("The secureId is (?P<passkey>[0-9]+)."),
#                ]

# # # #############################################################
# test_name = "GATT/SR/GAR/BV-05-C"#Unstable
#
# interaction = [
#                "MMI_MAKE_IUT_CONNECTABLE",
#                "MMI_IUT_CONFIRM_READ_MULTIPLE_HANDLE_VALUES",
#
#                ]
# description = [
#     ("Please prepare IUT into a connectable mode.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can accept GATT connect request from\nPTS."),
#     ("Please confirm IUT Handle pair = 'XXXX'O 'XXXX'O\nvalue='XXXXXXXXXXXXXXXXXXXXXXXXXXX in random selected\nadopted database. Click Yes if it matches the IUT, otherwise click No.\nDescription: Verify that the Implementation Under Test (IUT) can send\nRead multiple characteristics."),
#                ]


# # #############################################################
# test_name = "GATT/SR/GAW/BI-06-C"#pairing issue
#
# interaction = [
#                "MMI_MAKE_IUT_CONNECTABLE",
#                 "_mmi_2004",
#                 "MMI_CONFIRM_PASSKEY",
#
#                ]
# description = [
#     ("Please prepare IUT into a connectable mode.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can accept GATT connect request from\nPTS."),
#     ("Please confirm that 6 digit number is matched with (?P<passkey>[0-9]+)."),
#     ("The secureId is (?P<passkey>[0-9]+)."),
#                ]


# # # #############################################################
test_name = "GATT/SR/GAW/BV-03-C"# This test passed after fix server

interaction = [
               "MMI_MAKE_IUT_CONNECTABLE",
               "MMI_IUT_CONFIRM_READ_HANDLE_VALUE",

               ]
description = [
    ("Please prepare IUT into a connectable mode.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can accept GATT connect request from\nPTS."),
    ("Please confirm IUT Handle='XX'O characteristic\nvalue='XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX'O in random\nelected adopted database. Click Yes if it matches the IUT, otherwise\nclick No.\n\nDescription: Verify that the Implementation Under Test (IUT)\ncan send Read long characteristic to PTS random select adopted database."),
               ]

# # # #############################################################
# test_name = "GATT/CL/GAW/BV-03-C"# This test passed after fix server
#
# interaction = [
#     "MMI_IUT_INITIATE_CONNECTION",
#     "MMI_IUT_SEND_WRITE_REQUEST_GREATER",
#     "MMI_IUT_CONFIRM_WRITE_INVALID_LENGTH",
#     "MMI_IUT_INITIATE_DISCONNECTION",
#
# ]
# #
# description = [
#     ("Please initiate a GATT connection to the PTS.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) can initiate GATT connect request to\nPTS."),
#     ("Please send write request with characteristic handle = '0064'O with greater than '2' byte of any octet value to the PTS.\n\nDescription:\nVerify that the Implementation Under Test (IUT) can send write request."),
#     ("Please confirm IUT received Invalid attribute value length error. Click\nYes if IUT received it, otherwise click No.\n\nDescription: Verify that\nthe Implementation Under Test (IUT) indicate Invalid attribute value\nlength error when write a characteristic."),
#     ("Please initiate a GATT disconnection to the PTS.\n\nDescription: Verify\nthat the Implementation Under Test (IUT) can initiate GATT disconnect\nrequest to PTS."),
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
    out = dut.interact(pts_address, profile, test_name, action,
                       test_description, "")
    print(f"Output:{out}")
    print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")


