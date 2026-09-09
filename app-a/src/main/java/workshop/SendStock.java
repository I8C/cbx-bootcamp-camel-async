package workshop;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import java.util.UUID;

@Path("/changes")
@Consumes("application/json")
@Produces("application/json")
public class SendStock {
    @Inject ProducerTemplate camel;

    public record Input(int change, boolean fail) {}
    public record Event(String id, int change, boolean fail) {}

    @POST
    public Response send(Input input) {
        if (input == null || input.change() == 0 || Math.abs((long) input.change()) > 1000) {
            throw new BadRequestException("Enter a non-zero whole number between -1000 and 1000.");
        }
        var event = new Event(UUID.randomUUID().toString(), input.change(), input.fail());
        camel.requestBody("direct:send", event);
        return Response.accepted(event).build();
    }
}
