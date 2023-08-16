#!/usr/bin/env python3
#
#   Copyright 2020 - The Android Open Source Project
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

import logging

from blueberry.tests.gd.cert.behavior import IHasBehaviors, SingleArgumentBehavior, ReplyStage
from blueberry.tests.gd.cert.captures import L2capCaptures
from blueberry.tests.gd.cert.closable import Closable
from blueberry.tests.gd.cert.closable import safeClose
from blueberry.tests.gd.cert.event_stream import FilteringEventStream
from blueberry.tests.gd.cert.event_stream import IEventStream
from blueberry.tests.gd.cert.matchers import L2capMatchers
from blueberry.tests.gd.cert.py_acl_manager import PyAclManager
from blueberry.tests.gd.cert.truth import assertThat
import l2cap_packets as l2cap


class CertL2capChannel(Closable, IEventStream):

    def __init__(self, device, scid, dcid, acl_stream, acl, control_channel, fcs=None):
        self._device = device
        self._scid = scid
        self._dcid = dcid
        self._acl_stream = acl_stream
        self._acl = acl
        self._control_channel = control_channel
        self._config_rsp_received = False
        self._config_rsp_sent = False
        if fcs == l2cap.FcsType.DEFAULT:
            self._our_acl_view = FilteringEventStream(acl_stream, L2capMatchers.ExtractBasicFrameWithFcs(scid))
        else:
            self._our_acl_view = FilteringEventStream(acl_stream, L2capMatchers.ExtractBasicFrame(scid))

    def close(self):
        self._acl_stream.close()
        safeClose(self._acl)

    def get_event_queue(self):
        return self._our_acl_view.get_event_queue()

    def is_configured(self):
        return self._config_rsp_received and self._config_rsp_sent

    def send(self, packet):
        frame = l2cap.BasicFrame(self._dcid, packet)
        self._acl.send(frame.Serialize())

    def send_i_frame(self,
                     tx_seq,
                     req_seq,
                     f=l2cap.Final.NOT_SET,
                     sar=l2cap.SegmentationAndReassembly.UNSEGMENTED,
                     payload=None,
                     fcs=False):
        if fcs == l2cap.FcsType.DEFAULT:
            frame = l2cap.EnhancedInformationFrameWithFcs(self._dcid, tx_seq, f, req_seq, sar, payload)
        else:
            frame = l2cap.EnhancedInformationFrame(self._dcid, tx_seq, f, req_seq, sar, payload)
        self._acl.send(frame.Serialize())

    def send_s_frame(self, req_seq, s=l2cap.SupervisoryFunction.RECEIVER_READY, p=l2cap.Poll.NOT_SET, f=l2cap.Final.NOT_SET):
        frame = l2cap.EnhancedSupervisoryFrame(self._dcid, s, p, f, req_seq)
        self._acl.send(frame.Serialize())

    def config_request_for_me(self):
        return L2capMatchers.ConfigurationRequest(self._scid)

    def send_configure_request(self, options, sid=2, continuation=l2cap.Continuation.END):
        assertThat(self._scid).isNotEqualTo(1)
        request = l2cap.ConfigurationRequest(sid, self._dcid, continuation, options)
        self._control_channel.send(request)

    def _send_information_request(self, type):
        assertThat(self._scid).isEqualTo(1)
        information_request = l2cap.InformationRequest(type)
        self.send(information_request)

    def send_extended_features_request(self):
        self._send_information_request(l2cap.InformationRequestInfoType.EXTENDED_FEATURES_SUPPORTED)

    def verify_configuration_request_and_respond(self, result=l2cap.ConfigurationResponseResult.SUCCESS, options=None):
        request_capture = L2capCaptures.ConfigurationRequest(self._scid)
        assertThat(self._control_channel).emits(request_capture)
        request = request_capture.get()
        sid = request.identifier
        if options is None:
            options = []
        config_response = l2cap.ConfigurationResponse(sid, self._dcid, l2cap.Continuation.END,
                                                                     result, options)
        self._control_channel.send(config_response)

    def send_configuration_response(self, request, result=l2cap.ConfigurationResponseResult.SUCCESS, options=None):
        sid = request.identifier
        if options is None:
            options = []
        config_response = l2cap.ConfigurationResponse(sid, self._dcid, l2cap.Continuation.END,
                                                                     result, options)
        self._control_channel.send(config_response)
        self._config_rsp_sent = True

    def verify_configuration_response(self, result=l2cap.ConfigurationResponseResult.SUCCESS):
        assertThat(self._control_channel).emits(L2capMatchers.ConfigurationResponse(result))

    def disconnect_and_verify(self):
        assertThat(self._scid).isNotEqualTo(1)
        self._control_channel.send(l2cap.DisconnectionRequest(1, self._dcid, self._scid))

        assertThat(self._control_channel).emits(L2capMatchers.DisconnectionResponse(self._scid, self._dcid))

    def verify_disconnect_request(self):
        assertThat(self._control_channel).emits(L2capMatchers.DisconnectionRequest(self._dcid, self._scid))


