from dataclasses import dataclass
import hci_packets as hci
import link_layer_packets as ll
import unittest
from hci_packets import ErrorCode
from py.bluetooth import Address
from py.controller import ControllerTest


class Test(ControllerTest):

    inquiry_address_1 = Address('aa:aa:aa:aa:aa:01')
    inquiry_address_2 = Address('aa:aa:aa:aa:aa:02')
    inquiry_class_of_device_1 = 0xaaaa01
    inquiry_class_of_device_2 = 0xaaaa02

    # Verify that the controller properly implements the event filter
    # with filter type INQUIRY_RESULT and filter condition ALL_DEVICES.
    async def test_inquiry_result_all_devices(self):
        controller = self.controller

        controller.send_cmd(hci.SetEventFilterInquiryResultAllDevices())

        await self.expect_evt(hci.SetEventFilterComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        controller.send_cmd(hci.WriteScanEnable(scan_enable=hci.ScanEnable.INQUIRY_SCAN_ONLY))

        await self.expect_evt(hci.WriteScanEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        await self.expect_ll(
            ll.Inquiry(source_address=controller.address,
                       destination_address=Address(),
                       inquiry_type=ll.InquiryType.STANDARD,
                       lap=0))

        controller.send_ll(
            ll.InquiryResponse(source_address=Test.inquiry_address_1,
                               destination_address=controller.address,
                               page_scan_repetition_mode=1,
                               class_of_device=Test.inquiry_class_of_device_1,
                               clock_offset=0))

        controller.send_ll(
            ll.InquiryResponse(source_address=Test.inquiry_address_2,
                               destination_address=controller.address,
                               page_scan_repetition_mode=1,
                               class_of_device=Test.inquiry_class_of_device_2,
                               clock_offset=0))

        await self.expect_evt(
            hci.InquiryResult(responses=[
                hci.InquiryResponse(bd_addr=Test.inquiry_address_1,
                                    page_scan_repetition_mode=hci.PageScanRepetitionMode.R1,
                                    class_of_device=Test.inquiry_class_of_device_1,
                                    clock_offset=0)
            ]))

        await self.expect_evt(
            hci.InquiryResult(responses=[
                hci.InquiryResponse(bd_addr=Test.inquiry_address_2,
                                    page_scan_repetition_mode=hci.PageScanRepetitionMode.R1,
                                    class_of_device=Test.inquiry_class_of_device_2,
                                    clock_offset=0)
            ]))

    # Verify that the controller properly implements the event filter
    # with filter type INQUIRY_RESULT and filter condition CLASS_OF_DEVICE.
    async def test_inquiry_result_class_of_device(self):
        controller = self.controller

        controller.send_cmd(
            hci.SetEventFilterInquiryResultClassOfDevice(class_of_device=0xaaaa02, class_of_device_mask=0xffffff))

        await self.expect_evt(hci.SetEventFilterComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        controller.send_cmd(hci.WriteScanEnable(scan_enable=hci.ScanEnable.INQUIRY_SCAN_ONLY))

        await self.expect_evt(hci.WriteScanEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        await self.expect_ll(
            ll.Inquiry(source_address=controller.address,
                       destination_address=Address(),
                       inquiry_type=ll.InquiryType.STANDARD,
                       lap=0))

        controller.send_ll(
            ll.InquiryResponse(source_address=Test.inquiry_address_1,
                               destination_address=controller.address,
                               page_scan_repetition_mode=1,
                               class_of_device=Test.inquiry_class_of_device_1,
                               clock_offset=0))

        controller.send_ll(
            ll.InquiryResponse(source_address=Test.inquiry_address_2,
                               destination_address=controller.address,
                               page_scan_repetition_mode=1,
                               class_of_device=Test.inquiry_class_of_device_2,
                               clock_offset=0))

        await self.expect_evt(
            hci.InquiryResult(responses=[
                hci.InquiryResponse(bd_addr=Test.inquiry_address_2,
                                    page_scan_repetition_mode=hci.PageScanRepetitionMode.R1,
                                    class_of_device=Test.inquiry_class_of_device_2,
                                    clock_offset=0)
            ]))

        controller.send_cmd(
            hci.SetEventFilterInquiryResultClassOfDevice(class_of_device=0xaaaa00, class_of_device_mask=0xffff00))

        await self.expect_evt(hci.SetEventFilterComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        await self.expect_ll(
            ll.Inquiry(source_address=controller.address,
                       destination_address=Address(),
                       inquiry_type=ll.InquiryType.STANDARD,
                       lap=0))

        controller.send_ll(
            ll.InquiryResponse(source_address=Test.inquiry_address_1,
                               destination_address=controller.address,
                               page_scan_repetition_mode=1,
                               class_of_device=Test.inquiry_class_of_device_1,
                               clock_offset=0))

        controller.send_ll(
            ll.InquiryResponse(source_address=Test.inquiry_address_2,
                               destination_address=controller.address,
                               page_scan_repetition_mode=1,
                               class_of_device=Test.inquiry_class_of_device_2,
                               clock_offset=0))

        await self.expect_evt(
            hci.InquiryResult(responses=[
                hci.InquiryResponse(bd_addr=Test.inquiry_address_1,
                                    page_scan_repetition_mode=hci.PageScanRepetitionMode.R1,
                                    class_of_device=Test.inquiry_class_of_device_1,
                                    clock_offset=0)
            ]))

        await self.expect_evt(
            hci.InquiryResult(responses=[
                hci.InquiryResponse(bd_addr=Test.inquiry_address_2,
                                    page_scan_repetition_mode=hci.PageScanRepetitionMode.R1,
                                    class_of_device=Test.inquiry_class_of_device_2,
                                    clock_offset=0)
            ]))

    # Verify that the controller properly implements the event filter
    # with filter type INQUIRY_RESULT and filter condition ADDRESS.
    async def test_inquiry_result_address(self):
        controller = self.controller

        controller.send_cmd(hci.SetEventFilterInquiryResultAddress(address=Test.inquiry_address_2))

        await self.expect_evt(hci.SetEventFilterComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        controller.send_cmd(hci.WriteScanEnable(scan_enable=hci.ScanEnable.INQUIRY_SCAN_ONLY))

        await self.expect_evt(hci.WriteScanEnableComplete(status=ErrorCode.SUCCESS, num_hci_command_packets=1))

        await self.expect_ll(
            ll.Inquiry(source_address=controller.address,
                       destination_address=Address(),
                       inquiry_type=ll.InquiryType.STANDARD,
                       lap=0))

        controller.send_ll(
            ll.InquiryResponse(source_address=Test.inquiry_address_1,
                               destination_address=controller.address,
                               page_scan_repetition_mode=1,
                               class_of_device=Test.inquiry_class_of_device_1,
                               clock_offset=0))

        controller.send_ll(
            ll.InquiryResponse(source_address=Test.inquiry_address_2,
                               destination_address=controller.address,
                               page_scan_repetition_mode=1,
                               class_of_device=Test.inquiry_class_of_device_2,
                               clock_offset=0))

        await self.expect_evt(
            hci.InquiryResult(responses=[
                hci.InquiryResponse(bd_addr=Test.inquiry_address_2,
                                    page_scan_repetition_mode=hci.PageScanRepetitionMode.R1,
                                    class_of_device=Test.inquiry_class_of_device_2,
                                    clock_offset=0)
            ]))

    # Verify that the controller properly implements the event filter
    # with filter type CONNECTION_SETUP and filter condition ALL_DEVICES and
    # auto accept flag OFF.
    async def test_connection_setup_all_devices(self):
        pass

    # Verify that the controller properly implements the event filter
    # with filter type CONNECTION_SETUP and filter condition CLASS_OF_DEVICE and
    # auto accept flag OFF.
    async def test_connection_setup_class_of_device(self):
        pass

    # Verify that the controller properly implements the event filter
    # with filter type CONNECTION_SETUP and filter condition ADDRESS and
    # auto accept flag OFF.
    async def test_connection_setup_address(self):
        pass

    # Verify that the controller properly implements the event filter
    # with auto accept flag ON and role switch DISABLED.
    async def test_connection_auto_accept_role_switch_disabled(self):
        pass

    # Verify that the controller properly implements the event filter
    # with auto accept flag ON and role switch ENABLED.
    async def test_connection_auto_accept_role_switch_enabled(self):
        pass
