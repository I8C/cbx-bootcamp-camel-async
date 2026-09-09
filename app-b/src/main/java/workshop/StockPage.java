package workshop;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class StockPage extends RouteBuilder {
    @ConfigProperty(name = "stock.instance") String instance;
    public record Stock(String instance, int quantity, List<String> events) {}

    @Override
    @SuppressWarnings("unchecked")
    public void configure() {
        restConfiguration().component("platform-http").bindingMode(RestBindingMode.json);
        rest("/stock").get().produces("application/json").outType(Stock.class).to("direct:stock");

        from("direct:stock")
            .to("sql:classpath:stock.sql")
            .process(exchange -> {
                List<Map<String, Object>> rows = exchange.getMessage().getBody(List.class);
                int quantity = ((Number) rows.getFirst().get("quantity")).intValue();
                var ids = rows.stream().map(row -> (String) row.get("id")).filter(Objects::nonNull).toList();
                exchange.getMessage().setBody(new Stock(instance, quantity, ids));
            });
    }
}
