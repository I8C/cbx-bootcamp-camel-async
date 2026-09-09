package workshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSContext;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

// Facilitator verification only. Participant applications do not depend on this code.
class WorkshopTest {
    final Path root = Path.of("..").toAbsolutePath().normalize();
    final ObjectMapper json = new ObjectMapper();
    final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    final Map<String, Process> apps = new HashMap<>();
    final List<Path> logs = new ArrayList<>();

    @Test
    void guideMatchesCheckpointEdits() throws Exception {
        String guide = Files.readString(root.resolve("docs/participant-guide.md"));
        for (String route : List.of("SendRoute", "StockRoute")) {
            String starter = Files.readString(root.resolve("checkpoints/starter/" + route + ".java.txt"));
            String solution = Files.readString(root.resolve("checkpoints/queue/" + route + ".java.txt"));
            String placeholder = starter.lines().filter(line -> line.contains(".throwException(") && line.contains("TODO"))
                .findFirst().orElseThrow().strip();
            String replacement = solution.lines().filter(line -> line.contains(".to(\"{{stock.destination}}")
                || line.contains(".to(\"sql:UPDATE")).findFirst().orElseThrow().strip();
            assertTrue(guide.contains(placeholder), "Guide must show the exact starter line");
            assertTrue(guide.contains(replacement), "Guide must show the exact solution line");
            assertEquals(solution, Files.readString(root.resolve("checkpoints/topic/" + route + ".java.txt")));
        }
    }

    @Test
    void workshopScenarios() throws Exception {
        // Refuse to disturb an existing workshop session.
        for (int port : List.of(8080, 8081, 8082)) {
            try (var socket = new java.net.ServerSocket(port)) { }
        }
        try {
            reset();
            start("a", false);
            start("b1", false);
            assertEquals(100, quantity(8081));
            assertEquals(0, stock(8081).get("events").size());
            assertTrue(get(8081, "/stock").headers().firstValue("content-type").orElse("").contains("application/json"));
            String first = send(10, false);
            await(() -> quantity(8081) == 110, "successful database commit");
            assertTrue(stock(8081).get("events").toString().contains(first));
            assertEquals(400, post("{\"change\":0,\"fail\":false}").statusCode());
            assertEquals(400, post("{\"change\":1001,\"fail\":false}").statusCode());
            assertEquals(400, post("{broken json").statusCode());
            assertEquals(400, post("null").statusCode());

            String failed = send(7, true);
            await(() -> dlqContains(failed), "failed message in DLQ");
            assertEquals(110, quantity(8081));
            assertFalse(stock(8081).get("events").toString().contains(failed));
            String log = Files.readString(logs.getLast());
            assertEquals(3, log.lines().filter(line -> line.contains("SQL executed for " + failed)).count());

            stop("b1");
            send(5, false);
            start("b1", false);
            await(() -> quantity(8081) == 115, "buffered queue message");

            // Both consumers running: one message must go to exactly one view.
            start("b2", false);
            String split = send(3, false);
            await(() -> quantity(8081) + quantity(8082) == 218, "competing delivery");
            boolean got1 = stock(8081).get("events").toString().contains(split);
            boolean got2 = stock(8082).get("events").toString().contains(split);
            assertTrue(got1 ^ got2, "Only one consumer receives a queue message");
            stopAll();

            reset();
            start("a", true);
            start("b1", true);
            start("b2", true);
            String copied = send(10, false);
            await(() -> quantity(8081) == 110 && quantity(8082) == 110, "multicast copies");
            assertTrue(stock(8081).get("events").toString().contains(copied));
            assertTrue(stock(8082).get("events").toString().contains(copied));
            stop("b2");
            send(5, false);
            await(() -> quantity(8081) == 115, "online subscriber");
            start("b2", true);
            await(() -> quantity(8082) == 115, "durable subscriber catch-up");
            stopAll();
            for (Path applicationLog : logs) {
                String contents = Files.readString(applicationLog);
                assertFalse(contents.contains("SynchedLocalTransactionFailedException"), applicationLog.toString());
                assertFalse(contents.contains("The session has already been closed"), applicationLog.toString());
            }
        } finally {
            stopAll();
            reset();
        }
    }

    void start(String name, boolean topic) throws Exception {
        String module = name.equals("a") ? "app-a" : "app-b";
        Path log = root.resolve("verification/target/" + name + "-" + logs.size() + ".log");
        logs.add(log);
        var command = new ArrayList<String>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-Xmx192m");
        Path config = root.resolve("checkpoints/" + (topic ? "topic" : "queue") + "/" + module + ".properties");
        command.add("-Dquarkus.config.locations=" + config.toUri());
        command.addAll(List.of("-jar", module + "/target/quarkus-app/quarkus-run.jar"));
        var builder = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("INSTANCE", name);
        builder.environment().put("HTTP_PORT", name.equals("b2") ? "8082" : "8081");
        builder.environment().put("TX_DIRECTORY", ".runtime/test-tx-" + name);
        apps.put(name, builder.start());
        int port = name.equals("a") ? 8080 : name.equals("b1") ? 8081 : 8082;
        await(() -> {
            if (!apps.get(name).isAlive()) throw new IllegalStateException(name + " exited; inspect " + log);
            try { return get(port, "/").statusCode() == 200; } catch (Exception e) { return false; }
        }, name + " startup; inspect " + log, 120);
    }

    void stop(String name) throws Exception {
        Process process = apps.remove(name);
        if (process != null) {
            process.destroy();
            if (!process.waitFor(20, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor();
            }
        }
    }

    void stopAll() throws Exception {
        for (String name : new ArrayList<>(apps.keySet())) stop(name);
    }

    HttpResponse<String> get(int port, String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(3)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> post(String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:8080/changes"))
            .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    String send(int change, boolean fail) throws Exception {
        var result = post("{\"change\":" + change + ",\"fail\":" + fail + "}");
        assertEquals(202, result.statusCode(), result.body());
        assertTrue(result.headers().firstValue("content-type").orElse("").contains("application/json"));
        return json.readTree(result.body()).get("id").asText();
    }

    JsonNode stock(int port) {
        try { return json.readTree(get(port, "/stock").body()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    int quantity(int port) { return stock(port).get("quantity").asInt(); }

    boolean dlqContains(String id) {
        try (var factory = new ActiveMQConnectionFactory("tcp://localhost:61616" );
             var context = factory.createContext("workshop", "workshop");
             var browser = context.createBrowser(context.createQueue("DLQ"))) {
            var messages = browser.getEnumeration();
            while (messages.hasMoreElements()) {
                if (((jakarta.jms.Message) messages.nextElement()).getBody(String.class).contains(id)) return true;
            }
            return false;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    void reset() throws Exception {
        try (var factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
             var context = factory.createContext("workshop", "workshop", JMSContext.AUTO_ACKNOWLEDGE)) {
            for (String queue : List.of("stock.work", "stock.events::stock.b1", "stock.events::stock.b2", "DLQ")) {
                try (var consumer = context.createConsumer(context.createQueue(queue))) {
                    while (consumer.receive(300) != null) { }
                }
            }
        }
        for (String db : List.of("b1", "b2")) {
            try (var connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/" + db, "workshop", "workshop");
                 var sql = connection.createStatement()) {
                sql.executeUpdate("DELETE FROM events");
                sql.executeUpdate("UPDATE stock SET quantity = 100");
            }
        }
    }

    void await(BooleanSupplier condition, String description) throws Exception {
        await(condition, description, 40);
    }

    void await(BooleanSupplier condition, String description, int seconds) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(seconds).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(250);
        }
        fail("Timed out: " + description);
    }
}
