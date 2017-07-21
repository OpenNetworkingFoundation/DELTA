package org.deltaproject.manager.core;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Configuration {

    private String FLOODLIGHT_ROOT = "";
    private String FLOODLIGHT_VER = "";

    private String ODL_ROOT = "";
    private String ODL_VER = "";
    private String ODL_APPAGENT = "";

    private String ONOS_ROOT = "";
    private String ONOS_VER = "";
    private String ONOS_KARAF_ROOT = "";

    private String CBENCH_ROOT = "";

    private String TARGET_CONTROLLER = "";
    private String TARGET_VERSION = "";

    private String CONTROLLER_SSH = "";
    private String CHANNEL_SSH = "";
    private String HOST_SSH = "";

    private String TARGET_HOST = "";

    private String OF_PORT = "";
    private String OF_VERSION = "";

    private String MITM_NIC = "";
    private String CONTROLLER_IP = "";

    private String original = "";

    private ArrayList<String> switchList;

    private String DUMMY_CONT_IP = "";
    private String DUMMY_CONT_PORT = "";

    private String AM_IP = "";
    private String AM_PORT = "";

    private String WEB = "";

    private String TOPOLOGY_TYPE = "";

    private static final Configuration instance = new Configuration();


    protected Configuration() {

    }

    public static Configuration copy() {

        Configuration oldOne = Configuration.getInstance();
        Configuration newOne = new Configuration();

        newOne.setCONTROLLER_SSH(oldOne.getCONTROLLER_SSH());
        newOne.setCHANNEL_SSH(oldOne.getCHANNEL_SSH());
        newOne.setHOST_SSH(oldOne.getHOST_SSH());

        newOne.setTARGET_CONTROLLER(oldOne.getTARGET_CONTROLLER());
        newOne.setTARGET_VERSION(oldOne.getTARGET_VERSION());
        newOne.setTARGET_HOST(oldOne.getTARGET_HOST());

        newOne.setCBENCH_ROOT(oldOne.getCBENCH_ROOT());
        newOne.setOF_VERSION(oldOne.getOF_VERSION());
        newOne.setOF_PORT(oldOne.getOF_PORT());

        newOne.setMITM_NIC(oldOne.getMITM_NIC());

        newOne.setCONTROLLER_IP(oldOne.getCONTROLLER_IP());
        newOne.setSwitchList(oldOne.getSwitchList());

        newOne.setDUMMY_CONT_IP(oldOne.getDUMMY_CONT_IP());
        newOne.setDUMMY_CONT_PORT(oldOne.getDUMMY_CONT_PORT());

        newOne.setAM_IP(oldOne.getAM_IP());
        newOne.setAM_PORT(oldOne.getAM_PORT());

        newOne.setTOPOLOGY_TYPE(oldOne.getTopologyType());

        return newOne;
    }

    public static Configuration getInstance() {
        return instance;
    }

    public void initialize(String path) {
        switchList = new ArrayList<String>();
        readConfigFile(path);
    }

    public String getTopologyType() { return TOPOLOGY_TYPE; }

    public ArrayList<String> getSwitchList() {
        return switchList;
    }

    public String show() {
        return original;
    }

    public void readConfigFile(String config) {
        BufferedReader br = null;
        InputStreamReader isr = null;
        FileInputStream fis = null;
        File file = new File(config);
        String temp = "";

        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, "UTF-8");
            br = new BufferedReader(isr);

            while ((temp = br.readLine()) != null) {

                original += temp + "\n";

                if (temp.contains("FLOODLIGHT_ROOT"))
                    this.FLOODLIGHT_ROOT = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("FLOODLIGHT_VER"))
                    this.FLOODLIGHT_VER = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("ODL_ROOT"))
                    this.ODL_ROOT = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("ODL_VER"))
                    this.ODL_VER = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("ODL_APPAGENT"))
                    this.ODL_APPAGENT = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("ONOS_ROOT"))
                    this.ONOS_ROOT = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("ONOS_VER"))
                    this.ONOS_VER = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("ONOS_KARAF_ROOT"))
                    this.ONOS_KARAF_ROOT = temp
                            .substring(temp.indexOf("=") + 1);

                if (temp.contains("CBENCH_ROOT"))
                    this.CBENCH_ROOT = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("CONTROLLER_IP")) {
                    this.CONTROLLER_IP = temp.substring(temp.indexOf("=") + 1);

                    WEB += temp + "\n";
                }

//                if (temp.contains("TARGET_CONTROLLER")) {
//                    this.TARGET_CONTROLLER = temp
//                            .substring(temp.indexOf("=") + 1);
//
//                    WEB += temp + "\n";
//                }

