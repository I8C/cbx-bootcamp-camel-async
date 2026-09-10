# Participant guide

Follow this guide in order. Complete preparation before the two timed sessions.

## Before the workshop: prepare your computer

### 1. Install the tools and get the workshop

Install Git with Git Bash, JDK 21, and Docker Desktop **or** Podman with a working Compose provider.

* download Git-bash from https://git-scm.com/downloads,  
  use the default installation option  
  and open it to execute the next commands.
* install JDK 21 with SDKMan: 
    ```bash
    curl -s "https://get.sdkman.io" | bash
    sdk install java 21-tem  
    sdk use java 21-tem
    source "$HOME/.sdkman/bin/sdkman-init.sh“
    ```

An editor is enough; an IDE is optional. Obtain this repository from the facilitator and open Git Bash
in its root folder (the folder containing `README.md`, `mvnw` and `compose.yaml`).
All commands in this guide run from that folder. Maven 3.9.11 is downloaded by the supplied Maven Wrapper;
you do not need a separate Maven installation.

Set `JAVA_HOME` to your JDK 21 directory and put `$JAVA_HOME/bin` first on `PATH` if Java 21 is not already selected.
Find where java was installed with SDKMan run in Git-bash: `sdk home java 21-tem`.  
Start your container engine. Podman users on Windows must also start their Podman machine.
Ports 8080, 8081, 8082, 8161, 61616 and 5432 must be available.

### 2. Check tools and download dependencies

Podman users: run this in each Git Bash terminal used for container commands, checks or resets:

```bash
export CONTAINER_ENGINE=podman
```

Docker is the default. Then run:

```bash
bash scripts/check.sh
bash scripts/containers.sh pull
bash mvnw clean package
```

The check must succeed, and both its Java and Maven version output must report Java 21.
Wait for **BUILD SUCCESS**. Downloads can take several minutes; resolve installation or proxy issues
with the facilitator before the session.

### 3. Check infrastructure readiness

```bash
bash scripts/containers.sh up -d
bash scripts/containers.sh logs --tail 20 artemis
bash scripts/containers.sh ps
```

Wait for `Server is now active` in the Artemis log and a healthy PostgreSQL container.
Repeat the log/status commands if needed. Preparation is complete; stock changes begin in session 1.
You may leave containers running or preserve them with `bash scripts/containers.sh stop` until the session.

### Podman network recovery

If Podman reports `netavark` or `nftables` while starting the network, the failure is in the Podman machine,
before either workshop container starts. Restart the machine and try the infrastructure readiness commands again:

```bash
podman machine stop
podman machine start
podman info
bash scripts/containers.sh up -d
```

If the same network error remains, use Docker Desktop for this workshop instead. Start Docker Desktop, open a new
Git Bash terminal, run `unset CONTAINER_ENGINE`, then run `bash scripts/reset.sh`. Keep using the same container
engine for the remainder of the workshop.

### Commands used during the exercises

Keep each Java app in its own Git Bash terminal. Stop it with Ctrl+C. Each browser page talks only to its own backend.

**Build before run:** from the workshop folder, run `bash mvnw clean package` and wait for **BUILD SUCCESS**.
Then use `bash scripts/run.sh a`, `b1` or `b2` in separate terminals. The run script checks the package; it does not compile.
Rebuild after every source, UI, SQL, property or checkpoint change. Simply stopping/restarting an unchanged app needs no rebuild.

`containers.sh` forwards Compose commands such as `up -d`, `stop`, `down` and `version`.
`stop` preserves containers and data; `down` removes containers but preserves named volumes.
Use `bash scripts/containers.sh --help` for usage. Container commands do not build Java.

`reset.sh` deletes this workshop's broker/database data and XA logs, then starts fresh containers.
Stop all Java apps first and type `RESET` when prompted. Wait for infrastructure readiness as above afterwards.
`checkpoint.sh` replaces two route files and two property files; save any edits you want to keep and type `YES` when prompted.
Only `starter` has TODO placeholders. The completed `queue` and `topic` checkpoints are fallbacks described at the end of this guide.
Rebuild after loading any checkpoint.

## Session 1: queue, transaction, DLQ (60 minutes)

### 0–25: connect the route

Start with the starter. Stop all Java apps, then prepare fresh workshop data and the two route placeholders:

```bash
bash scripts/checkpoint.sh starter
bash scripts/reset.sh
```

Type `YES`, then `RESET`, at the prompts. Wait for Artemis to be ready again.
The starter copies the two route placeholders into the apps and tells you where **TODO 1** and **TODO 2** are.
The supplied REST classes and stock page query are shared by all checkpoints and need no edits.
Both stock databases start at 100.

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
Open [App A](http://localhost:8080) and [B1](http://localhost:8081). The [Artemis console](http://localhost:8161/console)
uses username `workshop` and password `workshop`. B2 is used in session 2.
Send `10`. B1 becomes **110** and shows the event ID. Match it with App A's confirmation.

Now trace the flow: the browser sends REST to `SendRest`, `direct:send` reaches `SendRoute`, Camel JMS sends
to Artemis, and `StockRoute` uses Camel SQL to update B1. `StockRest` and `StockPage` read the result for the browser.
App A sends a **change**, not the final stock value. The JavaScript only sends REST requests and reads REST responses;
it never connects to JMS. Processor logic is in `createEvent` and `createStockResponse` below their routes.

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
Save your two edited route files if you want to keep them.

## Session 2: one worker or every subscriber? (60 minutes)

### 0–10: two independent views

Stop A and B1, then reset the workshop data:

```bash
bash scripts/reset.sh
```

Wait for Artemis, then start A, B1 and B2 in three terminals. No rebuild is needed because the successful
session 1 route edits are unchanged. Open both B pages: **100 / 100**.
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

Restart A, B1 and B2.

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

## If you are stuck: completed checkpoints

The checkpoints are fallbacks, not normal workshop steps. Stop all Java apps first. They replace the two exercise
route files and their properties, so save any edits you want to keep. Type `YES` at the prompt, then rebuild.

Use the completed queue solution if you are stuck in session 1 or at the start of session 2:

```bash
bash scripts/checkpoint.sh queue
bash mvnw clean package
```

Use the completed topic solution if you are stuck after switching to publish-subscribe in session 2:

```bash
bash scripts/checkpoint.sh topic
bash mvnw clean package
```

If the stock values should return to 100 before continuing, run `bash scripts/reset.sh` after selecting the checkpoint,
then wait for Artemis and PostgreSQL to be ready. Start the appropriate apps again.
