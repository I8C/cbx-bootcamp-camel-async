# Participant guide

Use Git Bash in the workshop folder. Installation and image downloads happen before these sessions.
Keep App A and B1 in separate terminals. Each browser page talks only to its own backend.

**Build before run:** from the workshop folder, run `bash mvnw clean package` and wait for **BUILD SUCCESS**.
Then use `bash scripts/run.sh a`, `b1` or `b2` in separate terminals. The run script checks the package; it does not compile.
Rebuild after every source, UI, SQL, property or checkpoint change. Simply stopping/restarting an unchanged app needs no rebuild.

## Session 1: queue, transaction, DLQ (60 minutes)

### 0–10: trace one message

The initial checkout is the **completed queue solution**, so you can first see it work. Start infrastructure with
`bash scripts/containers.sh up -d`, build with `bash mvnw clean package`, then run A and B1 in separate terminals.
Open App A on port 8080 and B1 on 8081. Point to the sender, broker, consumer, database and browser.
Stock starts at 100. App A sends a **change**, not the final stock value.
The JavaScript only sends a REST request or reads a REST response; it never connects to JMS.
Both endpoints use Camel REST DSL, and the database steps use Camel SQL.
`SendRest.java` forwards the POST to `SendRoute.java`; `StockRest.java` forwards the GET to `StockPage.java`.
The `direct:` endpoints connect these routes inside each app. REST definitions stay separate from JMS and SQL steps.
Processor logic is in named methods below the routes: `createEvent` and `createStockResponse`.

### 10–25: connect the route

Stop the apps, then prepare the starter:

```bash
bash scripts/checkpoint.sh starter
```

Type `YES` at the checkpoint prompt. It copies the starter files into the apps and tells you where **TODO 1** and **TODO 2** are.
The supplied REST classes and stock page query are shared by all checkpoints and need no edits.
If you see completed `.to(...)` steps instead, you are still looking at the queue/topic solution: load `starter` and reopen the source files.

In `app-a/src/main/java/workshop/SendRoute.java`, find this exact line under **TODO 1**:

```java
.throwException(IllegalStateException.class, "TODO 1: send to JMS")
```

Replace only that line with:

```java
.to("{{stock.destination}}?exchangePattern=InOnly&deliveryPersistent=true&jmsMessageType=Text")
```

Read it as: send this JSON to the configured JMS destination. The extra options mean one-way, persistent, readable text.

In `app-b/src/main/java/workshop/StockRoute.java`, find this exact line under **TODO 2**:

```java
.throwException(IllegalStateException.class, "TODO 2: update stock")
```

Replace only that line with:

```java
.to("sql:UPDATE stock SET quantity = quantity + :#change WHERE id = 1")
```

Read it as: add the message's change to stock. Camel binds `:#change` from the message header as an SQL parameter.
The supplied preceding SQL step records the event ID. Both SQL steps are inside the JMS/XA transaction.
Neither replacement ends with a semicolon: leave the following route steps in place. Complete both edits before sending messages.

```bash
bash mvnw clean package
```

Restart A and B1 using `bash scripts/run.sh a` and `bash scripts/run.sh b1` in separate terminals.
Send `10`. B1 becomes **110** and shows the event ID. Match it with App A's confirmation.

Stop B1 with Ctrl+C. Send `5`. App A still accepts it. In the broker console, inspect the `stock.work` queue.
Restart B1: stock becomes **115**. The queue held work while the consumer was offline.

### 25–45: make a transaction fail

In App A, enter `7`, check **Simulate processing failure**, then Send.
Watch B1's terminal: `SQL executed for ...; fail=true` appears **three times**, approximately two seconds apart.
Each attempt then throws the workshop exception. This log shows an attempted update, not a commit.

B1 remains **115**, and the failed ID is absent from its committed list.
In the broker console, select the `DLQ` queue and browse its message. Inspect the JSON ID and failure flag.
The database changes and JMS acknowledgement rolled back together. Artemis retried and finally moved the message.

### 45–55: recover by sending a valid change

Uncheck failure and send `7` again. This is a **new event**, not a replay of the DLQ entry.
B1 becomes **122**. The failed message remains in the DLQ for inspection.
Do not retry the unchanged DLQ message: its failure flag would make it fail again.

### 55–60: recap

Explain in your own words: why does “queued” differ from “processed”? Why did stock stay unchanged on failure?
Save your two edited route files if you want to keep them. The `queue` checkpoint contains the solution.

## Session 2: one worker or every subscriber? (60 minutes)

### 0–10: two independent views

Stop A and B1. Reset and select the queue solution:

```bash
bash scripts/reset.sh
bash scripts/checkpoint.sh queue
bash mvnw clean package
```

Wait for Artemis, then start A, B1 and B2 in three terminals. Open both B pages: **100 / 100**.
They use identical code, but different databases. They represent independent copies of stock.

### 10–25: observe the wrong pattern for copies

Send `10`. Exactly one view becomes **110**; the other stays **100**.
The shared queue gives each event to one worker. Several sends may be unevenly distributed; alternation is not promised.

For a completely predictable demonstration, stop **all three apps**, run `bash scripts/reset.sh`, then start only A and B1.
No rebuild is needed here because no code or properties changed.
Send `10`: B1 becomes **110**. Stop B1, start B2 and send `5`: B2 becomes **105**.
Restart B1: the pages show **110 / 105**, even though both are connected.
No queue messages remain to repair either view. Each page correctly reports only its own committed messages.

### 25–40: give each stock view a copy

Stop all apps and run `bash scripts/reset.sh`. Reset matters: changing the pattern does not repair old missing events.

Change one line in `app-a/src/main/resources/application.properties`:

```properties
stock.destination=jms:topic:stock.events
```

Change one line in `app-b/src/main/resources/application.properties`:

```properties
stock.source=jms:queue:stock.events::stock.${stock.instance}
```

Artemis already has a multicast address `stock.events` with two durable subscription queues, `stock.b1` and `stock.b2`.
The part after `::` selects the instance's subscription queue. Although consumption still says `jms:queue`,
the **producer publishes to a topic and Artemis copies each event into both subscription queues**.
This avoids writing subscription setup code in the exercise.

```bash
bash mvnw clean package
```

Restart A, B1 and B2. If needed, `bash scripts/checkpoint.sh topic` supplies the same solution; rebuild afterwards.

### 40–50: compare and catch up

Send `10`: both views become **110**, with the **same event ID**.
Stop B2, send `5`: B1 becomes **115**. B2's page reports disconnection and displays its last known value.
Restart B2: its durable subscription supplies the missing event, and B2 becomes **115** too.

### 50–60: explain the choice

- **Anycast / queue:** one event goes to one competing worker. Useful when workers share a single job responsibility.
- **Multicast / topic:** each independent subscription gets a copy. Useful when independent applications each need every event.
- Each subscriber commits independently. The views converge after successful processing; simultaneous updates are not guaranteed.
- Two browsers pointed at the same backend would not demonstrate this distinction. We ran two backends with separate databases.

Stop all Java applications. Use `bash scripts/containers.sh stop` to preserve the exercise, or reset it for the next run.