class CertL2capControlChannelBehaviors(object):

    def __init__(self, parent):
        self.on_config_req_behavior = SingleArgumentBehavior(
            lambda: CertL2capControlChannelBehaviors.CertReplyStage(parent))

    def on_config_req(self, matcher):
        return self.on_config_req_behavior.begin(matcher)

    class CertReplyStage(ReplyStage):

        def __init__(self, parent):
            self.parent = parent

        def send_configuration_response(self, result=l2cap.ConfigurationResponseResult.SUCCESS, options=None):
            self._commit(lambda request: self._send_configuration_response(request, result, options))
            return self

        def _send_configuration_response(self, request, result=l2cap.ConfigurationResponseResult.SUCCESS, options=None):
            dcid = request.destination_cid
            if dcid not in self.parent.scid_to_channel:
                logging.warning("Received config request with unknown dcid")
                return
            self.parent.scid_to_channel[dcid].send_configuration_response(request, result, options)


class CertL2cap(Closable, IHasBehaviors):

    def __init__(self, device):
        self._device = device
        self._acl_manager = PyAclManager(device)
        self._acl = None

        self.control_table = {
            l2cap.CommandCode.CONNECTION_RESPONSE: self._on_connection_response_default,
            l2cap.CommandCode.CONFIGURATION_REQUEST: self._on_configuration_request_default,
            l2cap.CommandCode.CONFIGURATION_RESPONSE: self._on_configuration_response_default,
            l2cap.CommandCode.DISCONNECTION_REQUEST: self._on_disconnection_request_default,
            l2cap.CommandCode.DISCONNECTION_RESPONSE: self._on_disconnection_response_default,
            l2cap.CommandCode.INFORMATION_REQUEST: self._on_information_request_default,
            l2cap.CommandCode.INFORMATION_RESPONSE: self._on_information_response_default
        }

        self.scid_to_channel = {}

        self.support_ertm = True
        self.support_fcs = True

        self._control_behaviors = CertL2capControlChannelBehaviors(self)
        self._control_behaviors.on_config_req_behavior.set_default(self._send_configuration_response_default)

    def close(self):
        self._acl_manager.close()
        safeClose(self._acl)

    def get_behaviors(self):
        return self._control_behaviors

    def connect_acl(self, remote_addr):
        self._acl_manager.initiate_connection(remote_addr)
        self._acl = self._acl_manager.complete_outgoing_connection()
        self.control_channel = CertL2capChannel(
            self._device, 1, 1, self._acl.acl_stream, self._acl, control_channel=None)
        self._acl.acl_stream.register_callback(self._handle_control_packet)

    def accept_incoming_connection(self):
        self._acl_manager.listen_for_an_incoming_connection()
        self._acl = self._acl_manager.complete_incoming_connection()

    def open_channel(self, psm, scid, fcs=None):
        self.control_channel.send(l2cap.ConnectionRequest(psm, scid))

        response = L2capCaptures.ConnectionResponse(scid)
        assertThat(self.control_channel).emits(response)
        channel = CertL2capChannel(self._device, scid,
                                   response.get().destination_cid, self._acl.acl_stream, self._acl,
                                   self.control_channel, fcs)
        self.scid_to_channel[scid] = channel

        return channel

    def verify_and_respond_open_channel_from_remote(self, psm=0x33, scid=None, fcs=None):

        request = L2capCaptures.ConnectionRequest(psm)
        assertThat(self.control_channel).emits(request)

        sid = request.get().identifier
        dcid = request.get().source_cid
        if scid is None or scid in self.scid_to_channel:
            scid = dcid
        channel = CertL2capChannel(self._device, scid, dcid, self._acl.acl_stream, self._acl, self.control_channel, fcs)
        self.scid_to_channel[scid] = channel

        connection_response = l2cap.ConnectionResponse(
            sid, scid, dcid, l2cap.ConnectionResponseResult.SUCCESS,
            l2cap.ConnectionResponseStatus.NO_FURTHER_INFORMATION_AVAILABLE)
        self.control_channel.send(connection_response)

        return channel

    def verify_and_respond_open_channel_from_remote_and_send_config_req(self, psm=0x33):
        """
        Verify a connection request, and send a combo packet of connection response and configuration request
        """
        request = L2capCaptures.ConnectionRequest(psm)
        assertThat(self.control_channel).emits(request)

        sid = request.get().identifier
        dcid = request.get().source_cid
        scid = dcid
        channel = CertL2capChannel(self._device, scid, dcid, self._acl.acl_stream, self._acl, self.control_channel)
        self.scid_to_channel[scid] = channel

        # Connection response and config request combo packet
        conn_rsp_and_config_req = [
            0x03, sid, 0x08, 0x00, dcid, 0x00, dcid, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, sid + 1, 0x04, 0x00, dcid,
            0x00, 0x00, 0x00
        ]
        self.control_channel.send(conn_rsp_and_config_req)

        return channel

    # prefer to use channel abstraction instead, if at all possible
    def send_acl(self, packet):
        self._acl.send(packet.Serialize())

    def get_control_channel(self):
        return self.control_channel

    # Disable ERTM when exchange extened feature
    def claim_ertm_unsupported(self):
        self.support_ertm = False

    def _on_connection_response_default(self, l2cap_control_view):
        pass

    def _on_configuration_request_default(self, l2cap_control_view):
        request = l2cap.ConfigurationRequest(l2cap_control_view)
        dcid = request.destination_cid
        if dcid not in self.scid_to_channel:
            logging.warning("Received config request with unknown dcid")
            return
        self._control_behaviors.on_config_req_behavior.run(request)

    def _send_configuration_response_default(self, captured_request_view):
        dcid = captured_request_view.destination_cid
        if dcid not in self.scid_to_channel:
            return
        self.scid_to_channel[dcid].send_configuration_response(captured_request_view)

    @staticmethod
    def config_option_basic_explicit(mtu=642):
        mtu_opt = l2cap.MtuConfigurationOption()
        mtu_opt.mtu = mtu
        rfc_opt = l2cap.RetransmissionAndFlowControlConfigurationOption()
        rfc_opt.mode = l2cap.RetransmissionAndFlowControlModeOption.L2CAP_BASIC
        return [mtu_opt, rfc_opt]

    @staticmethod
    def config_option_mtu_explicit(mtu=642):
        mtu_opt = l2cap.MtuConfigurationOption()
        mtu_opt.mtu = mtu
        return [mtu_opt]

    @staticmethod
    def config_option_ertm(mtu=642,
                           fcs=None,
                           max_transmit=10,
                           mps=1010,
                           tx_window_size=10,
                           monitor_time_out=2000,
                           retransmission_time_out=1000):
        result = []
        mtu_opt = l2cap.MtuConfigurationOption()
        mtu_opt.mtu = mtu
        result.append(mtu_opt)
        if fcs is not None:
            fcs_opt = l2cap.FrameCheckSequenceOption()
            fcs_opt.fcs_type = fcs
            result.append(fcs_opt)
        rfc_opt = l2cap.RetransmissionAndFlowControlConfigurationOption()
        rfc_opt.mode = l2cap.RetransmissionAndFlowControlModeOption.ENHANCED_RETRANSMISSION
        rfc_opt.tx_window_size = tx_window_size
        rfc_opt.max_transmit = max_transmit
        rfc_opt.retransmission_time_out = retransmission_time_out
        rfc_opt.monitor_time_out = monitor_time_out
        rfc_opt.maximum_pdu_size = mps
        result.append(rfc_opt)
        return result

    @staticmethod
    def config_option_ertm_with_max_transmit_one():
        return CertL2cap.config_option_ertm(max_transmit=1)

    @staticmethod
    def config_option_ertm_with_mps(mps=1010):
        return CertL2cap.config_option_ertm(mps=mps)

    def _on_configuration_response_default(self, l2cap_control_view):
        response = l2cap.ConfigurationResponse(l2cap_control_view)
        scid = response.source_cid
        if scid not in self.scid_to_channel:
            logging.warning("Received config request with unknown dcid")
            return
        result = response.result
        if result == l2cap.ConfigurationResponseResult.SUCCESS:
            self.scid_to_channel[scid]._config_rsp_received = True

    def _on_disconnection_request_default(self, l2cap_control_view):
        disconnection_request = l2cap.DisconnectionRequest(l2cap_control_view)
        sid = disconnection_request.identifier
        scid = disconnection_request.source_cid
        dcid = disconnection_request.destination_cid
        disconnection_response = l2cap.DisconnectionResponse(sid, dcid, scid)
        self.control_channel.send(disconnection_response)

    def _on_disconnection_response_default(self, l2cap_control_view):
        pass

    def _on_information_request_default(self, l2cap_control_view):
        information_request = l2cap.InformationRequest(l2cap_control_view)
        sid = information_request.identifier
        information_type = information_request.info_type
        if information_type == l2cap.InformationRequestInfoType.CONNECTIONLESS_MTU:
            response = l2cap.InformationResponseConnectionlessMtu(
                sid, l2cap.InformationRequestResult.SUCCESS, 100)
            self.control_channel.send(response)
            return
        if information_type == l2cap.InformationRequestInfoType.EXTENDED_FEATURES_SUPPORTED:
            response = l2cap.InformationResponseExtendedFeatures(
                sid, l2cap.InformationRequestResult.SUCCESS, 0, 0, 0, self.support_ertm, 0, self.support_fcs, 0,
                0, 0, 0, 0)
            self.control_channel.send(response)
            return
        if information_type == l2cap.InformationRequestInfoType.FIXED_CHANNELS_SUPPORTED:
            response = l2cap.InformationResponseFixedChannels(
                sid, l2cap.InformationRequestResult.SUCCESS, 2)
            self.control_channel.send(response)
            return

    def _on_information_response_default(self, l2cap_control_view):
        pass

    def _handle_control_packet(self, l2cap_packet):
        packet_bytes = l2cap_packet.payload
        l2cap_view = l2cap.BasicFrame(packet_bytes)
        if l2cap_view.channel_id != 1:
            return
        l2cap_control_view = l2cap.Control(l2cap_view.GetPayload())
        fn = self.control_table.get(l2cap_control_view.code)
        if fn is not None:
            fn(l2cap_control_view)
