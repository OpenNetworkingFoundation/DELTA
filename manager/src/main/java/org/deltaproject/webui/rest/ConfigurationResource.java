package org.deltaproject.webui.rest;

import org.deltaproject.manager.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Get queued test cases add test cases.
 * Created by Changhoon on 7/7/16.
 */
@Path("/json/config")
public class ConfigurationResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @POST
    @Path("/post")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response setConfiguration(String data) {

        Configuration cfg = Configuration.getInstance();
        String[] recvMsgArr = data.split(" ");
        cfg.setTARGET_CONTROLLER(recvMsgArr[0]);
        cfg.setTARGET_VERSION(recvMsgArr[1]);
        cfg.setOF_PORT(recvMsgArr[2]);
        cfg.setOF_VERSION(recvMsgArr[3]);
        cfg.setCONTROLLER_IP(recvMsgArr[4]);
        cfg.setTOPOLOGY_TYPE(recvMsgArr[6]);

        ArrayList<String> switchList = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(recvMsgArr[5], ",");
        while (st.hasMoreTokens()) {
            switchList.add(st.nextToken());
        }

        cfg.setSwitchList(switchList);
//        log.info("Configuration changed.");
//        log.info(cfg.toString());

        return Response.status(201).build();
    }
}
