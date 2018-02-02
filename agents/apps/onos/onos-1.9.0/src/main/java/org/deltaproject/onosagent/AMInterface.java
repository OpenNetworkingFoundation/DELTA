package org.deltaproject.onosagent;

import org.slf4j.Logger;

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
import java.net.ConnectException;
import java.net.Socket;

import static org.slf4j.LoggerFactory.getLogger;

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
    private final Logger log = getLogger(getClass());

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

    public void connectServer(String agent) throws Exception {
        socket = new Socket(serverIP, serverPort);
        in = socket.getInputStream();
        dis = new DataInputStream(in);
        out = socket.getOutputStream();
        dos = new DataOutputStream(out);

        dos.writeUTF(agent);
        dos.flush();
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

            String cmd = null;
            if (recv.contains("install")) {
                result = app.sendUnFlaggedFlowRemoveMsg("install", 0);
            } else if (recv.contains("check")) {
                long ruleId = Long.parseLong(recv.split("\\|")[2]);
                result = app.sendUnFlaggedFlowRemoveMsg("check", ruleId);
            }
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
        String recv = "";

        while (true) {
            try {
                this.setServerAddr();
                this.connectServer("AppAgent");
                while (true) {
                    recv = dis.readUTF();
                    log.info("[App-Agent] Received " + recv);
                    if (recv.contains("umode")) {
                        findingUnkwonAttack(recv);
                    } else if (recv.contains("echo")) {
                        write("echo");
                    } else {
                        replayingKnownAttack(recv);
                    }
                }
            } catch (ConnectException e) {
                log.error("[App-Agent] Agent Manager is not listening");
            } catch (EOFException e) {
                // if any error occurs
                log.info("[App-Agent] Closing...");
            } catch (Exception e) {
                log.error(e.toString());
            } finally {
                try {
                    if (dis != null) {
                        dis.close();
                    }
                    if (dos != null) {
                        dos.close();
                    }
                } catch (IOException e) {
                    log.error(e.toString());
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error(e.toString());
            }
        }
    }
}
