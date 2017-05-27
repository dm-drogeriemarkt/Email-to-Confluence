package de.dm.mail2blog;

import com.atlassian.scheduler.JobRunnerResponse;
import lombok.Setter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by d0265672 on 27/03/2017.
 */
@Path("/runner")
public class Mail2BlogJobRest {

    // Autowired
    @Setter IMail2BlogJob mail2BlogJob;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("/run")
    public Response run() {
        JobRunnerResponse result = mail2BlogJob.runJob(null);
        Mail2BlogJobRestResponse response = new Mail2BlogJobRestResponse();
        response.setMessage(result.getMessage());
        response.setStatus(result.getRunOutcome().name());
        return Response.ok(response).build();
    }
}
