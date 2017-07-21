package org.deltaproject.manager.testcase;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.apache.commons.lang3.StringUtils;
import org.deltaproject.manager.core.ChannelAgentManager;
import org.deltaproject.manager.core.Configuration;
import org.deltaproject.webui.TestCase;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.errormsg.OFHelloFailedErrorMsg;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;

import static org.deltaproject.webui.TestCase.TestResult.*;

public class TestSwitchCase {
    private static final Logger log = LoggerFactory.getLogger(TestSwitchCase.class);

    public static final int HANDSHAKE_DEFAULT = 0;
    public static final int HANDSHAKE_NO_HELLO = 1;
    public static final int HANDSHAKE_INCOMPATIBLE_HELLO = 2;
    public static final int NO_HANDSHAKE = 3;
    public static final int DEFAULT_TIMEOUT = 5000;

    private Configuration cfg;

    private OFFactory defaultFactory;
    private Random random;

    private long r_xid = 0xeeeeeeeel;

    private String ofversion;
    private int ofport;

    private Process proc;
    private int procPID;

    private ChannelAgentManager chm;

    public TestSwitchCase(ChannelAgentManager cm) {
        random = new Random();
        chm = cm;
    }

    public void setConfig(Configuration cfg) {
        this.cfg = cfg;
        ofversion = cfg.getOF_VERSION();
        if (ofversion.equals("1.0"))
            defaultFactory = OFFactories.getFactory(OFVersion.OF_10);
        else if (ofversion.equals("1.3"))
            defaultFactory = OFFactories.getFactory(OFVersion.OF_13);

        ofport = Integer.parseInt(cfg.getOF_PORT());
    }

