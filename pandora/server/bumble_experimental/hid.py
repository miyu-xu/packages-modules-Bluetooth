import grpc
import logging

from bumble.device import Device
from pandora_experimental.hid_grpc import (
    SendHostReportRequest,
    SendHostReportResponse,
)
from pandora_experimental.hid_grpc_aio import HIDServicer


class HIDService(HIDServicer):
    device: Device

    def __init__(self, device: Device) -> None:
        super().__init__()
        self.device = device

    async def SendHostReport(
        self,
        request: SendHostReportRequest,
        context: grpc.ServicerContext
    ) -> SendHostReportResponse:
        logging.info(
            f'SendHostReport(address={request.address}, '
            f'type={request.report_type}, report="{request.report}")'
        )
        # TODO: implement SendHostReport
        return SendHostReportResponse()
