package org.deltaproject.odlagent.core;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Created by seungsoo on 03/07/2017.
 */
public class Interface extends Thread {
    private int result = 1;

    private AppAgentFacadeImpl app;
    private Activator act;

    private Socket socket;
    private InputStream in;
    private DataInputStream dis;
    private OutputStream out;
    private DataOutputStream dos;

    private String serverIP;
    private int serverPort;

    public Interface() {

    }

    public void setServerAddr() {
        // default
        this.serverIP = "10.0.2.2";
        this.serverPort = 3366;

        String path = "~";

        String home = System.getenv("HOME");
        if (home != null) {
            path = home;
        }

        BufferedReader br = null;
        InputStreamReader isr = null;
        FileInputStream fis = null;
        File file = new File(path + "/agent.cfg");
        String temp;

        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, "UTF-8");
            br = new BufferedReader(isr);

            while ((temp = br.readLine()) != null) {
                if (temp.contains("MANAGER_IP"))
                    this.serverIP = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("MANAGER_PORT"))
                    this.serverPort = Integer.parseInt(temp.substring(temp.indexOf("=") + 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                isr.close();

                assert br != null;
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void connectServer(String agent) {
        try {
            socket = new Socket(serverIP, serverPort);
            in = socket.getInputStream();
            dis = new DataInputStream(in);
            out = socket.getOutputStream();
            dos = new DataOutputStream(out);

            dos.writeUTF(agent);
            dos.flush();
            System.out.println("[App-Agent] Connected with Agent-Manager");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setAgent(AppAgentFacadeImpl in) {
        this.app = in;
    }

    public void setActivator(Activator in) {
        this.act = in;
    }

    public void write(String in) {
        try {
            dos.writeUTF(in);
            dos.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void replayingKnownAttack(String recv) throws IOException {
        String result = "";

        if (recv.contains("3.1.020")) {
            app.setControlMessageDrop();
            dos.writeUTF("null");
        } else if (recv.contains("3.1.030")) {
            app.setInfiniteLoop();
            return;
        } else if (recv.contains("3.1.040")) {
            result = app.testInternalStorageAbuse();
            dos.writeUTF(result);
        } else if (recv.contains("3.1.070")) {
            result = app.testFlowRuleModification();
            dos.writeUTF(result);
        } else if (recv.contains("3.1.080")) {
            if (recv.contains("false"))
                app.testFlowTableClearance(false);
            else
                app.testFlowTableClearance(true);
            return;
        } else if (recv.contains("3.1.090")) {
            result = act.testEventListenerUnsubscription("l2switch");
            dos.writeUTF(result);
        } else if (recv.contains("3.1.100")) {
            result = act.testApplicationEviction("l2switch");
            dos.writeUTF(result);
        } else if (recv.contains("3.1.110")) {
            app.testResourceExhaustionMem();
            return;
        } else if (recv.contains("3.1.120")) {
            app.testResourceExhaustionCPU();
            return;
        } else if (recv.contains("3.1.130")) {
            app.testSystemVariableManipulation();
            return;
        } else if (recv.contains("3.1.140")) {
            app.testSystemCommandExecution();
            return;
        } else if (recv.contains("restore")) {
            act.testApplicationEviction("restore");
            dos.writeUTF("OK");
        }  else if (recv.contains("2.1.060")) {
            String cmd = null;
            if (recv.contains("install")) {
                result = app.sendUnFlaggedFlowRemoveMsg("install", 0);
            } else if (recv.contains("check")) {
                long ruleId = Long.parseLong(recv.split("\\|")[2]);
                result = app.sendUnFlaggedFlowRemoveMsg("check", ruleId);
            }
            dos.writeUTF(result);
        }

        /* else if (recv.contains("3.1.190")) {
            app.testFlowRuleFlooding();
            return;
        } else if (recv.contains("3.1.200")) {
            result = app.testSwitchFirmwareMisuse();
            dos.writeUTF(result);
        } else if (recv.contains("2.1.060")) {
            result = app.sendUnFlaggedRemoveMsg();
            dos.writeUTF(result);
        } */

        dos.flush();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        String recv;

        try {
            while (true) {
                // reads characters encoded with modified UTF-8
                recv = dis.readUTF();
                System.out.println("[App-Agent] Receive msg from agent-manager " + recv);
                replayingKnownAttack(recv);
            }
        } catch (Exception e) {
            // if any error occurs
            // e.printStackTrace();
        } finally {

        }
    }
}
