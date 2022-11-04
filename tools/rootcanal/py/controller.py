import lib_rootcanal_python3 as rootcanal
import py.bluetooth
import hci_packets as hci
import link_layer_packets as ll
import collections
import asyncio
import random
from typing import Optional
from hci_packets import ErrorCode
from py.bluetooth import Address


class Controller(rootcanal.BaseController):

    def __init__(self):
        super().__init__(self.receive_hci_, self.receive_ll_)
        self.evt_queue = collections.deque()
        self.acl_queue = collections.deque()
        self.ll_queue = collections.deque()
        self.evt_queue_event = asyncio.Event()
        self.acl_queue_event = asyncio.Event()
        self.ll_queue_event = asyncio.Event()

    def receive_hci_(self, typ: rootcanal.HciType, packet: bytes):
        if typ == rootcanal.HciType.Evt:
            print(f"<-- received HCI event data={len(packet)}[..]")
            self.evt_queue.append(packet)
            self.evt_queue_event.set()
        elif typ == rootcanal.HciType.Acl:
            print(f"<-- received HCI ACL packet data={len(packet)}[..]")
            self.acl_queue.append(packet)
            self.acl_queue_event.set()
        else:
            print(f"ignoring HCI packet typ={typ}")

    def receive_ll_(self, packet: bytes):
        print(f"<-- received LL pdu data={len(packet)}[..]")
        self.ll_queue.append(packet)
        self.ll_queue_event.set()

    def send_cmd(self, cmd: hci.Command):
        print(f"--> sending HCI command {cmd.__class__.__name__}")
        self.send_hci(rootcanal.HciType.Cmd, cmd.serialize())

    def send_ll(self, pdu: ll.LinkLayerPacket, rssi: Optional[int] = None):
        print(f"--> sending LL pdu {pdu.__class__.__name__}")
        if rssi is not None:
            pdu = ll.RssiWrapper(rssi=rssi, payload=pdu.serialize())
        super().send_ll(pdu.serialize())

    def stop(self):
        if self.evt_queue:
            print("evt queue not empty at stop():")
            for packet in self.evt_queue:
                evt = hci.Event.parse_all(packet)
                evt.show()
            raise Exception("evt queue not empty at stop()")

        if self.ll_queue:
            for packet in self.ll_queue:
                pdu = ll.LinkLayerPacket.parse_all(packet)
                pdu.show()
            raise Exception("ll queue not empty at stop()")

        super().stop()

    async def receive_evt(self):
        while not self.evt_queue:
            await self.evt_queue_event.wait()
            self.evt_queue_event.clear()
        return self.evt_queue.popleft()

    async def expect_evt(self, expected_evt: hci.Event):
        packet = await self.receive_evt()
        evt = hci.Event.parse_all(packet)
        if evt != expected_evt:
            print("received unexpected event")
            print("expected event:")
            expected_evt.show()
            print("received event:")
            evt.show()
            raise Exception(f"unexpected evt {evt.__class__.__name__}")


