package org.deltaproject.manager.handler;

import org.deltaproject.manager.utils.AgentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class OpenDaylightHandler implements ControllerHandler {

    public static final String HYDROGEN = "";
    private static final Logger log = LoggerFactory.getLogger(OpenDaylightHandler.class.getName());

    private Process proc = null;
    private boolean isRunning = false;

    public String version = "";
    public String appPath = "";
    public String sshAddr = "";

    private int currentPID = -1;

    private BufferedWriter stdIn;
    private BufferedReader stdOut;

    private Thread loggerThd;

    private String controllerPath;
    private String user;

    private int attackCount = 1;
    private int indexController = -1;
    private int indexApp = -1;

    public OpenDaylightHandler(String path, String v, String ssh) {
        this.version = v;
        this.sshAddr = ssh;
    }

    public OpenDaylightHandler setAppAgentPath(String path) {
        this.appPath = path;

        return this;
    }

    public boolean createController() {
        isRunning = false;
        String str;
        String[] cmdArray = null;

        try {
            if (version.equals("helium")) {
                user = sshAddr.substring(0, sshAddr.indexOf('@'));
//                controllerPath = "/home/" + user + "/odl-helium-sr3/opendaylight/distribution/opendaylight/handler/distribution.opendaylight-osgipackage/opendaylight/run.sh -Xmx2g";
            }
//                cmdArray = new String[]{"ssh", sshAddr, "sudo", controllerPath};
//
//            } else if (version.equals("carbon")) {
//                user = sshAddr.substring(0, sshAddr.indexOf('@'));
//                controllerPath = "/home/" + user + "/distribution-karaf-0.6.0-Carbon/bin/karaf";
//                cmdArray = new String[]{"ssh", sshAddr, controllerPath};
//            }
            cmdArray = new String[]{System.getenv("DELTA_ROOT") + "/tools/dev/app-agent-setup/opendaylight/delta-run-odl", version};

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.redirectErrorStream(true);
            proc = pb.start();
            loggerThd = new Thread(AgentLogger.getLoggerThreadInstance(proc, AgentLogger.APP_AGENT));
            loggerThd.start();

            stdIn = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));

            if (version.equals("helium")) {
                do {
//                    str = AgentLogger.readLogFile(AgentLogger.APP_AGENT);
                    str = AgentLogger.getTemp();
                    Thread.sleep(1000);
                }
                while (!str.contains("initialized successfully"));
            } else if (version.equals("carbon")) {
                do {
//                    str = AgentLogger.readLogFile(AgentLogger.APP_AGENT);
                    str = AgentLogger.getTemp();
                }
                while (!str.contains("shutdown OpenDaylight"));
            }

            log.info("OpenDaylight is activated");

            installAppAgent();

            AgentLogger.setTemp("");

        } catch (Exception e) {
            log.error(e.toString());
        }

        return true;
    }

    public boolean installAppAgent() {
        boolean isInstalled = false;
        int bundleID = 0;

        String successMsg = "";

        if (version.equals("helium")) {
            successMsg = "Installed";
        } else if (version.equals("carbon")) {
            return true;
        }

        try {
            stdIn.write("install file:" + "/home/" + user + "/delta-agent-app-odl-" + version + "-1.0-SNAPSHOT.jar" + "\n");
            stdIn.flush();

            while (!isInstalled) {

//                String line = stdOut.readLine();
                String line = AgentLogger.getTemp();
                if (line.contains(successMsg)) {
                    isInstalled = true;

                    int idx = line.indexOf(successMsg);
                    if (version.equals("helium")) {
                        bundleID = Integer.parseInt(line.substring(idx - 4, idx - 1));
                    } else if (version.equals("carbon")) {
                        bundleID = Integer.parseInt(line.substring(idx + successMsg.length()).replace("\n", ""));
                    }

                    stdIn.write("start " + bundleID + "\n");
                    stdIn.flush();

                    log.info("AppAgent bundle ID [" + bundleID + "] Installed");
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

//            if (version.equals("helium")) {
//                // for service chain interference
//                stdIn.write("install file:" + "/home/" + user + "/delta-agent-app-odl-" + version + "-sub-1.0-SNAPSHOT.jar" + "\n");
//                stdIn.flush();
//
//                isInstalled = false;
//                while (!isInstalled) {
//                    stdOut = AgentLogger.readLogFile(AgentLogger.APP_AGENT);
//                    if (stdOut.contains(successMsg)) {
//                        isInstalled = true;
//
//                        int idx = stdOut.indexOf(successMsg);
//                        this.bundleID = Integer.parseInt(stdOut.substring(idx - 4,
//                                idx - 1));
//
//                        stdIn.write("start " + bundleID + "\n");
//                        stdIn.flush();
//                    }
//                }
//            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        log.info("OpenDaylight app-agent is installed");

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

    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return "OpenDaylight";
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return this.version;
    }

    @Override
    public String getPath() {
        // TODO Auto-generated method stub
        return this.controllerPath + "\n" + this.appPath;
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