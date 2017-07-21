package org.deltaproject.onosagent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * An Agent Manager Interface of ONOS AppAgent.
 */
public class AMInterface extends Thread {

    private AppAgent app;
    private Socket socket;
    private InputStream in;
    private DataInputStream dis;
    private OutputStream out;
    private DataOutputStream dos;
    private String serverIP;
    private int serverPort;

    public AMInterface(AppAgent in) {
        this.app = in;
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
                if (temp.contains("MANAGER_IP")) {
                    this.serverIP = temp.substring(temp.indexOf("=") + 1);
                }
                if (temp.contains("MANAGER_PORT")) {
                    this.serverPort = Integer.parseInt(temp.substring(temp.indexOf("=") + 1));
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                isr.close();
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

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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

    public void replayingKnownAttack(String recv) throws IOException {
        String result = "";

        if (recv.contains("3.1.020")) {
            app.setControlMessageDrop();
            result = app.testControlMessageDrop();
            dos.writeUTF(result);
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
            app.testFlowTableClearance();
            dos.writeUTF(result);
        } else if (recv.contains("3.1.090")) {
            if (app.testEventListenerUnsubscription()) {
                dos.writeUTF("success");
            } else {
                dos.writeUTF("fail");
            }
        } else if (recv.contains("3.1.100")) {
            result = app.testApplicationEviction("fwd");
            dos.writeUTF(result);
        } else if (recv.contains("3.1.110")) {
            app.testResourceExhaustionMem();
            return;
        } else if (recv.contains("3.1.120")) {
            app.testResourceExhaustionCpu();
            return;
        } else if (recv.contains("3.1.130")) {
            app.testSystemVariableManipulation();
            return;
        } else if (recv.contains("3.1.140")) {
            app.testSystemCommandExecution();
            return;
        } else if (recv.contains("3.1.190")) {
            app.testFlowRuleFlooding();
            return;
        } else if (recv.contains("3.1.200")) {
            result = app.testSwitchFirmwareMisuse();
            dos.writeUTF(result);
        } else if (recv.contains("2.1.060")) {
            result = app.sendUnFlaggedFlowRemoveMsg();
            dos.writeUTF(result);
        } else if (recv.contains("test")) {

            return;
        }

        dos.flush();
    }

    public void findingUnkwonAttack(String recv) {

    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        String recv = "";

        try {
            while ((recv = dis.readUTF()) != null) {
                // reads characters encoded with modified UTF-8
                if (recv.contains("umode")) {
                    findingUnkwonAttack(recv);
                } else if (recv.contains("echo")) {
                    System.out.println("[App-Agent] Received " + recv);
                    write("echo");
                } else {
                    replayingKnownAttack(recv);
                }
            }
        } catch (EOFException e) {
            System.out.println("[App-Agent] Closing...");
        } catch (Exception e) {
            // if any error occurs
//            e.printStackTrace();
        } finally {
            try {
                dis.close();
                dos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
//                e.printStackTrace();
            }
        }
    }
}
