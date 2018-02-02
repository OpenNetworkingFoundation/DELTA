package org.deltaproject.channelagent.testcase;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.deltaproject.channelagent.dummy.DummyController;
import org.deltaproject.channelagent.dummy.DummySwitch;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.errormsg.OFHelloFailedErrorMsg;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBand;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by seungsoo on 11/07/2017.
 */
public class TestSwitchCase {
    private static final Logger log = LoggerFactory.getLogger(TestSwitchCase.class);

    public static final int HANDSHAKE_DEFAULT = 0;
    public static final int HANDSHAKE_NO_HELLO = 1;
    public static final int HANDSHAKE_INCOMPATIBLE_HELLO = 2;
    public static final int NO_HANDSHAKE = 3;
    public static final int DEFAULT_TIMEOUT = 5000;

    private String targetIP;
    private String targetPORT;
    private byte targetOFVersion;

    private DummyController dummyController;

    private OFFactory defaultFactory;
    private Random random;

    private long requestXid = 0xeeeeeeeel;
    private OFMessage response;

    private Process proc;
    private int pid;
    private int ofport;
    private long r_xid = 0xeeeeeeeel;

    public TestSwitchCase(String ip, byte ver, String port) {
        targetIP = ip;
        targetPORT = port;
        targetOFVersion = ver;

        random = new Random();

        if (targetOFVersion == 1)
            defaultFactory = OFFactories.getFactory(OFVersion.OF_10);
        else if (targetOFVersion == 4)
            defaultFactory = OFFactories.getFactory(OFVersion.OF_13);

        ofport = Integer.parseInt(port);
    }

