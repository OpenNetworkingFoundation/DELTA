package org.deltaproject.webui;

import org.deltaproject.manager.core.Configuration;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A queued test case listed on DELTA GUI.
 * Created by Changhoon on 7/5/16.
 */
public class TestCase {

    /**
     * Type of status for test cases.
     */
    public enum Status {
        /**
         * Represents that the status is not determined.
         */
        UNKNOWN,

        /**
         * Represents that the test case cannot be executed.
         */
        UNAVAILABLE,

        /**
         * Represents that the test case is queued.
         */
        QUEUED,

        /**
         * Represents that the test case is running.
         */
        RUNNING,

        /**
         * Represents that the test case is completed.
         */
        COMPLETE,
    }

    /**
     * Type of category for test cases.
     */
    public enum Category {
        /**
         * Represents that the test case targets the control plane.
         */
        CONTROL_PLANE_OF,

        /**
         * Represents that the test case targets the data plane.
         */
        DATA_PLANE_OF,

        /**
         * Represents that the test case is a advanced threat.
         */
        ADVANCED,

        /**
         * Represents that the test case leverages a fuzzing technique.
         */
        FUZZING,

        /**
         * Represents that the case is not belong to any category.
         */
        UNSPECIFIED,
    }

    /**
     * Type of SDN controllers.
     */
    public enum ControllerType {
        /**
         * Represents that the controller type is ONOSHandler.
         */
        ONOS,

        /**
         * Represents that the controller type is OpenDayLight.
         */
        OPENDAYLIGHT,

        /**
         * Represents that the controller type is FloodLight.
         */
        FLOODLIGHT,

        /**
         * Represents that the controller type is unspecified.
         */
        UNSPECIFIED,
    }

    /**
     * Type of execution results.
     */
    public enum TestResult {
        /**
         *
         */
        UNKNOWN,

        /**
         *
         */
        PASS,

        /**
         *
         */
        FAIL,
    }

    private Integer index = -1;
    private Category category;
    private String casenum;
    private String name;
    private String desc;
    private ControllerType controllerType;
    private String controllerVer;
    private Status status;
    private Date time;
    private TestResult result;
    private Configuration configuration;

    public TestCase(String casenum) {
        this.casenum = casenum;
        TestCase testCase = TestCaseDirectory.getDirectory().get(casenum);
        this.name = testCase.getName();
        this.desc = testCase.getDesc();
        this.category = testCase.getCategory();
        this.controllerType = testCase.getControllerType();
        this.controllerVer = testCase.getControllerVersion();
        this.status = testCase.getStatus();
        this.time = new Date();
        this.result = TestResult.UNKNOWN;
    }

    public TestCase(Category category, String casenum, String name, String desc) {
        this.category = category;
        this.casenum = casenum;
        this.name = name;
        this.controllerType = ControllerType.UNSPECIFIED;
        this.controllerVer = "";
        this.status = Status.UNKNOWN;
        this.time = new Date();
        this.result = TestResult.UNKNOWN;
        this.desc = desc;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setcasenum(String casenum) {
        this.casenum = casenum;
    }

    public void setControllerType(ControllerType controllerType) {
        this.controllerType = controllerType;
    }

    public void setControllerVersion(String controllerVer) {
        this.controllerVer = controllerVer;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(Status status) {
        this.time = new Date();
        this.status = status;
    }

    public void setResult(TestResult result) {
        this.result = result;
    }

    public Integer getIndex() {
        return index;
    }

    public Category getCategory() {
        return category;
    }

    public String getcasenum() {
        return casenum;
    }

    public String getName() {
        return name;
    }

    public ControllerType getControllerType() {
        return controllerType;
    }

    public String getControllerVersion() {
        return controllerVer;
    }

    public Status getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public String getTime() {

        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(time);
    }

    public TestResult getResult() {
        return result;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public String toString() {
        return "[category=" + category.name() +
                ", index=" + index +
                ", casenum=" + casenum +
                ", desc=" + desc +
                ", controllerType=" + controllerType.name() +
                ", controllerVer=" + controllerVer +
                ", status=" + status.name() +
                "]";
    }
}