//                if (temp.contains("TARGET_VERSION")) {
//                    this.TARGET_VERSION = temp.substring(temp.indexOf("=") + 1);
//
//                    WEB += temp + "\n";
//                }

//                if (temp.contains("OF_PORT")) {
//                    this.OF_PORT = temp.substring(temp.indexOf("=") + 1);
//
//                    WEB += temp + "\n";
//                }

//                if (temp.contains("OF_VER")) {
//                    this.OF_VERSION = temp.substring(temp.indexOf("=") + 1);
//
//                    WEB += temp + "\n";
//                }

//                if (temp.contains("SWITCH_IP")) {
//                    String switchlist = temp.substring(temp.indexOf("=") + 1);
//                    StringTokenizer st = new StringTokenizer(switchlist, ",");
//
//                    while (st.hasMoreTokens()) {
//                        this.switchList.add(st.nextToken());
//                    }
//
//                    WEB += temp + "\n";
//                }

                if (temp.contains("MITM_NIC"))
                    this.MITM_NIC = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("TARGET_HOST"))
                    this.TARGET_HOST = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("CONTROLLER_SSH"))
                    this.CONTROLLER_SSH = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("CHANNEL_SSH"))
                    this.CHANNEL_SSH = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("HOST_SSH"))
                    this.HOST_SSH = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("DUMMY_CONT_IP"))
                    this.DUMMY_CONT_IP = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("DUMMY_CONT_PORT"))
                    this.DUMMY_CONT_PORT = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("AM_IP"))
                    this.AM_IP = temp.substring(temp.indexOf("=") + 1);

                if (temp.contains("AM_PORT"))
                    this.AM_PORT = temp.substring(temp.indexOf("=") + 1);
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

    @Override
    public String toString() {
        return "Configuration{" +
                "FLOODLIGHT_ROOT='" + FLOODLIGHT_ROOT + '\'' +
                ", FLOODLIGHT_VER='" + FLOODLIGHT_VER + '\'' +
                ", ODL_ROOT='" + ODL_ROOT + '\'' +
                ", ODL_VER='" + ODL_VER + '\'' +
                ", ODL_APPAGENT='" + ODL_APPAGENT + '\'' +
                ", ONOS_ROOT='" + ONOS_ROOT + '\'' +
                ", ONOS_VER='" + ONOS_VER + '\'' +
                ", ONOS_KARAF_ROOT='" + ONOS_KARAF_ROOT + '\'' +
                ", CBENCH_ROOT='" + CBENCH_ROOT + '\'' +
                ", TARGET_CONTROLLER='" + TARGET_CONTROLLER + '\'' +
                ", TARGET_VERSION='" + TARGET_VERSION + '\'' +
                ", CONTROLLER_SSH='" + CONTROLLER_SSH + '\'' +
                ", CHANNEL_SSH='" + CHANNEL_SSH + '\'' +
                ", HOST_SSH='" + HOST_SSH + '\'' +
                ", TARGET_HOST='" + TARGET_HOST + '\'' +
                ", OF_PORT='" + OF_PORT + '\'' +
                ", OF_VERSION='" + OF_VERSION + '\'' +
                ", MITM_NIC='" + MITM_NIC + '\'' +
                ", CONTROLLER_IP='" + CONTROLLER_IP + '\'' +
                ", switchList=" + switchList +
                ", DUMMY_CONT_IP='" + DUMMY_CONT_IP + '\'' +
                ", DUMMY_CONT_PORT='" + DUMMY_CONT_PORT + '\'' +
                ", AM_IP='" + AM_IP + '\'' +
                ", AM_PORT='" + AM_PORT + '\'' +
                ", WEB='" + WEB + '\'' +
                ", TOPOLOGY_TYPE='" + TOPOLOGY_TYPE + '\'' +
                '}';
    }

    public String getFLOODLIGHT_ROOT() {
        return FLOODLIGHT_ROOT;
    }

    public void setFLOODLIGHT_ROOT(String FLOODLIGHT_ROOT) {
        this.FLOODLIGHT_ROOT = FLOODLIGHT_ROOT;
    }

    public String getFLOODLIGHT_VER() {
        return FLOODLIGHT_VER;
    }

    public void setFLOODLIGHT_VER(String FLOODLIGHT_VER) {
        this.FLOODLIGHT_VER = FLOODLIGHT_VER;
    }

    public String getODL_ROOT() {
        return ODL_ROOT;
    }

    public void setODL_ROOT(String ODL_ROOT) {
        this.ODL_ROOT = ODL_ROOT;
    }

    public String getODL_VER() {
        return ODL_VER;
    }

    public void setODL_VER(String ODL_VER) {
        this.ODL_VER = ODL_VER;
    }

    public String getODL_APPAGENT() {
        return ODL_APPAGENT;
    }

    public void setODL_APPAGENT(String ODL_APPAGENT) {
        this.ODL_APPAGENT = ODL_APPAGENT;
    }

    public String getONOS_ROOT() {
        return ONOS_ROOT;
    }

    public void setONOS_ROOT(String ONOS_ROOT) {
        this.ONOS_ROOT = ONOS_ROOT;
    }

    public String getONOS_VER() {
        return ONOS_VER;
    }

    public void setONOS_VER(String ONOS_VER) {
        this.ONOS_VER = ONOS_VER;
    }

    public String getONOS_KARAF_ROOT() {
        return ONOS_KARAF_ROOT;
    }

    public void setONOS_KARAF_ROOT(String ONOS_KARAF_ROOT) {
        this.ONOS_KARAF_ROOT = ONOS_KARAF_ROOT;
    }

    public String getCBENCH_ROOT() {
        return CBENCH_ROOT;
    }

    public void setCBENCH_ROOT(String CBENCH_ROOT) {
        this.CBENCH_ROOT = CBENCH_ROOT;
    }

    public String getTARGET_CONTROLLER() {
        return TARGET_CONTROLLER;
    }

    public void setTARGET_CONTROLLER(String TARGET_CONTROLLER) {
        this.TARGET_CONTROLLER = TARGET_CONTROLLER;
    }

    public String getTARGET_VERSION() {
        return TARGET_VERSION;
    }

    public void setTARGET_VERSION(String TARGET_VERSION) {
        this.TARGET_VERSION = TARGET_VERSION;
    }

    public String getCONTROLLER_SSH() {
        return CONTROLLER_SSH;
    }

    public void setCONTROLLER_SSH(String CONTROLLER_SSH) {
        this.CONTROLLER_SSH = CONTROLLER_SSH;
    }

    public String getCHANNEL_SSH() {
        return CHANNEL_SSH;
    }

    public void setCHANNEL_SSH(String CHANNEL_SSH) {
        this.CHANNEL_SSH = CHANNEL_SSH;
    }

    public String getHOST_SSH() {
        return HOST_SSH;
    }

    public void setHOST_SSH(String HOST_SSH) {
        this.HOST_SSH = HOST_SSH;
    }

    public String getTARGET_HOST() {
        return TARGET_HOST;
    }

    public void setTARGET_HOST(String TARGET_HOST) {
        this.TARGET_HOST = TARGET_HOST;
    }

    public String getOF_PORT() {
        return OF_PORT;
    }

    public void setOF_PORT(String OF_PORT) {
        this.OF_PORT = OF_PORT;
    }

    public String getOF_VERSION() {
        return OF_VERSION;
    }

    public void setOF_VERSION(String OF_VERSION) {
        this.OF_VERSION = OF_VERSION;
    }

    public String getMITM_NIC() {
        return MITM_NIC;
    }

    public void setMITM_NIC(String MITM_NIC) {
        this.MITM_NIC = MITM_NIC;
    }

    public String getCONTROLLER_IP() {
        return CONTROLLER_IP;
    }

    public void setCONTROLLER_IP(String CONTROLLER_IP) {
        this.CONTROLLER_IP = CONTROLLER_IP;
    }

    public String getOriginal() {
        return original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public void setSwitchList(ArrayList<String> switchList) {
        this.switchList = switchList;
    }

    public String getDUMMY_CONT_IP() {
        return DUMMY_CONT_IP;
    }

    public void setDUMMY_CONT_IP(String DUMMY_CONT_IP) {
        this.DUMMY_CONT_IP = DUMMY_CONT_IP;
    }

    public String getDUMMY_CONT_PORT() {
        return DUMMY_CONT_PORT;
    }

    public void setDUMMY_CONT_PORT(String DUMMY_CONT_PORT) {
        this.DUMMY_CONT_PORT = DUMMY_CONT_PORT;
    }

    public String getAM_IP() {
        return AM_IP;
    }

    public void setAM_IP(String AM_IP) {
        this.AM_IP = AM_IP;
    }

    public String getAM_PORT() {
        return AM_PORT;
    }

    public void setAM_PORT(String AM_PORT) {
        this.AM_PORT = AM_PORT;
    }

    public String getWEB() {
        return WEB;
    }

    public void setWEB(String WEB) {
        this.WEB = WEB;
    }

    public String getTOPOLOGY_TYPE() {
        return TOPOLOGY_TYPE;
    }

    public void setTOPOLOGY_TYPE(String TOPOLOGY_TYPE) {
        this.TOPOLOGY_TYPE = TOPOLOGY_TYPE;
    }
}