    public void runDummyController(int type) {
        dummyController = new DummyController(targetOFVersion, ofport);
        dummyController.listeningSwitch();
        dummyController.setHandShakeType(type);
        dummyController.start();

        if (type != HANDSHAKE_DEFAULT)
            return;

        while (!dummyController.getHandshaked()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        log.info("[Channel Agent] switch handshake completed");
    }

    public void stopDummyController() {
        dummyController.interrupt();

        while (!dummyController.isSockClosed()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /*
     * 1.1.010 - Port Range Violation
     */
    public String testPortRangeViolation(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Port Range Violation test");

        String result = "";
        OFPortMod request = defaultFactory.buildPortMod().setXid(r_xid).setPortNo(OFPort.ANY).build();
        log.info("[Channel Agent] Send msg: " + request.toString());
        dummyController.sendMsg(request, -1);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.020 - Table Identifier Violation (>=OF_1.3)
     */
    public String testTableID(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Table Identifier Violation");

        int inport = 1;
        int outport = 2;
        String result = "";

        Match.Builder mb = defaultFactory.buildMatch();
        mb.setExact(MatchField.IN_PORT, OFPort.ofInt(inport));

        OFActionOutput.Builder aob = defaultFactory.actions().buildOutput();
        aob.setMaxLen(Integer.MAX_VALUE);
        aob.setPort(OFPort.ofInt(outport));

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(aob.build());

        OFInstructionApplyActions apa = defaultFactory.instructions().applyActions(actions);
        List<OFInstruction> inst = new ArrayList<OFInstruction>();
        inst.add(apa);

        Set<OFFlowModFlags> set = new HashSet<OFFlowModFlags>();
        set.add(OFFlowModFlags.SEND_FLOW_REM);

        OFFlowAdd.Builder fab = defaultFactory.buildFlowAdd();
        fab.setPriority(10);
        fab.setXid(r_xid);

        Random ran = new Random();
        fab.setTableId(TableId.of(255));                // table num
        fab.setMatch(mb.build());
        fab.setInstructions(inst);
        fab.setBufferId(OFBufferId.NO_BUFFER);
        fab.setFlags(set);

        OFFlowAdd request = fab.build();
        log.info("[Channel Agent] Send msg: " + request.toString());
        dummyController.sendMsg(request, -1);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.030 - Group Identifier Violation (>=OF 1.3)
     */
    public String testGroupID(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Group Identifier Violation");

        int outport = 2;
        String result = "";

        OFActionOutput.Builder aob = defaultFactory.actions().buildOutput();
        aob.setMaxLen(Integer.MAX_VALUE);
        aob.setPort(OFPort.ofInt(outport));

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(aob.build());

        OFBucket.Builder bb = defaultFactory.buildBucket();
        bb.setActions(actions);

        List<OFBucket> bklist = new ArrayList<OFBucket>();
        bklist.add(bb.build());

        OFGroupModify.Builder gab = defaultFactory.buildGroupModify();
        gab.setGroupType(OFGroupType.ALL);
        gab.setGroup(OFGroup.MAX);
        gab.setBuckets(bklist);
        gab.setXid(this.r_xid);

        OFGroupModify request = gab.build();
        log.info("[Channel Agent] Send msg: " + request.toString());
        dummyController.sendMsg(request, -1);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.040 - Meter Identifier Violation (>=OF 1.3)
     */
    public String testMeterID(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Meter Identifier Violation");

        String result = "";

        OFMeterBand ofMeterBand = defaultFactory.meterBands().buildDrop().build();

        OFMeterMod request = defaultFactory.buildMeterMod()
                .setXid(this.r_xid)
//                .setMeterId(0xFFFFFFFFl)
                .setMeterId(0x1)
                .setCommand(OFMeterModCommand.ADD)
                .setFlags(Sets.newHashSet(OFMeterFlags.KBPS))
                .setMeters(Arrays.asList(ofMeterBand))
                .build();


        log.info("[Channel Agent] Send msg: " + request.toString());
        dummyController.sendMsg(request, -1);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.050 - Table Loop Violation (>=OF 1.3)
     */
    public String testTableLoop(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Table Loop Violation - ");

        int inport = 1;
        int outport = 2;
        String result = "";

        Match.Builder mb = defaultFactory.buildMatch();
        mb.setExact(MatchField.IN_PORT, OFPort.ofInt(inport));

        OFActionOutput.Builder aob = defaultFactory.actions().buildOutput();
        aob.setMaxLen(Integer.MAX_VALUE);
        aob.setPort(OFPort.ofInt(outport));

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(aob.build());

        OFInstructionApplyActions apa = defaultFactory.instructions().applyActions(actions);
        OFInstructionGotoTable gotoinst = defaultFactory.instructions().gotoTable(TableId.of(1));

        List<OFInstruction> inst = new ArrayList<OFInstruction>();
        inst.add(apa);
        inst.add(gotoinst);                     // go to table id 1

        Set<OFFlowModFlags> set = new HashSet<OFFlowModFlags>();
        set.add(OFFlowModFlags.SEND_FLOW_REM);

        OFFlowAdd.Builder fab = defaultFactory.buildFlowAdd();
        fab.setPriority(1000);
        fab.setXid(r_xid);
        fab.setTableId(TableId.of(10));         // table id 10
        fab.setMatch(mb.build());
        fab.setInstructions(inst);
        fab.setBufferId(OFBufferId.NO_BUFFER);
        fab.setFlags(set);

        OFFlowAdd request = fab.build();
        log.info("[Channel Agent] Send msg: " + request.toString());
        dummyController.sendMsg(request, -1);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.060 - Corrupted Control Message Type
     */
    public String testCorruptedControlMsgType(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Corrupted Control Message Type");

        String result = "";

        byte[] rawPkt = new byte[8];
        if (targetOFVersion == 4)
            rawPkt[0] = 0x04; // version
        else if (targetOFVersion == 1)
            rawPkt[0] = 0x01;
        else
            return "null";

        rawPkt[1] = 0x61; // Type == 97
        rawPkt[2] = 0x00;
        rawPkt[3] = 0x08;

        rawPkt[4] = (byte) 0xee;
        rawPkt[5] = (byte) 0xee;
        rawPkt[6] = (byte) 0xee;
        rawPkt[7] = (byte) 0xee;

        log.info("[Channel] Send msg with unsupported OF type (97)");
        dummyController.sendRawMsg(rawPkt);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += "unsupported OF type (0x97)";
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.070 - Unsupported Version Numeber
     */
    public String testUnsupportedVersionNumber(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Unsupported Version Numeber");

        String result = "";
        runDummyController(HANDSHAKE_INCOMPATIBLE_HELLO);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += "unsupported version HELLO msg (0xee)";
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.080 - Malformed Version Number
     */
    public String testMalformedVersionNumber(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Malformed Version Number");

        String result = "";
        OFPortMod request = defaultFactory.buildPortMod().setXid(r_xid).setPortNo(OFPort.of(1))
                .setConfig(new HashSet<>(Arrays.asList(OFPortConfig.NO_STP))).build();
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1024);
        request.writeTo(buf);

        int length = buf.readableBytes();
        byte[] bytes = new byte[length];
        buf.getBytes(buf.readerIndex(), bytes);

        if (targetOFVersion == 4)
            bytes[0] = 0x01; // version
        else if (targetOFVersion == 1)
            bytes[0] = 0x04;

        log.info("[Channel Agent] Send msg :" + request.toString());
        dummyController.sendRawMsg(bytes);

        buf.clear();
        buf.release();

        Thread.sleep(2000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.090 - Invalid OXM - Type (Command)
     */
    public String testInvalidOXMType(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Invalid OXM - Type");

        String result = "";
        int inport = 1;
        int outport = 2;

        Match.Builder mb = defaultFactory.buildMatch();
        mb.setExact(MatchField.IN_PORT, OFPort.ofInt(inport));

        OFActionOutput.Builder aob = defaultFactory.actions().buildOutput();
        aob.setMaxLen(Integer.MAX_VALUE);
        aob.setPort(OFPort.ofInt(outport));

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(aob.build());

        /*OFInstructionApplyActions apa = defaultFactory.instructions().applyActions(actions);
        List<OFInstruction> inst = new ArrayList<OFInstruction>();
        inst.add(apa);*/

        OFFlowAdd.Builder fab = defaultFactory.buildFlowAdd();
        fab.setXid(r_xid);
        fab.setMatch(mb.build());
        fab.setActions(actions);
        fab.setHardTimeout(1000);

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1024);

        OFFlowAdd request = fab.build();
        request.writeTo(buf);

        if (targetOFVersion == 4)
            buf.setByte(25, 0x5);                   // unknown command 0x05
        else if (targetOFVersion == 1)
            buf.setByte(57, 0x5);

        log.info("[Channel Agent] Send msg :" + request.toString() + " with unknown command (0x05) FlowMod");
        dummyController.sendRawMsg(buf);

        Thread.sleep(2000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.100 - Invalid OXM - Length (>=OF 1.3)
     */
    public String testInvalidOXMLength(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Invalid OXM - Length");

        String result = "";
        int inport = 1;
        int outport = 2;

        Match.Builder mb = defaultFactory.buildMatch();
        mb.setExact(MatchField.IN_PORT, OFPort.ofInt(inport));

        OFActionOutput.Builder aob = defaultFactory.actions().buildOutput();
        aob.setMaxLen(Integer.MAX_VALUE);
        aob.setPort(OFPort.ofInt(outport));

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(aob.build());

        OFFlowAdd.Builder fab = defaultFactory.buildFlowAdd();
        fab.setXid(r_xid);
        fab.setMatch(mb.build());
        fab.setActions(actions);
        fab.setHardTimeout(1000);

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1024);

        OFFlowAdd request = fab.build();
        request.writeTo(buf);

        buf.setByte(55, 0x5);                   // before length 4 -> after 5

        log.info("[Channel Agent] Send msg :" + request.toString() + " with invalid OXM length");
        dummyController.sendRawMsg(buf);

        Thread.sleep(2000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.110 - Invalid OXM - Value (>=OF 1.3)
     */
    public String testInvalidOXMValue(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Invalid OXM - Value");

        String result = "";
        int inport = 1;
        int outport = 2;

        Match.Builder mb = defaultFactory.buildMatch();
        mb.setExact(MatchField.IN_PORT, OFPort.ofInt(inport));
        mb.setExact(MatchField.ETH_TYPE, EthType.of(0x800));
        mb.setExact(MatchField.IP_DSCP, IpDscp.DSCP_63);

        OFActionOutput.Builder aob = defaultFactory.actions().buildOutput();
        aob.setMaxLen(Integer.MAX_VALUE);
        aob.setPort(OFPort.ofInt(outport));

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(aob.build());

        OFInstructionApplyActions apa = defaultFactory.instructions().applyActions(actions);

        List<OFInstruction> inst = new ArrayList<OFInstruction>();
        inst.add(apa);

        OFFlowAdd.Builder fab = defaultFactory.buildFlowAdd();
        fab.setTableId(TableId.of(10));
        fab.setXid(r_xid);
        fab.setMatch(mb.build());
        fab.setInstructions(inst);
        fab.setHardTimeout(1000);

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1024);

        OFFlowAdd request = fab.build();
        request.writeTo(buf);

        buf.setByte(70, 0x40); // DSCP_63 Value : 63(0x3f) -> 64(0x40)

        log.info("[Channel Agent] Send msg :" + request.toString() + "with invalid IP_DSCP value 63(0x3f) -> 64(0x40)");
        dummyController.sendRawMsg(buf);

        Thread.sleep(2000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.120 - Disabled Table Features Request (>=OF 1.3)
     */
    public String testDisabledTableFeatureRequest(String test) throws InterruptedException {

        log.info("[Channel Agent] " + test + " - Disabled Table Features Request");



        OFTableFeaturesStatsRequest.Builder table = defaultFactory.buildTableFeaturesStatsRequest();
        table.setXid(r_xid);
        OFMessage request = table.build();

        dummyController.sendMsg(request, -1);

        Thread.sleep(2000);

        ArrayList<OFTableFeaturePropType> receivedList = new ArrayList();

        OFTableFeaturesStatsReply response = (OFTableFeaturesStatsReply) dummyController.getResponse();
        OFTableFeatures ofTableFeatures = response.getEntries().get(0);
        for (OFTableFeatureProp oFTableFeatureProp : ofTableFeatures.getProperties()) {
            int type = oFTableFeatureProp.getType();
            OFTableFeaturePropType ofTableFeaturePropType = OFTableFeaturePropType.values()[type];
            receivedList.add(ofTableFeaturePropType);
        }

        OFTableFeaturePropType seletedType = null;
        List<OFTableFeaturePropType> originalList = Arrays.asList(OFTableFeaturePropType.values());
        for (OFTableFeaturePropType type : originalList) {
            if (!receivedList.contains(type)) {
                seletedType = type;
                break;
            }
        }

        log.info("[Channel Agent] Send msg :" + request.toString());

        String result = "";
        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.130 - Handshake without Hello Message
     */
    public String testHandshakeWithoutHello(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Handshake without Hello Message");

        runDummyController(TestSwitchCase.NO_HANDSHAKE);
        log.info("[Channel Agent] HELLO msg is not exchanged within the specified default timeout");

        Thread.sleep(DEFAULT_TIMEOUT);

        String result = "";

        if (!dummyController.getHandshaked()) {
            result = "Handshake is NOT completed";
        } else {
            result = "Handshake is completed";
        }

        return result;
    }

    /*
     * 1.1.140 - Control Message before Hello Message
     */
    public String testControlMsgBeforeHello(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Control Message before Hello Message");

        runDummyController(TestSwitchCase.NO_HANDSHAKE);

        OFBarrierRequest.Builder brb = defaultFactory.buildBarrierRequest();
        brb.setXid(r_xid);

        OFMessage request = brb.build();
        dummyController.sendMsg(request, -1);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();
        log.info("[Channel Agent] Send msg :" + request.toString());

        String result = "";
        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.150 - Incompatible Hello after Connection Establishment
     */
    public String testIncompatibleHelloAfterConnection(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Incompatible Hello after Connection Establishment");

        OFHelloFailedErrorMsg.Builder hfb = defaultFactory.errorMsgs().buildHelloFailedErrorMsg();
        hfb.setXid(r_xid);
        hfb.setCode(OFHelloFailedCode.INCOMPATIBLE);

        OFMessage request = hfb.build();
        dummyController.sendMsg(request, -1);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();
        log.info("[Channel Agent] Send msg :" + request.toString());

        String result = "";
        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.160 - Corrupted Cookie Values
     */
    public String testCorruptedCookieValue(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Corrupted Cookie Values");

        String result = "";
        int inport = 1;
        int outport = 2;

        Match.Builder mb = defaultFactory.buildMatch();
        mb.setExact(MatchField.IN_PORT, OFPort.ofInt(inport));

        OFActionOutput.Builder aob = defaultFactory.actions().buildOutput();
        aob.setMaxLen(Integer.MAX_VALUE);
        aob.setPort(OFPort.ofInt(outport));

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(aob.build());

        OFFlowAdd.Builder fab = defaultFactory.buildFlowAdd();
        fab.setXid(r_xid);
        fab.setMatch(mb.build());
        fab.setActions(actions);
        fab.setHardTimeout(1000);

        fab.setCookie(U64.of(0xFFFFFFFFFFFFFFFFl));

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1024);

        OFFlowAdd request = fab.build();
        request.writeTo(buf);

        log.info("[Channel Agent] Send msg :" + request.toString() + " with corrupted cookie value");
        dummyController.sendRawMsg(buf);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }

    /*
     * 1.1.170 - Malformed Buffer ID Values
     */
    public String testMalformedBufferIDValue(String test) throws InterruptedException {
        log.info("[Channel Agent] " + test + " - Malformed Buffer ID Values");

        String result = "";
        int inport = 1;
        int outport = 2;

        Match.Builder mb = defaultFactory.buildMatch();
        mb.setExact(MatchField.IN_PORT, OFPort.ofInt(inport));

        OFActionOutput.Builder aob = defaultFactory.actions().buildOutput();
        aob.setMaxLen(Integer.MAX_VALUE);
        aob.setPort(OFPort.ofInt(outport));

        List<OFAction> actions = new ArrayList<OFAction>();
        actions.add(aob.build());

        OFFlowAdd.Builder fab = defaultFactory.buildFlowAdd();
        fab.setXid(r_xid);
        fab.setMatch(mb.build());
        fab.setActions(actions);
        fab.setHardTimeout(1000);

        Random ran = new Random();

        fab.setBufferId(OFBufferId.of(ran.nextInt(0xffff) + 1));

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(1024);

        OFFlowAdd request = fab.build();
        request.writeTo(buf);

        log.info("Send msg :" + request.toString() + " with malformed buffer ID values");
        dummyController.sendRawMsg(buf);

        Thread.sleep(1000);

        OFMessage response = dummyController.getResponse();

        result += request.toString();
        if (response != null) {
            result += "\n" + response.toString();
        } else {
            result += "\nnull";
        }

        return result;
    }
}