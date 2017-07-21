package org.deltaproject.webui;

import org.deltaproject.manager.core.AttackConductor;
import org.deltaproject.manager.core.Configuration;
import org.deltaproject.manager.core.ControllerManager;
import org.deltaproject.manager.testcase.TestSwitchCase;
import org.deltaproject.manager.utils.AgentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.deltaproject.webui.TestCase.Status.*;

/**
 * A executor for queued test case.
 * Created by Changhoon on 9/8/16.
 */
public class TestCaseExecutor extends Thread {

    private AttackConductor conductor;
    private TestQueue queue = TestQueue.getInstance();
    private boolean running;
    private static final Logger log = LoggerFactory.getLogger(TestCaseExecutor.class);

    public TestCaseExecutor(AttackConductor conductor) {
        this.conductor = conductor;
        running = true;
    }

    @Override
    public void run() {
        while (running) {
            if (!queue.isEmpty()) {

                // apply configuration from Web UI
                TestCase test = queue.getNext();
                log.info(test.getcasenum() + " - " + test.getName() + " - " + test.getDesc());
                conductor.refreshConfig(test.getConfiguration());
                try {
                    queue.setRunningTestCase(test);
                    conductor.executeTestCase(test);
                    queue.unsetRunningTestCase(test);
                    AgentLogger.stopAllLogger();
                } catch (InterruptedException e) {
                    test.setStatus(UNAVAILABLE);
                    log.error(e.toString());
                }
            }

            try {
                Thread.sleep(999);
            } catch (InterruptedException e) {
                running = false;
            }
        }
    }
}
