package org.deltaproject.manager.analysis;

import org.deltaproject.manager.core.AppAgentManager;
import org.deltaproject.manager.core.ControllerManager;
import org.deltaproject.manager.testcase.TestAdvancedCase;
import org.deltaproject.webui.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.StringTokenizer;

import static org.deltaproject.webui.TestCase.TestResult.*;

public class ResultAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(ResultAnalyzer.class);
    public static final int LATENCY_TIME = 0;
    public static final int COMMUNICATON = 1;
    public static final int CONTROLLER_STATE = 2;
    public static final int SWITCH_STATE = 3;
    public static final int APPAGENT_REPLY = 4;

    private ControllerManager controllerm;
    private AppAgentManager appm;

    private String fuzzingResult = "";

    public ResultAnalyzer(ControllerManager in, AppAgentManager in2) {
        controllerm = in;
        appm = in2;
    }

    public boolean checkAll(String code, String in) {
        return true;
    }

    public String getFuzzingLog() {
        return this.fuzzingResult;
    }

    public boolean checkUnknown(ArrayList<ResultInfo> results) {
        boolean isSuccess = false;

        if (this.checkControllerState()) {
            isSuccess = true;
            fuzzingResult += "Controller Shutdown\n";
        }

        if (this.checkSwitchState()) {
            isSuccess = true;
            fuzzingResult += "Switch Shutdown\n";
        }

        if (this.checkSwitchState()) {
            isSuccess = true;
            fuzzingResult += "Switch Shutdown\n";
        }

        if (isSuccess) {
            log.info("Success");
        }

        return true;
    }

    public boolean checkResult(TestCase test, ResultInfo result) {
        ArrayList<Integer> types = result.types();
        boolean isSuccess = false;

        for (int i = 0; i < types.size(); i++) {
            switch (types.get(i)) {
                case ResultInfo.CONTROLLER_STATE:
                    isSuccess = checkControllerState();
                    break;

                case ResultInfo.SWITCH_STATE:
                    isSuccess = checkSwitchState();
                    break;

                case ResultInfo.LATENCY_TIME:
                    isSuccess = checkLatency(result.getBeforeL(), result.getAfterL());
                    break;

                case ResultInfo.COMMUNICATON:
                    boolean temp1 = false;
                    boolean temp2 = false;

                    if (result.getBeforeL() != null)
                        temp1 = checkCommunication(result.getBeforeL());

                    if (result.getAfterL() != null)
                        temp2 = checkCommunication(result.getAfterL());

                    isSuccess = temp2;
                    break;

                case ResultInfo.APPAGENT_REPLY:
                    isSuccess = checkAppAgentResponse(result.getResult());
                    break;

                case ResultInfo.CHANNELAGENT_REPLY:
                    isSuccess = checkTopoInfo(result.getResult());
                    break;
            }
        }

        if (!isSuccess) {
            test.setResult(PASS);
            log.info(test.getcasenum() + ", PASS");
            return true;
        } else {
            test.setResult(FAIL);
            log.info(test.getcasenum() + ", FAIL");
            return false;
        }
    }

    public boolean checkCommunication(String in) {
        if (in.contains("Unreachable")) {
            log.info("Ping response host unreachable");
        } else if (in.contains("100%")) {
            log.info("100% Packet loss");
        } else {
            return false;
        }

        return true;
    }

    public boolean checkLatency(String before, String after) {
        if (before.contains("100%") || after.contains("100%")) {
            return true;
        }

        int idx = before.indexOf("min");
        before = before.substring(idx);

        StringTokenizer str = new StringTokenizer(before, "/");
        for (int i = 0; i < 4; i++)
            str.nextToken();
        before = str.nextToken();

        idx = after.indexOf("min");
        after = after.substring(idx);

        str = new StringTokenizer(after, "/");
        for (int i = 0; i < 4; i++)
            str.nextToken();
        after = str.nextToken();

        int beforeInt = (int) (Double.parseDouble(before) * 1000);
        int afterInt = (int) (Double.parseDouble(after) * 1000);

        log.info("Latency (ms) before: " + before + " < After: " + after);
        log.info("Threshold (ms) : " + Double.parseDouble(before) * 2);

        if ((beforeInt * 2) < afterInt) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkAppAgentResponse(String in) {
        // System.out.println("from: " + in);
        if (in.contains("success")) {
            in = in.substring(in.indexOf("|") + 1);
            log.info(in);
        } else {
            return false;
        }

        return true;
    }

    public boolean checkControllerState() {
        if (appm.write("echo")) {
            String res = appm.read();
            if (res.equals("echo")) {
                log.info("Controller NOT shutdown, recv echo");
                return false;
            }
        }

        log.info("Controller shutdown, no recv echo");
        /*
        if (!controllerm.isRunning()) {
            log.info("Controller shutdown");
        } else {
            log.info("Controller NOT shutdown");
            return false;
        }
        */

        return true;
    }

    public boolean checkTopoInfo(String in) {
        if (in.contains("success")) {
            in = in.substring(in.indexOf("|") + 1);
            log.info(in);
        } else {
            log.info(in);
            return false;
        }

        return true;
    }

    public boolean checkSwitchState() {
        Process temp = null;
        String tempS = "";
        int switchCnt = controllerm.getSwitchCounter();
        int cnt = controllerm.isConnectedSwitch(false);

        if (switchCnt != cnt)
            log.info("Switch diconnected");
        else {
            log.info("Switches NOT disconnected");
            return false;
        }

        return true;
    }
}