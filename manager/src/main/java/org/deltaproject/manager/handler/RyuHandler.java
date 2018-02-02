package org.deltaproject.manager.handler;

import org.apache.commons.lang3.StringUtils;
import org.deltaproject.manager.utils.AgentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;

public class RyuHandler implements ControllerHandler {
    private Process proc = null;
    private static final Logger log = LoggerFactory.getLogger(RyuHandler.class.getName());
    private boolean isRunning = false;

    public String version = "";
    public String ryu = "";
    public String sshAddr = "";
    public String of_version = "";

    private int currentPID = -1;

    private BufferedWriter stdIn;
    private BufferedReader stdOut;

    private Thread loggerThd;

    public RyuHandler(String path, String v, String ssh, String of_version) {
        this.version = v;
        this.sshAddr = ssh;
        this.of_version = of_version;
    }

    @Override
    public boolean createController() {
        isRunning = false;

        String str = "";

        String[] cmdArray = null;

        // TODO: Simple_Switch app needs to be loaded according to the OpenFlow Version.
        try {
            if (this.version.contains("4.16")) {
                if (this.of_version.contains("1.0"))
                    cmdArray = new String[]{System.getenv("DELTA_ROOT") + "/tools/dev/app-agent-setup/ryu/delta-run-ryu", "1.0"};
                else if (this.of_version.contains("1.3"))
                    cmdArray = new String[]{System.getenv("DELTA_ROOT") + "/tools/dev/app-agent-setup/ryu/delta-run-ryu", "1.3"};
                else
                    return false;
            } else {
                return false;
            }

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.redirectErrorStream(true);
            proc = pb.start();
            loggerThd = new Thread(AgentLogger.getLoggerThreadInstance(proc, AgentLogger.APP_AGENT));
            loggerThd.start();

            Field pidField = Class.forName("java.lang.UNIXProcess").getDeclaredField("pid");
            pidField.setAccessible(true);
            Object value = pidField.get(proc);
            this.currentPID = (Integer) value;

            Thread.sleep(5000);

            // TODO: This part will add to check whether Ryu application is complete.

            isRunning = true;
            log.info("Ryu is activated");

            Process temp = Runtime.getRuntime().exec("ssh " + sshAddr + " sudo ps -ef | grep ryu-manager");
            String tempS;

            BufferedReader stdOut2 = new BufferedReader(new InputStreamReader(temp.getInputStream()));

            while ((tempS = stdOut2.readLine()) != null && !tempS.isEmpty()) {
                if (tempS.contains("ryu-manager")) {
                    String[] list = StringUtils.split(tempS);

                    currentPID = Integer.parseInt(list[1]);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        AgentLogger.setTemp("");

        return true;
    }

    @Override
    public Process getProc() {
        return this.proc;
    }

    @Override
    public void killController() {
        Process pc = null;
        try {
            pc = Runtime.getRuntime().exec("ssh " + sshAddr + " sudo killall ryu-manager");
            pc.getErrorStream().close();
            pc.getInputStream().close();
            pc.getOutputStream().close();
            pc.waitFor();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        this.currentPID = -1;
    }

    @Override
    public boolean installAppAgent() {
        return true;
    }

    @Override
    public String getType() {
        return "Ryu";
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public String getPath() {
        return this.ryu;
    }

    @Override
    public int getPID() {
        return this.currentPID;
    }

    @Override
    public BufferedReader getStdOut() {
        return this.stdOut;
    }
}
