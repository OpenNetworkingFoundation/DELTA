package org.deltaproject.manager.core;

import org.deltaproject.manager.fuzzing.TestFuzzing;
import org.deltaproject.manager.fuzzing.TestStateDiagram;
import org.deltaproject.manager.testcase.TestAdvancedCase;
import org.deltaproject.manager.testcase.TestControllerCase;
import org.deltaproject.manager.testcase.CaseInfo;
import org.deltaproject.manager.testcase.TestSwitchCase;
import org.deltaproject.webui.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

public class AttackConductor {
    private static final Logger log = LoggerFactory.getLogger(AttackConductor.class);

    private HashMap<String, String> infoSwitchCase;
    private HashMap<String, String> infoControllerCase;
    private HashMap<String, String> infoAdvancedCase;

    private AppAgentManager appm;
    private HostAgentManager hostm;
    private ChannelAgentManager channelm;
    private ControllerManager controllerm;

    private Configuration cfg = Configuration.getInstance();

    private DataOutputStream dos;
    private DataInputStream dis;

    private TestAdvancedCase testAdvancedCase;
    private TestSwitchCase testSwitchCase;
    private TestControllerCase testControllerCase;

    private TestFuzzing testFuzzing;
    private TestStateDiagram testState;

    private String agentType;

    public AttackConductor(String config) {
        cfg.initialize(config);

        infoControllerCase = new HashMap<String, String>();
        infoSwitchCase = new HashMap<String, String>();
        infoAdvancedCase = new HashMap<String, String>();

        this.controllerm = new ControllerManager();
        this.appm = new AppAgentManager();
        this.hostm = new HostAgentManager();
        this.channelm = new ChannelAgentManager();

		/* Update Test Cases */
        CaseInfo.updateAdvancedCase(infoAdvancedCase);
        CaseInfo.updateControllerCase(infoControllerCase);
        CaseInfo.updateSwitchCase(infoSwitchCase);

        testSwitchCase = new TestSwitchCase(channelm);
        testControllerCase = new TestControllerCase(appm, hostm, channelm, controllerm);
        testAdvancedCase = new TestAdvancedCase(appm, hostm, channelm, controllerm);

        testFuzzing = new TestFuzzing(appm, hostm, channelm, controllerm);
        testState = new TestStateDiagram(appm);

    }

    public void refreshConfig(Configuration cfg) {
        controllerm.setConfig(cfg);
        testSwitchCase.setConfig(cfg);
    }

    public String showConfig() {
        return cfg.show();
    }

    public void setSocket(Socket socket) throws Exception {
        dos = new DataOutputStream(socket.getOutputStream());
        dis = new DataInputStream(socket.getInputStream());

        agentType = dis.readUTF();

        if (agentType.contains("AppAgent")) {
            appm.setAppSocket(socket, dos, dis);
            dos.writeUTF("OK");
            dos.flush();
            log.info("App agent connected");
        } else if (agentType.contains("ActAgent")) {        /* for OpenDaylightHandler */
            appm.setActSocket(socket, dos, dis);
        } else if (agentType.contains("ChannelAgent")) {
            channelm.setSocket(socket, dos, dis);
            dos.writeUTF("OK");
            dos.flush();

			/* send configuration to channel agent */
            String config = "config," + "version:" + cfg.getOF_VERSION() + ",nic:" + cfg.getMITM_NIC() + ",port:"
                    + cfg.getOF_PORT() + ",controller_ip:" + cfg.getCONTROLLER_IP() + ",switch_ip:" + cfg.getSwitchList().get(0)
                    + ",handler:dummy" + ",cbench:" + cfg.getCBENCH_ROOT();

            channelm.write(config);
            log.info("Channel agent connected");
        } else if (agentType.contains("HostAgent")) {
            hostm.setSocket(socket, dos, dis);
            hostm.write("target:" + cfg.getTARGET_HOST());
            log.info("Host agent connected");
        }
    }

    public void executeTestCase(TestCase test) throws InterruptedException {
        long start = System.currentTimeMillis();
        if (test.getcasenum().charAt(0) == '1') {
            testSwitchCase.replayKnownAttack(test);
        } else if (test.getcasenum().charAt(0) == '2') {
            testControllerCase.replayKnownAttack(test);
        } else if (test.getcasenum().charAt(0) == '3') {
            testAdvancedCase.replayKnownAttack(test);
        } else if (test.getcasenum().charAt(0) == '0') {
            testFuzzing.testFuzzing(test);
        }
        long end = System.currentTimeMillis();
        log.info("Running Time(s) : " + (end - start) / 1000.0);
        log.info(test.getName() + " is done\n==============================================================================\n");
    }

    public void printAttackList() {
        System.out.println("\nData Plane Test Set");
        TreeMap<String, String> treeMap = new TreeMap<String, String>(infoSwitchCase);
        Iterator<String> treeMapIter = treeMap.keySet().iterator();
        while (treeMapIter.hasNext()) {

            String key = (String) treeMapIter.next();
            String value = (String) treeMap.get(key);
            System.out.println(String.format("%s\t: %s", key, value));
        }

        System.out.println("\nControl Plane Test Set (under developing)");
        treeMap = new TreeMap<String, String>(infoControllerCase);
        treeMapIter = treeMap.keySet().iterator();
        while (treeMapIter.hasNext()) {
            String key = (String) treeMapIter.next();
            String value = (String) treeMap.get(key);
            System.out.println(String.format("%s\t: %s", key, value));
        }

        System.out.println("\nAdvanced Test Set");
        treeMap = new TreeMap<String, String>(infoAdvancedCase);
        treeMapIter = treeMap.keySet().iterator();
        while (treeMapIter.hasNext()) {

            String key = (String) treeMapIter.next();
            String value = (String) treeMap.get(key);
            System.out.println(String.format("%s\t: %s", key, value));
        }
    }

    public boolean isPossibleAttack(String code) {
        if (infoControllerCase.containsKey(code))
            return true;
        else if (infoSwitchCase.containsKey(code))
            return true;
        else if (infoAdvancedCase.containsKey(code))
            return true;
        else if (code.charAt(0) == '0')
            return true;
        else
            return false;
    }

    public void setTestSwitchCase(TestSwitchCase testSwitchCase) {
        this.testSwitchCase = testSwitchCase;
    }

    public HostAgentManager getHostm() {
        return hostm;
    }

    public ChannelAgentManager getChannelm() {
        return channelm;
    }

    public ControllerManager getControllerm() {
        return controllerm;
    }
}
