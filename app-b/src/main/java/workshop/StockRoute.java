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
            .unmarshal()
            .json(JsonLibrary.Jackson)
            .setHeader("id", simple("${body[id]}"))
            .setHeader("change", simple("${body[change]}"))
            .setHeader("fail", simple("${body[fail]}"))
            .log("App B receives ${header.id}; fail=${header.fail}")
            .to("sql:INSERT INTO events (id, change) VALUES (:#id, :#change)")
            // TODO 2: replace the next line with the SQL update from the guide.
            .throwException(IllegalStateException.class, "TODO 2: update stock")
            .log("SQL executed for ${header.id}; fail=${header.fail}")
            .choice()
            .when(header("fail").isEqualTo(true))
                .throwException(IllegalStateException.class, "Workshop failure after SQL; roll back!")
            .end();
    }
}
