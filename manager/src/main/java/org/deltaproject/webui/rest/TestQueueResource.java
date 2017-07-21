package org.deltaproject.webui.rest;

import org.deltaproject.manager.core.Configuration;
import org.deltaproject.webui.TestCase;
import org.deltaproject.webui.TestCaseDirectory;
import org.deltaproject.webui.TestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

/**
 * Get queued test cases add test cases.
 * Created by Changhoon on 7/7/16.
 */
@Path("/json/testqueue")
public class TestQueueResource {

    private final Logger log = LoggerFactory.getLogger(getClass());


    @GET
    @Path("/get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTestQueue() {
        GenericEntity<Collection<TestCase>> queuelist =
                new GenericEntity<Collection<TestCase>>(TestQueue.getInstance().getTestcases()) {

                };

        return Response.ok(queuelist).build();
    }

    @POST
    @Path("/post")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response addTestCases(String indexes) {

        String response = "";
        int count = 0;
        String[] indexList = indexes.split(",");
        for (int i = 0; i < indexList.length; i++) {
            if (TestCaseDirectory.getDirectory().containsKey(indexList[i].trim())) {
                TestCase testCase = new TestCase(indexList[i].trim());
                testCase.setStatus(TestCase.Status.QUEUED);
                testCase.setConfiguration(Configuration.copy());
                TestQueue.getInstance().push(testCase);


                response += testCase.getName() + "\n";
                count++;
            }
        }

        return Response.status(201).entity(response + "\n" + count + " test(s) has been queued.").build();
    }

    @POST
    @Path("/stop")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopQueuedTestCases(String indexes) {

        TestQueue testQueue = TestQueue.getInstance();

        int count = 0;
        String[] indexList = indexes.split(",");
        for (int i = 0; i < indexList.length; i++) {
            Integer testCaseIdx = Integer.parseInt(indexList[i].trim());
            TestCase testCase = testQueue.get(testCaseIdx);

            if (testCase.getStatus() == TestCase.Status.QUEUED) {
                testQueue.remove(testCaseIdx);

            } else if (testCase.getStatus() == TestCase.Status.RUNNING) {
                testQueue.getRunningTestCase();
            }

            count++;
        }

        return Response.status(201).entity(count + " test(s) has been removed.").build();
    }
}