    public void runRemoteAgents() {
        log.info("Run channel agent");
        chm.runAgent();

        if (cfg.getTopologyType().equals("vm")) {
            log.info("Run test mininet topology");

            String mininet;

            if (ofversion.equals("1.0"))
                mininet = " sudo python test-switch-topo.py " + cfg.getAM_IP() + " " + cfg.getAM_PORT() + " OpenFlow10";
            else
                mininet = " sudo python test-switch-topo.py " + cfg.getAM_IP() + " " + cfg.getAM_PORT() + " OpenFlow13";

            try {
                proc = Runtime.getRuntime().exec("ssh " + cfg.getHOST_SSH() + mininet);

                Field pidField = Class.forName("java.lang.UNIXProcess").getDeclaredField("pid");
                pidField.setAccessible(true);
                Object value = pidField.get(proc);
                this.procPID = (Integer) value;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopRemoteAgents() {
        chm.stopAgent();

        if (cfg.getTopologyType().equals("vm")) {
            if (procPID != -1)
                try {
                    proc = Runtime.getRuntime().exec("sudo kill -9 " + this.procPID);
                    proc.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            else
                procPID = -1;

            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void replayKnownAttack(TestCase test) throws InterruptedException {
        runRemoteAgents();

        switch (test.getcasenum()) {
            case "1.1.010":
                testPortRangeViolation(test);
                break;
            case "1.1.020":
                testTableID(test);
                break;
            case "1.1.030":
                testGroupID(test);
                break;
            case "1.1.040":
                testMeterID(test);
                break;
            case "1.1.050":
                testTableLoop(test);
                break;
            case "1.1.060":
                testCorruptedControlMsgType(test);
                break;
            case "1.1.070":
                testUnsupportedVersionNumber(test);
                break;
            case "1.1.080":
                testMalformedVersionNumber(test);
                break;
            case "1.1.090":
                testInvalidOXMType(test);
                break;
            case "1.1.100":
                testInvalidOXMLength(test);
                break;
            case "1.1.110":
                testInvalidOXMValue(test);
                break;
            case "1.1.120":
                testDisabledTableFeatureRequest(test);
                break;
            case "1.1.130":
                testHandshakeWithoutHello(test);
                break;
            case "1.1.140":
                testControlMsgBeforeHello(test);
                break;
            case "1.1.150":
                testIncompatibleHelloAfterConnection(test);
                break;
            case "1.1.160":
                testCorruptedCookieValue(test);
                break;
            case "1.1.170":
                testMalformedBufferIDValue(test);
                break;
        }

        stopRemoteAgents();
    }

    public boolean runDummyController() {
        log.info("Run dummy controller");
        chm.write("runDummyController");

        String response = chm.read();
        if (!response.contains("runDummyController")) {
            log.info("Run dummy controller fail!");
            return false;
        }
        return true;
    }

    /*
     * 1.1.010 - Port Range Violation
     * Verify that the switch rejects the use of ports that are greater than
     * OFPP_MAX and are not part of the reserved ports.
     */
    public void testPortRangeViolation(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Port Range Violation - Test switch protection against disallowed ports");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.020 - Table Identifier Violation (>=OF_1.3)
     * Verify that the switch rejects the use of invalid table id.
     */
    public void testTableID(TestCase test) throws InterruptedException {
        if (this.ofversion.equals("1.0")) {
            log.info("This test does not apply to OF 1.0 (Table ID not supported).");
            return;
        }

        log.info(test.getcasenum() + " - Table Identifier Violation - Test switch protection against invalid table ID");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.030 - Group Identifier Violation (>=OF 1.3)
     * Verify that the switch rejects the use of groups that are greater than
     * OFPG_MAX and are not part of the reserved groups.
     */
    public void testGroupID(TestCase test) throws InterruptedException {
        if (this.ofversion.equals("1.0")) {
            log.info("This test does not apply to OF 1.0 (Group ID not supported).");
            return;
        }

        log.info(test.getcasenum() + " - Group Identifier Violation - Test switch protection against disallowed group numbers");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.040 - Meter Identifier Violation (>=OF 1.3)
     * Verify that the switch rejects the use of meters that are greater than
     * OFPM_MAX and are not part of the virtual meters.
     */
    public void testMeterID(TestCase test) throws InterruptedException {
        if (this.ofversion.equals("1.0")) {
            log.info("This test does not apply to OF 1.0 (Meter ID not supportedd).");
            return;
        }

        log.info(test.getcasenum() + " - Meter Identifier Violation - Test switch protection against disallowed meter numbers");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.050 - Table Loop Violation (>=OF 1.3)
     * Verify that the switch rejects the use of invalid Goto table id
     * requesting a table loop.
     */
    public void testTableLoop(TestCase test) throws InterruptedException {
        if (this.ofversion.equals("1.0")) {
            log.info("This test does not apply to OF 1.0 (Multiple Tables not supportedd).");
            return;
        }

        log.info(test.getcasenum() + " - Table Loop Violation - Test switch protection against invalid GoToTable request");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.060 - Corrupted Control Message Type
     * Verify that the switch throws an error when it receives a control message
     * with unsupported message type.
     */
    public void testCorruptedControlMsgType(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Corrupted Control Message Type - Test switch protection against control message with unsupported type");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.070 - Unsupported Version Numeber
     * Verify that the switch throws an error when it receives a connection
     * setup message with an unsupported version number.
     */
    public void testUnsupportedVersionNumber(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Unsupported Version Numeber - Test switch protection against connection setup request with unsupported version number");

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.080 - Malformed Version Number
     * Verify that the switch throws an error when it receives a malformed
     * version number after establishing connection between switch and
     * controller with a different version.
     */
    public void testMalformedVersionNumber(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Malformed Version Number - Test switch protection against communication with mismatched OpenFlow versions");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.090 - Invalid OXM - Type (Command)
     * Verify that the switch throws an error when it receives a flow mod
     * message with invalid OXM type.
     */
    public void testInvalidOXMType(TestCase test) throws InterruptedException {
//        log.info(test.getcasenum() + " - Invalid OXM - Type - Test switch protection against flow mod with invalid message type");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.100 - Invalid OXM - Length (>=OF 1.3)
     * Verify that the switch throws an error when it receives a flow mod
     * message with invalid OXM length.
     */
    public void testInvalidOXMLength(TestCase test) throws InterruptedException {
        if (this.ofversion.equals("1.0")) {
            log.info("This test does not apply to OF 1.0 (not supported).");
            return;
        }

        log.info(test.getcasenum() + " - Invalid OXM - Length - Test switch protection against flow mod with invalid message length");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.110 - Invalid OXM - Value (>=OF 1.3)
     * Verify that the switch throws an error when it receives a flow mod
     * message with invalid message value
     */
    public void testInvalidOXMValue(TestCase test) throws InterruptedException {
        if (this.ofversion.equals("1.0")) {
            log.info("This test does not apply to OF 1.0 (not supported).");
            return;
        }

        log.info(test.getcasenum() + " - Invalid OXM - Value - Test switch protection against flow mod with invalid message value");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.120 - Disabled Table Features Request (>=OF 1.3)
     * If the switch has disabled the table feature request with non-empty body,
     * verify that the switch rejects this non-empty OFPMP_TABLE_FEATURES
     * request with a permission error
     */
    public void testDisabledTableFeatureRequest(TestCase test) throws InterruptedException {
        if (this.ofversion.equals("1.0")) {
            log.info("This test does not apply to OF 1.0 (Table Features not supported).");
            return;
        }

        log.info(test.getcasenum() + " - Disabled Table Features Request - Test for switch protection against table features request when feature is disabled");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.130 - Handshake without Hello Message
     * Check if the control connection is disconnected if the hello message is
     * not exchanged within the specified default timeout.
     */
    public void testHandshakeWithoutHello(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Handshake without Hello Message - Test for switch protection against incomplete control connection left open");

        chm.write(test.getcasenum());
        String response = chm.read();

        log.info("Channel agent NOT send HELLO msg");

        if (response.contains("NOT")) {
            test.setResult(PASS);
            log.info("Response : " + response + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response : " + response + " FAIL");
        }
    }

    /*
     * 1.1.140 - Control Message before Hello Message
     * In the main connection between switch and controller, check if the switch
     * processes a control message before exchanging OpenFlow hello message
     * (connection establishment).
     */
    public void testControlMsgBeforeHello(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Control Message before Hello Message - Test for switch protection against control communication prior to completed connection establishment");

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.150 - Incompatible Hello after Connection Establishment
     * Verify that the switch will properly handle the abnormal condition, when
     * it receives an OFPT_ERROR message with a type field of
     * OFPET_HELLO_FAILED, a code field of OFPHFC_INCOMPATIBLE after
     * establishing connection between switch and controller with a both agreed
     * version.
     */
    public void testIncompatibleHelloAfterConnection(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Incompatible Hello after Connection Establishment - Test for switch protection against abuse of the Hello_Failed error message");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.160 - Corrupted Cookie Values
     * Verify that the switch throws an error when it receives a corrupted
     * cookie value in OpenFlow messages after establishing connection between
     * switch and controller.
     */
    public void testCorruptedCookieValue(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Corrupted Cookie Values - Test for switch protection against replay attacks");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.1.170 - Malformed Buffer ID Values
     * Verify that the switch throws an error when it receives a malformed
     * buffer ID value after establishing connection between switch &
     * controller.
     */
    public void testMalformedBufferIDValue(TestCase test) throws InterruptedException {
        log.info(test.getcasenum() + " - Malformed Buffer ID Values - Test for switch protection against disallowed buffer ID values");

        if (!runDummyController())
            return;

        chm.write(test.getcasenum());
        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info("Channel agent send msg :" + split[0]);

        if (!split[1].contains("null")) {
            test.setResult(PASS);
            log.info("Response err msg: " + split[1] + ", PASS");
        } else {
            test.setResult(FAIL);
            log.info("Response is null, FAIL");
        }
    }

    /*
     * 1.2.010 - Slave Controller Violation (OF 1.3 ~)
     * Verify that the switch rejects unsupported control messages
     * (OFPT_PACKET_OUT, OFPT_FLOW_MOD, OFPT_GROUP_MOD, OFPT_PORT_MOD,
     * OFPT_TABLE_MOD requests, and OFPMP_TABLE_FEATURES) from slave
     * controllers.
     */
    public void testSlaveControllerViolation(TestCase test) {
        /*
        if (this.ofversion.equals("1.0")) {
            log.info("This test does not apply to OF 1.0 (Multiple Controllers not supported).");
            return;
        }
        log.info(test.getcasenum() + " - Slave Controller Violation - Test for switch protection against manipulation of the network by the slave controller");

        setUpDummyController(HANDSHAKE_DEFAULT);

        int inport = 1;
        int outport = 2;

        OFRoleRequest.Builder rrb = defaultFactory.buildRoleRequest();
        rrb.setRole(OFControllerRole.ROLE_NOCHANGE);
        rrb.setGenerationId(U64.ZERO);
        rrb.setXid(r_xid);

        OFRoleRequest request = rrb.build();
        dmcnt.sendMsg(request, -1);

        OFMessage response = dmcnt.getResponse();

        if (response != null) {
            log.info(response.toString());
        } else
            log.info("response is null");

        if (response != null) {
            OFRoleReply rr = (OFRoleReply) response;
            U64 gen = rr.getGenerationId();

            OFRoleRequest.Builder rrb2 = defaultFactory.buildRoleRequest();
            rrb2.setRole(OFControllerRole.ROLE_SLAVE);
            rrb2.setGenerationId(gen);
            rrb2.setXid(r_xid - 1);

            request = rrb2.build();
            dmcnt.sendMsg(request, -1);
        }

        OFPacketOut pktout = defaultFactory.buildPacketOut().setXid(r_xid).setBufferId(OFBufferId.NO_BUFFER).build();
        dmcnt.sendMsg(pktout, -1);
        response = dmcnt.getResponse();

        if (response != null) {
            log.info(response.toString());
        } else
            log.info("response is null");

        stopDummyController();
        */
    }

}
