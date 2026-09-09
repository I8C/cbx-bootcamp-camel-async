package workshop;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

@ApplicationScoped
public class StockRoute extends RouteBuilder {
    @Override
    public void configure() {
        errorHandler(noErrorHandler()); // Artemis owns retries and the DLQ.
        from("{{stock.source}}")
            .unmarshal().json(JsonLibrary.Jackson)
            .setHeader("id", simple("${body[id]}"))
            .setHeader("change", simple("${body[change]}"))
            .setHeader("fail", simple("${body[fail]}"))
            .to("sql:INSERT INTO events (id, change) VALUES (:#id, :#change)")
            // Exercise 2: update stock with Camel SQL (completed solution).
            .to("sql:UPDATE stock SET quantity = quantity + :#change WHERE id = 1")
            .log("SQL executed for ${header.id}; fail=${header.fail}")
            .choice().when(header("fail").isEqualTo(true))
                .throwException(IllegalStateException.class, "Workshop failure after SQL; roll back!")
            .end();
    }
}
