package org.deltaproject.webui;

import java.util.concurrent.ConcurrentHashMap;

import static org.deltaproject.webui.TestCase.Category.ADVANCED;
import static org.deltaproject.webui.TestCase.Category.CONTROL_PLANE_OF;
import static org.deltaproject.webui.TestCase.Category.DATA_PLANE_OF;

/**
 * A test case directory for entries on DELTA GUI.
 * Created by Changhoon on 7/6/16.
 */
public class TestCaseDirectory {

    private static final ConcurrentHashMap<String, TestCase> TEST_CASE_DIRECTORY =
            initializeDirectory();

    protected TestCaseDirectory() {
    }

    public static ConcurrentHashMap<String, TestCase> getDirectory() {
        return TEST_CASE_DIRECTORY;
    }

    private static ConcurrentHashMap<String, TestCase> initializeDirectory() {
        ConcurrentHashMap<String, TestCase> directory =
                new ConcurrentHashMap<>();

        directory.put("1.1.010",
                new TestCase(DATA_PLANE_OF, "1.1.010", "Port Range Violation", "Test switch protection against " +
                        "disallowed ports"));
        /* directory.put("1.1.011",
                new TestCase(DATA_PLANE_OF, "1.1.011", "TTP Port Range Violation", "")); */
        directory.put("1.1.020",
                new TestCase(DATA_PLANE_OF, "1.1.020", "Table Number Violation", "Test switch protection against " +
                        "invalid table ID"));
        directory.put("1.1.030",
                new TestCase(DATA_PLANE_OF, "1.1.030", "Group Identifier Violation", "Test switch protection against " +
                        "disallowed group numbers"));
        directory.put("1.1.040",
                new TestCase(DATA_PLANE_OF, "1.1.040", "Meter Identifier Violation", "Test switch protection against " +
                        "disallowed meter numbers"));
        directory.put("1.1.050",
                new TestCase(DATA_PLANE_OF, "1.1.050", "Table Loop Violation", "Test switch protection against " +
                        "invalid GoToTable request"));
        directory.put("1.1.060",
                new TestCase(DATA_PLANE_OF, "1.1.060", "Corrupted Control Message Type", "Test switch protection " +
                        "against control message with unsupported type"));
        directory.put("1.1.070",
                new TestCase(DATA_PLANE_OF, "1.1.070", "Unsupported Version Number (bad version)", "Test switch " +
                        "protection against connection setup request with unsupported version number"));
        directory.put("1.1.080",
                new TestCase(DATA_PLANE_OF, "1.1.080", "Malformed Version Number (supported but not negotiated " +
                        "version)", "Test switch protection against communication with mismatched OpenFlow versions"));
        directory.put("1.1.090",
                new TestCase(DATA_PLANE_OF, "1.1.090", "Invalid OXM - Type", "Test switch protection against flow mod" +
                        " with invalid message type"));
        directory.put("1.1.100",
                new TestCase(DATA_PLANE_OF, "1.1.100", "Invalid OXM - Length", "Test switch protection against flow " +
                        "mod with invalid message length"));
        directory.put("1.1.110",
                new TestCase(DATA_PLANE_OF, "1.1.110", "Invalid OXM - Value", "Test switch protection against flow " +
                        "mod with invalid message value"));
        directory.put("1.1.120",
                new TestCase(DATA_PLANE_OF, "1.1.120", "Disabled Table Features Request", "Test for switch protection" +
                        " against table features request when feature is disabled"));
        directory.put("1.1.130",
                new TestCase(DATA_PLANE_OF, "1.1.130", "Handshake without Hello Message", "Test for switch protection" +
                        " against incomplete control connection left open"));
        directory.put("1.1.140",
                new TestCase(DATA_PLANE_OF, "1.1.140", "Control Message before Hello Message (Main Connection)",
                        "Test for switch protection against control communication prior to completed connection " +
                                "establishment"));
        directory.put("1.1.150",
                new TestCase(DATA_PLANE_OF, "1.1.150", "Incompatible Hello after Connection Establishment", "Test for" +
                        " switch protection against abuse of the Hello_Failed error message"));
        directory.put("1.1.160",
                new TestCase(DATA_PLANE_OF, "1.1.160", "Corrupted Cookie Values", "Test for switch protection against" +
                        " replay attacks"));
        directory.put("1.1.170",
                new TestCase(DATA_PLANE_OF, "1.1.170", "Malformed Buffer ID Values", "Test for switch protection " +
                        "against disallowed buffer ID values"));

        /*directory.put("1.2.010",
                new TestCase(DATA_PLANE_OF, "1.2.010", "Slave Controller Violation"));
        directory.put("1.2.020",
                new TestCase(DATA_PLANE_OF, "1.2.020", "Corrupted Generation ID"));
        directory.put("1.2.030",
                new TestCase(DATA_PLANE_OF, "1.2.030", "Auxiliary Connection - Terminate when main connection is
                down"));
        directory.put("1.2.040",
                new TestCase(DATA_PLANE_OF, "1.2.040", "Auxiliary Connection - Initiate Non-Hello"));
        directory.put("1.2.050",
                new TestCase(DATA_PLANE_OF, "1.2.050", "Auxiliary Connection - Unsupported Messages")); */

        directory.put("2.1.010",
                new TestCase(CONTROL_PLANE_OF, "2.1.010", "Malformed Version Number (supported but not negotiated " +
                        "version)", "Test for controller protection against communication with mismatched OpenFlow " +
                        "versions"));
        directory.put("2.1.020",
                new TestCase(CONTROL_PLANE_OF, "2.1.020", "Corrupted Control Message Type", "Test for controller " +
                        "protection against control messages with corrupted content"));
        directory.put("2.1.030",
                new TestCase(CONTROL_PLANE_OF, "2.1.030", "Handshake without Hello Message", "Test for controller " +
                        "protection against incomplete control connection left open"));
        directory.put("2.1.040",
                new TestCase(CONTROL_PLANE_OF, "2.1.040", "Control Message before Hello Message (Main Connection)",
                        "Test for controller protection against control communication prior to completed connection " +
                                "establishment"));
        directory.put("2.1.050",
                new TestCase(CONTROL_PLANE_OF, "2.1.050", "Multiple main connection request from same switch", "Test " +
                        "for controller protection against multiple control requests"));
        directory.put("2.1.060",
                new TestCase(CONTROL_PLANE_OF, "2.1.060", "no-flagged Flow Remove Message notification", "Test for " +
                        "controller protection against unacknowledged manipulation of the network"));

        directory.put("2.1.070",
                new TestCase(CONTROL_PLANE_OF, "2.1.070", "TLS Support", "Test for controller support for Transport " +
                        "Layer Security"));

        directory.put("2.1.080",
                new TestCase(CONTROL_PLANE_OF, "2.1.080", "Auxiliary Connection Mismatch with main connection",
                        "Test for check the notification to the operator of a mismatch between the auxiliary " +
                                "connection transport and the main connection transport"));

        /*
        directory.put("2.1.071",
                new TestCase(CONTROL_PLANE_OF, "2.1.071", "Startup Behaviour with Failed TLS Connection"));
        directory.put("2.1.072",
                new TestCase(CONTROL_PLANE_OF, "2.1.072", "Handling Invalid Authentication Credentials"));
        directory.put("2.1.073",
                new TestCase(CONTROL_PLANE_OF, "2.1.073", "Handling Control Packet Modification")); */


        directory.put("3.1.010",
                new TestCase(ADVANCED, "3.1.010", "Packet-In Flooding", "Test for controller protection against " +
                        "Packet-In Flooding"));
        directory.put("3.1.020",
                new TestCase(ADVANCED, "3.1.020", "Control Message Drop", "Test for controller protection against " +
                        "application dropping control messages"));
        directory.put("3.1.030",
                new TestCase(ADVANCED, "3.1.030", "Infinite Loops", "Test for controller protection against " +
                        "application creating infinite loop"));
        directory.put("3.1.040",
                new TestCase(ADVANCED, "3.1.040", "Internal Storage Abuse", "Test for controller protection against " +
                        "application manipulating network information base"));
        directory.put("3.1.050",
                new TestCase(ADVANCED, "3.1.050", "Switch Table Flooding", "Test for switch protection against switch " +
                        "table flooding"));
        directory.put("3.1.060",
                new TestCase(ADVANCED, "3.1.060", "Switch ID spoofing", "Test for switch protection against ID " +
                        "spoofing"));
        directory.put("3.1.070",
                new TestCase(ADVANCED, "3.1.070", "Flow Rule Modification", "Test for switch protection against " +
                        "application modifying flow rule"));
        directory.put("3.1.080",
                new TestCase(ADVANCED, "3.1.080", "Flow Table Clearance", "Test for controller protection against " +
                        "flow table flushing"));
        directory.put("3.1.090",
                new TestCase(ADVANCED, "3.1.090", "Event Listener Unsubscription", "Test for controller protection " +
                        "against application unsubscribing neighbour application from events"));
        directory.put("3.1.100",
                new TestCase(ADVANCED, "3.1.100", "Application Eviction", "Test for controller protection against one" +
                        " application uninstalling another application"));
        directory.put("3.1.110",
                new TestCase(ADVANCED, "3.1.110", "Memory Exhaustion", "Test for controller protection against an " +
                        "application exhausting controller memory"));
        directory.put("3.1.120",
                new TestCase(ADVANCED, "3.1.120", "CPU Exhaustion", "Test for controller protection against an " +
                        "application exhausting controller CPU"));
        directory.put("3.1.130",
                new TestCase(ADVANCED, "3.1.130", "System Variable Manipulation", "Test for controller protection " +
                        "against an application manipulating a system variable"));
        directory.put("3.1.140",
                new TestCase(ADVANCED, "3.1.140", "System Command Execution", "Test for controller protection against" +
                        " an application accessing a system command"));
        /* directory.put("3.1.150",
                new TestCase(ADVANCED, "3.1.150", "Host Location Hijacking", "Not implemented yet")); */
        directory.put("3.1.160",
                new TestCase(ADVANCED, "3.1.160", "Link Fabrication", "Test for controller protection against " +
                        "application creating fictitious link"));
        directory.put("3.1.170",
                new TestCase(ADVANCED, "3.1.170", "Eavesdrop", "Test for control channel protection against malicious" +
                        " host sniffing the control channel"));
        directory.put("3.1.180",
                new TestCase(ADVANCED, "3.1.180", "Man-In-The-Middle", "Test for control channel protection against " +
                        "MITM attack"));
        directory.put("3.1.190",
                new TestCase(ADVANCED, "3.1.190", "Flow Rule Flooding", "Test for switch protection against flow rule" +
                        " flooding"));
        directory.put("3.1.200",
                new TestCase(ADVANCED, "3.1.200", "Switch Firmware Abuse", "Test for switch protection against " +
                        "application installing unsupported flow rules"));

        //directory.put("0.0.011",
        //        new TestCase(FUZZING, "0.0.010", "Control Plane Fuzzing Test", "Finding unknown attack case for
        // control plane"));
        //directory.put("0.0.010",
        //new TestCase(FUZZING, "0.0.010", "Control Plane Seed-based Fuzzing Test", "Finding unknown attack case for
        // control plane"));
        //directory.put("0.0.020",
        //        new TestCase(FUZZING, "0.0.020", "Data Plane Fuzzing Test", "Finding unknown attack case for data
        // plane"));
        //directory.put("0.0.021",
        //        new TestCase(FUZZING, "0.0.021", "Data Plane Seed-based Fuzzing Test", "Finding unknown attack case
        // for data plane"));

        return directory;
    }
}
