package workshop;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

@ApplicationScoped
public class SendRest extends RouteBuilder {
    @Override
    public void configure() {
        restConfiguration()
            .component("platform-http")
            .bindingMode(RestBindingMode.json)
            .clientRequestValidation(true);
        rest("/changes")
            .post()
            .consumes("application/json")
            .produces("application/json")
            .type(SendRoute.Input.class)
            .outType(SendRoute.Event.class)
            .to("direct:send");
    }
}
