package it.de.dm.mail2blog;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.atlassian.confluence.util.GeneralUtil.getStackTrace;

@Path("/runtest")
public class IntegrationTestRestRunner {

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/testProcessImaps")
    public Response testProcessImapsRunner() {
        IntegrationTestRestRunnerModel resp = new IntegrationTestRestRunnerModel();

        try {
            IntegrationTest integrationTest = new IntegrationTest();

            integrationTest.testProcessImaps();

            resp.setSuccessful(true);
            resp.setMessage("");
            resp.setStacktrace("");
        } catch (Throwable e) {
            resp.setSuccessful(false);
            resp.setMessage(e.toString());
            resp.setStacktrace(getStackTrace(e));
        }

        return Response.ok(resp).build();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/testProcessPop3")
    public Response testProcessPop3Runner() {
        IntegrationTestRestRunnerModel resp = new IntegrationTestRestRunnerModel();

        try {
            IntegrationTest integrationTest = new IntegrationTest();

            integrationTest.testProcessPop3();

            resp.setSuccessful(true);
            resp.setMessage("");
            resp.setStacktrace("");
        } catch (Throwable e) {
            resp.setSuccessful(false);
            resp.setMessage(e.toString());
            resp.setStacktrace(getStackTrace(e));
        }

        return Response.ok(resp).build();
    }
}