package org.deltaproject.manager.testcase;

import org.apache.commons.lang3.StringUtils;
import org.deltaproject.manager.core.*;
import org.deltaproject.webui.TestCase;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Random;

import static org.deltaproject.webui.TestCase.TestResult.*;

/**
 * Created by seungsoo on 9/3/16.
 */
public class TestControllerCase {
    private static final Logger log = LoggerFactory.getLogger(TestControllerCase.class);

    public static final int VALID_HANDSHAKE = 0;
    public static final int HANDSHAKE_NO_HELLO = 1;
    public static final int HANDSHAKE_INCOMPATIBLE_HELLO = 2;

    public static final int DEFAULT_TIMEOUT = 5000;

    private Configuration cfg = Configuration.getInstance();

    private ChannelAgentManager chm;
    private ControllerManager cm;
    private HostAgentManager hm;
    private AppAgentManager am;

    private Process proc;
    private int procPID;

    public TestControllerCase(AppAgentManager am, HostAgentManager hm, ChannelAgentManager cm, ControllerManager ctm) {
        this.chm = cm;
        this.cm = ctm;
        this.hm = hm;
        this.am = am;
    }

    public void runRemoteAgents() {
        chm.runAgent();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopRemoteAgents() {
        chm.stopAgent();
    }

    public void replayKnownAttack(TestCase test) throws InterruptedException {
        runRemoteAgents();

        switch (test.getcasenum()) {
            case "2.1.010":
                testMalformedVersionNumber(test);
                break;
            case "2.1.020":
                testCorruptedControlMsgType(test);
                break;
            case "2.1.030":
                testHandShakeWithoutHello(test);
                break;
            case "2.1.040":
                testControlMsgBeforeHello(test);
                break;
            case "2.1.050":
                testMultipleMainConnectionReq(test);
                break;
            case "2.1.060":
                testUnFlaggedFlowRemoveMsgNotification(test);
                break;
            case "2.1.070":
                testTLSupport(test);
                break;
        }

        stopRemoteAgents();
    }

    public void initController() {
        log.info("Target controller: " + cm.getType() + " " + cm.getVersion());
        log.info("Target controller is starting..");
        cm.createController();

        while (!cm.isListeningSwitch()) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        log.info("Listening to switches..");
    }

    public void isSWconnected() {
        /* waiting for switches */
        cm.isConnectedSwitch(true);
        log.info("All switches are connected");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
    * 2.1.010 - Malformed Version Number
    * Verify that the controller throws an error when it receives a malformed
    * version number after establishing connection between switch and
    * controller with a different version.
    */
    public void testMalformedVersionNumber(TestCase test) {
//        String info = test.getcasenum() + " - Malformed Version Number - Test for controller protection against communication with mismatched OpenFlow versions";
//        log.info(info);

        initController();

        log.info("Dummy switch starts");
        chm.write("startsw");

        if (chm.read().contains("switchok"))
            chm.write(test.getcasenum());

        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info(split[0]);
        log.info(split[1]);

        if (split[1].contains("PASS"))
            test.setResult(PASS);
        else
            test.setResult(FAIL);

        cm.killController();
    }

    /*
    * 2.1.020 - Corrupted Control Message Type
    * Verify that the controller throws an error when it receives a control message
    * with unsupported message type.
    */
    public void testCorruptedControlMsgType(TestCase test) {
        String info = test.getcasenum() + " - Corrupted Control Message Type - Test for controller protection against control messages with corrupted content";
        log.info(info);

        initController();

        log.info("Dummy switch starts");
        chm.write("startsw");

        if (chm.read().contains("switchok"))
            chm.write(test.getcasenum());

        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info(split[0]);
        log.info(split[1]);

        if (split[1].contains("PASS"))
            test.setResult(PASS);
        else
            test.setResult(FAIL);

        cm.killController();
    }


    /*
     * 2.1.030 - Handshake without Hello Message
     * Check if the control connection is disconnected if the hello message is
     * not exchanged within the specified default timeout.
     */
    public void testHandShakeWithoutHello(TestCase test) {
        String info = test.getcasenum() + " - Handshake without Hello Message - Test for controller protection against incomplete control connection left open";

        log.info(info);

        initController();
        chm.write("2.1.030");

        log.info("Dummy switch dosen't send hello message");

        try {
            Thread.sleep(DEFAULT_TIMEOUT);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.info("Check switch connections");
        String result = chm.read();
        int cnt = cm.isConnectedSwitch(false);

        if (result.contains("switchNotConnected")) {
            test.setResult(PASS);
            log.info("Switch not connected, PASS");
        } else {
            test.setResult(FAIL);
            log.info("Switch connected, FAIL");
        }

        cm.killController();
    }

    /*
     * 2.1.040 - Control Message before Hello Message
     * In the main connection between switch and controller, check if the controller
     * processes a control message before exchanging OpenFlow hello message
     * (connection establishment).
     */
    public void testControlMsgBeforeHello(TestCase test) {
        String info = test.getcasenum() + " - Control Message before Hello Message - Test for controller protection against control communication prior to completed connection establishment";
        log.info(info);

        initController();

        log.info("Dummy switch starts");
        chm.write("startsw|nohello");

        if (chm.read().contains("switchok")) {
            try {
                Thread.sleep(DEFAULT_TIMEOUT);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            chm.write(test.getcasenum());
        }

        String response = chm.read();

        String[] split = StringUtils.split(response, "\n");
        log.info(split[0]);
        log.info(split[1]);

        if (split[1].contains("PASS"))
            test.setResult(PASS);
        else
            test.setResult(FAIL);

        cm.killController();
    }

    /*
     * 2.1.050 - Multiple main connection requests from same switch
     * Check if the controller accepts multiple main connections from the same switch.
     */
    public void testMultipleMainConnectionReq(TestCase test) {
        String info = test.getcasenum() + " - Multiple main connection requests from same switch - Test for controller protection against multiple control requests";
        log.info(info);

        initController();

        log.info("Dummy switch starts");
        chm.write("startsw");

        if (chm.read().contains("switchok"))
            chm.write(test.getcasenum());

        String response = chm.read();
        log.info(response);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.info("Check switch connections");
        int cnt = cm.isConnectedSwitch(false);

        if (cnt == 1) {
            test.setResult(PASS);
            log.info("Reject other same switches, PASS");
        } else {
            test.setResult(FAIL);
            log.info("Accept other same switches, FAIL");
        }

        chm.write("stoptemp");
        cm.killController();
    }

    /*
     * 2.1.060 - Un-flagged Flow Remove Message Notification
     * Check if the controller accepts a flow remove message notification without requesting the delete event.
     */
    public void testUnFlaggedFlowRemoveMsgNotification(TestCase test) throws InterruptedException {
        String info = test.getcasenum() + " - Un-flagged Flow Remove Message Notification - Test for controller protection against unacknowledged manipulation of the network";
        log.info(info);

        initController();

        log.info("Dummy switch starts");
        chm.write("startsw");

        if (!chm.read().contains("switchok"))
            return;

        Thread.sleep(5000);
        am.write(test.getcasenum());
        log.info("App-agent sends msg " + am.read() + " with un-flagged removed");

        Thread.sleep(2000);
        chm.write(test.getcasenum());

        String response = chm.read();

        if (response.equals("nothing")) {
            test.setResult(FAIL);
            cm.killController();

            return;
        }

        String[] split = StringUtils.split(response, "\n");
        log.info(split[0]);
        log.info(split[1]);

        if (split[1].contains("PASS"))
            test.setResult(PASS);
        else
            test.setResult(FAIL);

        cm.killController();
    }

    /*
     * 2.1.070 - Test TLS Support
     * Check if the controller supports Transport Layer Security (TLS).
     */
    public void testTLSupport(TestCase test) {
        String info = test.getcasenum() + " - Test TLS Support - Test for controller support for Transport Layer Security";
        log.info(info);

        initController();

        chm.write(test.getcasenum());
        log.info("Dummy Switch initiates a connection using TLS");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.info("Check the switch connection");
        int cnt = cm.isConnectedSwitch(false);

        if (cnt == 0) {
            test.setResult(FAIL);
            log.info("Switch disconnected, FAIL");
        } else {
            test.setResult(PASS);
            log.info("Switch connected, PASS");
        }

        chm.write(test.getcasenum() + ":exit");
        cm.killController();
    }
}