# LL/DDI/SCN/BV-13-C [Network Privacy – Passive Scanning, Peer IRK]
async def BV_13_C(controller: Controller):
    # Test parameters.
    LL_scanner_scanInterval_MIN = 0x2000
    LL_scanner_scanInterval_MAX = 0x2000
    LL_scanner_scanWindow_MIN = 0x200
    LL_scanner_scanWindow_MAX = 0x200
    LL_scanner_Adv_Channel_Map = 0x7

    peer_irk = bytes([1] * 16)
    peer_identity_address = Address.from_str('aa:bb:cc:dd:ee:ff')
    peer_identity_address_type = hci.PeerAddressType.PUBLIC_DEVICE_OR_IDENTITY_ADDRESS
    peer_resolvable_address = Address(rootcanal.generate_rpa(peer_irk))

    # 1. The Upper Tester populates the IUT resolving list with the peer IRK
    # and identity address.
    controller.send_cmd(
        hci.LeAddDeviceToResolvingList(peer_irk=peer_irk,
                                       local_irk=bytes([0] * 16),
                                       peer_identity_address=peer_identity_address,
                                       peer_identity_address_type=peer_identity_address_type))

    await controller.expect_evt(
        hci.LeAddDeviceToResolvingListComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    controller.send_cmd(hci.LeSetResolvablePrivateAddressTimeout(rpa_timeout=0x10))

    await controller.expect_evt(
        hci.LeSetResolvablePrivateAddressTimeoutComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    controller.send_cmd(hci.LeSetAddressResolutionEnable(address_resolution_enable=hci.Enable.ENABLED))

    await controller.expect_evt(
        hci.LeSetAddressResolutionEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    # 2. The Upper Tester enables passive scanning in the IUT.
    controller.send_cmd(
        hci.LeSetScanParameters(le_scan_type=hci.LeScanType.PASSIVE,
                                le_scan_interval=LL_scanner_scanInterval_MAX,
                                le_scan_window=LL_scanner_scanWindow_MAX,
                                own_address_type=hci.OwnAddressType.RESOLVABLE_OR_PUBLIC_ADDRESS,
                                scanning_filter_policy=hci.LeScanningFilterPolicy.ACCEPT_ALL))

    await controller.expect_evt(hci.LeSetScanParametersComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    controller.send_cmd(hci.LeSetScanEnable(le_scan_enable=hci.Enable.ENABLED, filter_duplicates=hci.Enable.DISABLED))

    await controller.expect_evt(hci.LeSetScanEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    # 3. Configure the Lower Tester to start advertising. The Lower Tester uses
    # a resolvable private address in the AdvA field.
    # 4. The Lower Tester sends an ADV_NONCONN_IND packet each advertising event
    # using the selected advertising channel only. Repeat for at least 20
    # advertising intervals.
    controller.send_ll(ll.LeLegacyAdvertisingPdu(source_address=peer_resolvable_address,
                                                 advertising_address_type=ll.AddressType.RANDOM,
                                                 advertising_type=ll.LegacyAdvertisingType.ADV_NONCONN_IND,
                                                 advertising_data=[1, 2, 3]),
                       rssi=0xf0)

    # 5. The Upper Tester receives at least one HCI_LE_Advertising_Report
    # reporting the advertising packets sent by the Lower Tester. The address in
    # the report is resolved by the IUT using the distributed IRK.
    await controller.expect_evt(
        hci.LeAdvertisingReportRaw(responses=[
            hci.LeAdvertisingResponseRaw(event_type=hci.AdvertisingEventType.ADV_NONCONN_IND,
                                         address_type=hci.AddressType.PUBLIC_IDENTITY_ADDRESS,
                                         address=peer_identity_address,
                                         advertising_data=[1, 2, 3],
                                         rssi=0xf0)
        ]))

    # 6. The Upper Tester sends an HCI_LE_Set_Scan_Enable to the IUT to stop the
    # scanning function and receives an HCI_Command_Complete event in response.
    controller.send_cmd(hci.LeSetScanEnable(le_scan_enable=hci.Enable.DISABLED))

    await controller.expect_evt(hci.LeSetScanEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    # 7. The Upper Tester disables address resolution.
    controller.send_cmd(hci.LeSetAddressResolutionEnable(address_resolution_enable=hci.Enable.DISABLED))

    await controller.expect_evt(
        hci.LeSetAddressResolutionEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    # 8. The Upper Tester enables passive scanning in the IUT.
    controller.send_cmd(hci.LeSetScanEnable(le_scan_enable=hci.Enable.ENABLED, filter_duplicates=hci.Enable.DISABLED))

    await controller.expect_evt(hci.LeSetScanEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    # 9. The Lower Tester sends an ADV_NONCONN_IND packet each advertising event
    # using the selected advertising channel only. Repeat for at least 20
    # advertising intervals.
    controller.send_ll(ll.LeLegacyAdvertisingPdu(source_address=peer_resolvable_address,
                                                 advertising_address_type=ll.AddressType.RANDOM,
                                                 advertising_type=ll.LegacyAdvertisingType.ADV_NONCONN_IND,
                                                 advertising_data=[1, 2, 3]),
                       rssi=0xf0)

    # 10. The IUT does not resolve the Lower Tester’s address and reports it
    # unresolved (as received in the advertising PDU) in the advertising report
    # events to the Upper Tester.
    await controller.expect_evt(
        hci.LeAdvertisingReportRaw(responses=[
            hci.LeAdvertisingResponseRaw(event_type=hci.AdvertisingEventType.ADV_NONCONN_IND,
                                         address_type=hci.AddressType.RANDOM_DEVICE_ADDRESS,
                                         address=peer_resolvable_address,
                                         advertising_data=[1, 2, 3],
                                         rssi=0xf0)
        ]))

    # 11. The Upper Tester sends an HCI_LE_Set_Scan_Enable to the IUT to stop the
    # scanning function and receives an HCI_Command_Complete event in response.
    controller.send_cmd(hci.LeSetScanEnable(le_scan_enable=hci.Enable.DISABLED))

    await controller.expect_evt(hci.LeSetScanEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))


async def main():
    controller = Controller()
    controller.start()

    # Reset the controller and enable all events and LE events.
    controller.send_cmd(hci.Reset())

    await controller.expect_evt(hci.ResetComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    controller.send_cmd(hci.SetEventMask(event_mask=0xffffffffffffffff))

    await controller.expect_evt(hci.SetEventMaskComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    controller.send_cmd(hci.LeSetEventMask(le_event_mask=0xffffffffffffffff))

    await controller.expect_evt(hci.LeSetEventMaskComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

    # Start test.
    await BV_13_C(controller)
    controller.stop()


if __name__ == '__main__':
    asyncio.run(main())
