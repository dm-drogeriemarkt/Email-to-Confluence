package it.de.dm.mail2blog;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.atlassian.confluence.util.GeneralUtil.getStackTrace;

@Path("/runtest")
public class IntegrationTestRestRunner {

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/testProcess")
    public Response testProcessRunner() {
        IntegrationTestRestRunnerModel resp = new IntegrationTestRestRunnerModel();

        try {
            IntegrationTest integrationTest = new IntegrationTest();

            integrationTest.setUp();
            integrationTest.testProcess();

            resp.setSuccessful(true);
            resp.setMessage("");
            resp.setStacktrace("");
        } catch (Exception e) {
            resp.setSuccessful(false);
            resp.setMessage(e.toString());
            resp.setStacktrace(getStackTrace(e));
        }

        return Response.ok(resp).build();
    }
}