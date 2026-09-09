package workshop;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.SQLException;

@ApplicationScoped
public class StockDatabase {
    @Inject DataSource database;
    public record Event(String id, int change, boolean fail) {}

    public void apply(Event event) throws SQLException {
        try (var connection = database.getConnection();
             var update = connection.prepareStatement("UPDATE stock SET quantity = quantity + ? WHERE id = 1");
             var insert = connection.prepareStatement("INSERT INTO events (id, change) VALUES (?, ?)")) {
            insert.setString(1, event.id());
            insert.setInt(2, event.change());
            insert.executeUpdate();
            update.setInt(1, event.change());
            update.executeUpdate();
            System.out.println("SQL executed for " + event.id() + "; fail=" + event.fail());
            if (event.fail()) throw new IllegalStateException("Workshop failure after SQL; roll back!");
        }
    }
}
