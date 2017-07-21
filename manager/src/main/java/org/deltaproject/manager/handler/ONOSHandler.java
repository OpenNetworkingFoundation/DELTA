package org.deltaproject.manager.handler;

import org.apache.commons.lang3.StringUtils;
import org.deltaproject.manager.utils.AgentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;

public class ONOSHandler implements ControllerHandler {
    private Process proc = null;
    private static final Logger log = LoggerFactory.getLogger(ONOSHandler.class.getName());
    private boolean isRunning = false;

    public String version = "";
    public String onos1_9 = "";
    public String onos1_6 = "";
    public String onos1_1 = "";
    public String sshAddr = "";

    private int currentPID = -1;

    private BufferedWriter stdIn;
    private BufferedReader stdOut;

    private Thread loggerThd;

    public ONOSHandler(String path, String v, String ssh) {
        this.version = v;
        this.sshAddr = ssh;

        String user = ssh.substring(0, ssh.indexOf('@'));
        onos1_1 = "/home/" + user + "/Applications/apache-karaf-3.0.5/bin/karaf";
//        onos1_6 = "/home/" + user + "/onos-1.6.0/bin/onos-service";
//        onos1_9 = "/home/" + user + "/run-onos-delta";
    }

    public boolean createController() {
        isRunning = false;

        String str = "";

        String[] cmdArray = null;


        try {
            if (this.version.contains("1.1")) {
                cmdArray = new String[]{"ssh", sshAddr, onos1_1, "clean"};
            } else if (this.version.contains("1.6")) {
                cmdArray = new String[]{System.getenv("DELTA_ROOT") + "/tools/dev/app-agent-setup/onos/delta-run-onos", "1.6"};
            } else if (this.version.contains("1.9")) {
                cmdArray = new String[]{System.getenv("DELTA_ROOT") + "/tools/dev/app-agent-setup/onos/delta-run-onos", "1.9"};
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

            stdIn = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
//            stdOut = new BufferedReader(new InputStreamReader(proc.getInputStream()));

            Thread.sleep(10000);

            String line = null;
            do {
//                line = stdOut.readLine();
                line = AgentLogger.getTemp();
                Thread.sleep(500);
            }
            while (!line.contains("Welcome"));

            isRunning = true;
            log.info("ONOS is activated");

//            else {
//                log.info("Failed to start ONOSHandler");
//                return false;
//            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Process temp = Runtime.getRuntime().exec("ssh " + sshAddr + " sudo ps -ef | grep karaf");
            String tempS;

            BufferedReader stdOut2 = new BufferedReader(new InputStreamReader(temp.getInputStream()));

            while ((tempS = stdOut2.readLine()) != null && !tempS.isEmpty()) {
                if (tempS.contains("apache-karaf")) {
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

    public void killController() {
        Process pc = null;
        try {
            pc = Runtime.getRuntime().exec("ssh " + sshAddr + " sudo killall java");
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


    public Process getProc() {
        return this.proc;
    }

    /* ONOSHandler, AppAgent is automatically installed when the controller starts */
    public boolean installAppAgent() {

        return true;
    }

    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return "ONOS";
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return this.version;
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return this.onos1_1;
    }

    @Override
    public int getPID() {
        // TODO Auto-generated method stub
        return this.currentPID;
    }

    @Override
    public BufferedReader getStdOut() {
        // TODO Auto-generated method stub
        return this.stdOut;
    }
}
