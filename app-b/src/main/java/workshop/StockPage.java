package workshop;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("/stock")
public class StockPage {
    @Inject DataSource database;
    @ConfigProperty(name = "stock.instance") String instance;
    public record Stock(String instance, int quantity, List<String> events) {}

    @GET
    @Produces("application/json")
    public Stock read() throws SQLException {
        try (var connection = database.getConnection()) {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
            int quantity;
            var events = new ArrayList<String>();
            try (var query = connection.prepareStatement("SELECT quantity FROM stock WHERE id = 1");
                 var rows = query.executeQuery()) {
                rows.next();
                quantity = rows.getInt(1);
            }
            try (var query = connection.prepareStatement("SELECT id FROM events ORDER BY sequence DESC LIMIT 10");
                 var rows = query.executeQuery()) {
                while (rows.next()) events.add(rows.getString(1));
            }
            connection.commit();
            return new Stock(instance, quantity, events);
        }
    }
}
