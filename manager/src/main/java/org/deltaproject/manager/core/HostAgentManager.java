package org.deltaproject.manager.core;

import org.deltaproject.manager.utils.AgentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;

public class HostAgentManager extends Thread {
    private static final Logger log = LoggerFactory.getLogger(HostAgentManager.class);
    private Socket socket;

    private DataInputStream dis;
    private DataOutputStream dos;

    private Process proc;
    private int procPID;

    private Configuration cfg = Configuration.getInstance();

    Thread loggerThd;

    public HostAgentManager() {

    }

    void setSocket(Socket in, DataOutputStream w, DataInputStream r) {
        this.socket = in;
        this.dos = w;
        this.dis = r;
    }

    public Socket getSocket() {
        if (socket != null) {
            return this.socket;
        } else
            return null;
    }

    public void write(String input) {
        try {
            dos.writeUTF(input);
            dos.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String read() {
        try {
            return dis.readUTF();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "false";
    }

    public boolean runAgent(String topologyFile) {
        String version;

        if (cfg.getOF_VERSION().equals("1.0")) {
            version = "OpenFlow10";
        } else {
            version = "OpenFlow13";
        }

        try {

            String[] cmdArray = null;

            // in the case of all-in-one setting
            if (cfg.getTopologyType().equals("VM")) {
                cmdArray = new String[] {"ssh", cfg.getHOST_SSH(), "sudo", "python", topologyFile,
                        cfg.getCONTROLLER_IP(), cfg.getOF_PORT(), cfg.getAM_IP(), cfg.getAM_PORT(), version};

            // in the case of hardware setting
            } else if (cfg.getTopologyType().equals("HW")) {
                cmdArray = new String[] {"ssh", cfg.getHOST_SSH(), "java", "-jar", "delta-agent-host-1.0-SNAPSHOT.jar", cfg.getAM_IP(),
                        cfg.getAM_PORT()};
            }

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.redirectErrorStream(true);
            proc = pb.start();

            loggerThd = new Thread(AgentLogger.getLoggerThreadInstance(proc, AgentLogger.HOST_AGENT));
            loggerThd.start();

            Field pidField = Class.forName("java.lang.UNIXProcess").getDeclaredField("pid");
            pidField.setAccessible(true);
            Object value = pidField.get(proc);
            this.procPID = (Integer) value;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

//    public boolean runFuzzingTopo() {
//        String amAddr = cfg.getAMIP() + " " + cfg.getAMPort();
//        String controllerAddr = cfg.getControllerIP() + " " + cfg.getOFPort();
//        String version;
//
//        if (cfg.getOFVer().equals("1.0")) {
//            version = "OpenFlow10";
//        } else {
//            version = "OpenFlow13";
//        }
//
//        try {
//            proc = Runtime.getRuntime().exec("ssh " + cfg.getHostSSH() + " sudo python test-fuzzing-topo.py " + controllerAddr + " " + amAddr + " " + version);
//            Field pidField = Class.forName("java.lang.UNIXProcess").getDeclaredField("pid");
//            pidField.setAccessible(true);
//            Object value = pidField.get(proc);
//            this.procPID = (Integer) value;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return true;
//    }

    public void stopAgent() {
        try {
            if (dos != null) {
                dos.close();
                dos = null;
            }
            if (dis != null) {
                dis.close();
                dis = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (procPID != -1)
            try {
                Runtime.getRuntime().exec("ssh " + cfg.getHOST_SSH() + " sudo arp -d " + cfg.getCONTROLLER_IP());
                proc = Runtime.getRuntime().exec("sudo kill -9 " + this.procPID);
                proc.waitFor();
                procPID = -1;
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }
}
