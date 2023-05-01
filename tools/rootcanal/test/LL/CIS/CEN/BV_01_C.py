import lib_rootcanal_python3 as rootcanal
import hci_packets as hci
import link_layer_packets as ll
import unittest
from hci_packets import ErrorCode
from py.bluetooth import Address
from py.controller import ControllerTest


class Test(ControllerTest):

    SDU_Interval_C_TO_P = 10000 # 10ms
    SDU_Interval_P_TO_C = 10000 # 10ms
    ISO_Interval = 20000 # 20ms
    Worst_Case_SCA = hci.ClockAccuracy.PPM_500
    Packing = hci.Packing.SEQUENTIAL
    Framing = hci.Enable.DISABLED
    NSE = 4
    Max_SDU_C_TO_P = # Note 1; Max SDU defined by the bandwith
    Max_SDU_P_TO_C = # Note 1; Max SDU defined by the bandwith
    Max_PDU_C_TO_P = 251
    Max_PDU_P_TO_C = 251
    PHY_C_TO_P Yes Yes LE 1M PHY
    PHY_P_TO_C Yes Yes LE 1M PHY
    FT_C_TO_P = 1
    FT_P_TO_C = 1
    BN_C_TO_P = 1
    BN_P_TO_C = 1
    Max_Transport_Latency_C_TO_P = 40000 # 40ms
    Max_Transport_Latency_P_TO_C = 40000 # 40ms
    RTN_C_TO_P = 3
    RTN_P_TO_C = 3

    # LL/DDI/ADV/BV-01-C [Non-Connectable Advertising Events]
    async def test(self):
        # Test parameters.
        cig_id = 0x12
        cis_id = 0x42
        acl_connection_handle = 0xefe
        cis_connection_handle = 0xefe
        peer_address = Address('aa:bb:cc:dd:ee:ff')
        controller = self.controller

        # Prelude: Establish an ACL connection with the IUT.
        controller.send_cmd(
            hci.LeSetAdvertisingParameters(advertising_interval_min=0x200,
                                           advertising_interval_max=0x200,
                                           advertising_type=hci.AdvertisingType.ADV_IND,
                                           own_address_type=hci.OwnAddressType.PUBLIC_DEVICE_ADDRESS,
                                           advertising_channel_map=0x7,
                                           advertising_filter_policy=hci.AdvertisingFilterPolicy.ALL_DEVICES))

        await self.expect_evt(
            hci.LeSetAdvertisingParametersComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        controller.send_cmd(hci.LeSetAdvertisingEnable(advertising_enable=True))

        await self.expect_evt(hci.LeSetAdvertisingEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        controller.send_ll(ll.LeConnect(source_address=peer_address,
                                        destination_address=controller.address,
                                        initiating_address_type=ll.AddressType.PUBLIC,
                                        advertising_address_type=ll.AddressType.PUBLIC,
                                        conn_interval=0x200,
                                        conn_peripheral_latency=0x200,
                                        conn_supervision_timeout=0x200),
                           rssi=-16)

        await self.expect_ll(
            ll.LeConnectComplete(source_address=controller.address,
                                 destination_address=peer_address,
                                 conn_interval=0x200,
                                 conn_peripheral_latency=0x200,
                                 conn_supervision_timeout=0x200))

        await self.expect_evt(
            hci.LeEnhancedConnectionComplete(status=ErrorCode.SUCCESS,
                                             connection_handle=acl_connection_handle,
                                             role=hci.Role.PERIPHERAL,
                                             peer_address_type=hci.AddressType.PUBLIC_DEVICE_ADDRESS,
                                             peer_address=peer_address,
                                             conn_interval=0x200,
                                             conn_latency=0x200,
                                             supervision_timeout=0x200,
                                             central_clock_accuracy=hci.ClockAccuracy.PPM_500))

        # 1. The Upper Tester sends an HCI_LE_Set_CIG_Parameters_Test command to the IUT with
        # CIS_Count set to 1, BN, FT, NSE, PHY_C_TO_P[], PHY_P_TO_C[] and ISO_Interval to be set to
        # the values specified in Table 4.135 and Table 4.136. Any remaining values are assigned the
        # default values as specified in Section 4.10.1.3 Default Values for Set CIG Parameters
        # Commands. The Upper Tester receives a successful HCI_Command_Complete event with a
        # valid Connection_Handle from the IUT and CIS_Count = 1.
        controller.send_cmd(
            hci.LeSetCigParametersTest(
                cig_id=cig_id,
                sdu_interval_c_to_p=SDU_Interval_C_TO_P,
                sdu_interval_p_to_c=SDU_Interval_P_TO_C,
                ft_c_to_p=FT_C_TO_P,
                ft_p_to_c=FT_P_TO_C,
                iso_interval=ISO_Interval,
                worst_case_sca=Worst_Case_SCA,
                packing=Packing,
                framing=Framing,
                cis_config=[
                    hci.LeCisParametersTestConfig(
                        cis_id=cis_id,
                        nse=NSE,
                        max_sdu_c_to_p=Max_SDU_C_TO_P,
                        max_sdu_p_to_c=Max_SDU_P_TO_C,
                        max_pdu_c_to_p=Max_PDU_C_TO_P,
                        max_pdu_p_to_c=Max_PDU_P_TO_C,
                        phy_c_to_p=hci.PhyType.LE_1M,
                        phy_p_to_c=hci.PhyType.LE_1M,
                        bn_c_to_p=BN_C_TO_P,
                        bn_p_to_c=BN_P_TO_C)
                ])

        await self.expect_evt(
            hci.LeSetCigParametersTestComplete(
                status=ErrorCode.SUCCESS,
                num_hci_command_packets=1,
                cig_id=cig_id,
                cis_count=1,
                connection_handle=[cis_connection_handle]))

        # 2. The Upper Tester sends an HCI_LE_Create_CIS command to the IUT with the
        # ACL_Connection_Handle of the established ACL connection and CIS_Count set to 1. The Upper
        # Tester receives a Status of Success from the IUT.
        controller.send_cmd(
            hci.LeCreateCis(
                cis_config=[
                    hci.LeCreateCisConfig(
                        cis_connection_handle=cis_connection_handle,
                        acl_connection_handle=acl_connection_handle)
                ])

        await self.expect_evt(
            hci.LeCreateCisStatus(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        # 3. The Lower Tester receives an LL_CIS_REQ PDU from the IUT with all fields set to valid values.
        # CIS_Offset_Min is a value between 500µs and TSPX_conn_interval, CIS_Offset_Max is a value
        # between CIS_Offset_Min and the CIS_Offset_Max value as calculated in [14] Section 2.4.2.29
        # using TSPX_conn_interval as the value of connInterval, and connEventCount is the reference
        # event anchor point for which the offsets applied.
        await self.expect_llcp(
            source_address=controller.address,
            destination_address=controller.address,
            llcp.CisReq(cig_id=cig_id,
                        cis_id=cis_id,
                        phy_c_to_p=hci.PhyType.LE_1M,
                        phy_p_to_c=hci.PhyType.LE_1M,
                        framed=,
                        max_sdu_c_to_p=Max_SDU_C_TO_P,
                        max_sdu_p_to_c=Max_SDU_P_TO_C,
                        sdu_interval_c_to_p=SDU_Interval_C_TO_P,
                        sdu_interval_p_to_c=SDU_Interval_P_TO_C,
                        max_pdu_c_to_p=Max_PDU_C_TO_P,
                        max_pdu_p_to_c=Max_PDU_P_TO_C,
                        nse=NSE,
                        sub_interval=,
                        bn_p_to_c=BN_C_TO_P,
                        bn_c_to_p=BN_P_TO_C,
                        ft_c_to_p=FT_C_TO_P,
                        ft_p_to_c=FT_P_TO_C,
                        iso_interval=ISO_Interval,
                        cis_offset_min=,
                        cis_offset_max=,
                        conn_event_count=0)

        # 4. The Lower Tester sends an LL_CIS_RSP PDU to the IUT.
        controller.send_llcp(
            source_address=controller.address,
            destination_address=controller.address,
            llcp.CisRsp(cis_offset_min=,
                        cis_offset_max=,
                        conn_event_count=0)

        # 5. The Lower Tester receives an LL_CIS_IND from the IUT where the CIS_Offset is the time (ms)
        # from the start of the ACL connection event in connEvent Count to the first CIS anchor point, the
        # CIS_Sync_Delay is CIG_Sync_Delay minus the offset from the CIG reference point to the CIS
        # anchor point in s, and the connEventCount is the CIS_Offset reference point.
        await self.expect_llcp(
            source_address=controller.address,
            destination_address=peer_address,
            ll.CisInd(aa=0,
                      cis_offset=,
                      cig_sync_delay=,
                      cis_sync_delay=,
                      conn_event_count=0)

        # 6. The IUT sends a CIS Null PDU to the Lower Tester and the Lower Tester responds with a CIS
        # Null PDU. Alternately, the IUT sends an empty Data PDU, which the Lower Tester acknowledges.
        # These exchanges will continue until data is exchanged between the IUT and the Lower Tester in
        # later steps.

        # 7. The Upper Tester receives a successful HCI_LE_CIS_Established event with the NSE, BN, FT,
        # and Max_PDU parameters as set in step 1 from the IUT, after the first CIS packet sent by the LT.
        # The Connection_Handle parameter is set to the value provided in the HCI_LE_Create_CIS
        # command.
        await self.expect_evt(
            hci.LeCisEstablished(
                status=ErrorCode.SUCCESS,
                connection_handle=acl_connection_handle,
                cig_sync_delay=,
                cis_sync_delay=,
                transport_latency_c_to_p=,
                transport_latency_p_to_c=,
                phy_c_to_p=hci.SecondaryPhyType,
                phy_p_to_c=hci.SecondaryPhyType,
                nse=,
                bn_c_to_p=,
                bn_p_to_c=,
                ft_c_to_p=,
                ft_p_to_c=,
                max_pdu_c_to_p=,
                max_pdu_p_to_c=,
                iso_interval=))

        # 8. The Upper Tester orders the IUT to send data packets to the Lower Tester.

        # 9. The Lower Tester receives CIS data PDUs from the IUT in each sub-event of the CIS and
        # acknowledges those PDUs.

        # 10. Repeat step 9 for 50 ÷ BN isochronous events starting with the first event where a CIS data PDU
        # with nonzero payload is received.

        #                       NSE     SDU_Interval_C_TO_P     SDU_Interval_P_TO_C     ISO_Interval
        # LL/CIS/CEN/BV-01-C    4       10 ms                   10 ms                   20 ms
        # LL/CIS/CEN/BV-02-C    4       20 ms                   10 ms                   20 ms
        # LL/CIS/CEN/BV-25-C    2       40 ms                   40 ms                   40 ms
        # LL/CIS/CEN/BV-31-C    4       10 ms                   10 ms                   20 ms
        # LL/CIS/CEN/BV-32-C    2       40 ms                   40 ms                   40 ms
        # LL/CIS/CEN/BV-39-C    1       20 ms                   20 ms                   20 ms
