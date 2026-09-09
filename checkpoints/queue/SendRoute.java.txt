package workshop;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import java.util.UUID;

@ApplicationScoped
public class SendRoute extends RouteBuilder {
    public record Input(int change, boolean fail) {}
    public record Event(String id, int change, boolean fail) {}

    @Override
    public void configure() {
        restConfiguration().component("platform-http").bindingMode(RestBindingMode.json)
            .clientRequestValidation(true);
        onException(IllegalArgumentException.class).handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
            .setBody(constant("Enter a non-zero whole number between -1000 and 1000."));
        rest("/changes").post().consumes("application/json").produces("application/json")
            .type(Input.class).outType(Event.class).to("direct:send");

        from("direct:send")
            .process(exchange -> {
                var input = exchange.getMessage().getBody(Input.class);
                if (input == null || input.change() == 0 || Math.abs((long) input.change()) > 1000)
                    throw new IllegalArgumentException("Invalid change");
                exchange.getMessage().setBody(new Event(UUID.randomUUID().toString(), input.change(), input.fail()));
            })
            .setProperty("event", body())
            .marshal().json(JsonLibrary.Jackson)
            // Exercise 1: send the JSON to JMS (completed solution).
            .to("{{stock.destination}}?exchangePattern=InOnly&deliveryPersistent=true&jmsMessageType=Text")
            .setBody(exchangeProperty("event"))
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(202));
    }
}
