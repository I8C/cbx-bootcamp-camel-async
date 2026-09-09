package workshop;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

@ApplicationScoped
public class StockRest extends RouteBuilder {
    @Override
    public void configure() {
        restConfiguration().component("platform-http").bindingMode(RestBindingMode.json);
        rest("/stock").get().produces("application/json")
            .outType(StockPage.Stock.class).to("direct:stock");
    }
}
