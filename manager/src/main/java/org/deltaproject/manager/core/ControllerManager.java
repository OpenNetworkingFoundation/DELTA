package org.deltaproject.manager.core;

import org.deltaproject.manager.handler.FloodlightHandler;
import org.deltaproject.manager.handler.ONOSHandler;
import org.deltaproject.manager.handler.OpenDaylightHandler;
import org.deltaproject.manager.handler.ControllerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class ControllerManager {
    private String cbechPath = "";
    private String targetController = "";
    private String targetVersion = "";
    private String ofPort = "";
    private String sshAddr = "";

    private int cbenchPID = -1;

    private ArrayList<ControllerHandler> targetList;
    private ArrayList<String> switchList;
    private ArrayList<String> connectedSwitches;

    private Process processCbench;

    private Configuration cfg;
    private static final Logger log = LoggerFactory.getLogger(ControllerManager.class.getName());


    public ControllerManager() {
    }

    public void setConfig(Configuration cfg) {
        this.cfg = cfg;
        ControllerHandler fl = new FloodlightHandler(cfg.getFLOODLIGHT_ROOT(), cfg.getTARGET_VERSION(), cfg.getCONTROLLER_SSH());
        ControllerHandler odl = new OpenDaylightHandler(cfg.getODL_ROOT(), cfg.getTARGET_VERSION(), cfg.getCONTROLLER_SSH());
        ControllerHandler onos = new ONOSHandler(cfg.getONOS_ROOT(), cfg.getTARGET_VERSION(), cfg.getCONTROLLER_SSH());

        targetList = new ArrayList<ControllerHandler>();
        targetList.add(fl);
        targetList.add(odl);
        targetList.add(onos);
        switchList = new ArrayList<String>();

        connectedSwitches = new ArrayList<String>();
        sshAddr = cfg.getCONTROLLER_SSH();
        cbechPath = cfg.getCBENCH_ROOT();
        targetController = cfg.getTARGET_CONTROLLER();
        targetVersion = cfg.getTARGET_VERSION();
        ofPort = cfg.getOF_PORT();
        switchList = cfg.getSwitchList();
    }

    public BufferedReader getStdOut() {
        for (ControllerHandler tc : targetList) {
            if (tc.getType().equals(this.targetController)) {
                return tc.getStdOut();
            }
        }

        return null;
    }

    public boolean createController() {
        boolean result;
        for (ControllerHandler tc : targetList) {
            if (tc.getType().equals(this.targetController)) {
                result = tc.createController();

                return result;
            }
        }
        return false;
    }

    public boolean killController() {
        String switchIP = cfg.getHOST_SSH().substring(cfg.getHOST_SSH().indexOf('@') + 1);

        try {
            Runtime.getRuntime().exec("ssh " + cfg.getCONTROLLER_SSH() + " sudo arp -d " + switchIP);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (ControllerHandler tc : targetList) {
            if (tc.getType().equals(this.targetController)) {
                tc.killController();
                return true;
            }
        }
        return false;
    }

    public String getType() {
        return targetController;
    }

    public String getVersion() {
        return targetVersion;
    }

    public String showConfig() {
        String result = "";

        result += "Target Controller: ";

        for (ControllerHandler tc : targetList) {
            if (tc.getType().equals(this.targetController)) {
                result += tc.getType() + " - " + tc.getVersion();
            }
        }

        result += "\nTarget Path: ";

        for (ControllerHandler tc : targetList) {
            if (tc.getType().equals(this.targetController)) {
                result += tc.getPath();
            }
        }

        result += "\nSwitchs: ";

        for (String s : this.switchList) {
            result += s + ", ";
        }

        result += "\b\b  ";

        result += "\nCbench Path: " + this.cbechPath;

        return result;
    }

    public void addSwitchIP(String ip) {
        switchList.add(ip);
    }

    public boolean isRunning() {
        int controllerPID = -1;

        for (ControllerHandler tc : targetList) {
            if (tc.getType().equals(this.targetController)) {
                controllerPID = tc.getPID();
            }
        }

        Process temp = null;
        String str = "";
        try {
            temp = Runtime.getRuntime().exec(new String[]{"bash", "-c", "ps -a | grep " + controllerPID});

            BufferedReader stdOut = new BufferedReader(new InputStreamReader(temp.getInputStream()));

            if ((str = stdOut.readLine()) == null) {
                controllerPID = -1;
                temp.destroy();
                return false;
            }

            stdOut.close();
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public boolean executeCbench() {
        try {
            processCbench = Runtime.getRuntime().exec(cbechPath + "cbench -c " + cfg.getCONTROLLER_IP() + "  -p "
                    + ofPort + " -m 10000 -l 10 -s 16 -M 1000 -t");

            Field pidField = Class.forName("java.lang.UNIXProcess").getDeclaredField("pid");
            pidField.setAccessible(true);
            Object value = pidField.get(processCbench);

            this.cbenchPID = (Integer) value;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;
    }

    public void flushARPcache() {
        try {
            Process pc = Runtime.getRuntime().exec("ip -s -s neigh flush all");
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
    }

    public boolean isListeningSwitch() {
        Process temp;
        String tempS;

        boolean flag = false;

        try {
            temp = Runtime.getRuntime().exec("ssh " + sshAddr + " sudo netstat -ap | grep " + ofPort);

            BufferedReader stdOut = new BufferedReader(new InputStreamReader(temp.getInputStream()));

            while ((tempS = stdOut.readLine()) != null) {
                if (tempS.contains("LISTEN")) {
                    return true;
                }
            }
            stdOut.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    public int isConnectedSwitch(boolean wait) {
        Process temp;
        String tempS;
        int switchCnt = this.switchList.size();
        int cnt = 0;

        int flag = 1;

        while (flag > 0) {
            try {
                cnt = 0;
                String cmd = "";
                temp = Runtime.getRuntime().exec("ssh " + sshAddr + " sudo netstat -ap | grep " + "66[3,5]3");

                BufferedReader stdOut = new BufferedReader(new InputStreamReader(temp.getInputStream()));

                while ((tempS = stdOut.readLine()) != null) {
                    if (tempS.contains("ESTABLISHED")) {
                        connectedSwitches.add(tempS);
                        cnt++;
                    }
                }

                stdOut.close();

                if (switchCnt <= cnt) {
                    break;
                }

                Thread.sleep(1000);

                if (!wait) {
                    flag = 0;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return cnt;
    }

    public int getSwitchCounter() {
        return this.switchList.size();
    }
}
