# Integration as code: cardboard boxes

Two one-hour sessions for ten people, each running their own system.
**This is a teaching POC. Keep every Java class and HTML page short and obvious.**
There is one product, two tiny applications, no frontend framework and no service/repository layers.

```text
App A: browser -> Camel REST -> Camel JMS -> Artemis
                                    |
                     App B: Camel JMS -> Camel SQL -> its database
                            browser <- Camel REST + Camel SQL
```

App A sends a change, such as `+10`. App B applies it to its own stock, initially 100.
Later, run B twice. A queue shares work; a topic gives each stock view a copy.

## Before the workshop

Install Git (including Git Bash), JDK 21 and Docker Desktop **or** Podman with a working Compose provider.
Start the container engine. For Podman on Windows, start its machine first.
An editor is enough; an IDE is optional. Maven 3.9.11 is downloaded by the supplied official Maven Wrapper.
Use **Git Bash** for every command below, from this folder. Do not run installation snippets from the reference image blindly.

```bash
# Podman users only; repeat in each infrastructure terminal:
export CONTAINER_ENGINE=podman

bash scripts/check.sh
bash scripts/containers.sh pull
bash mvnw -B clean package
bash scripts/containers.sh up -d
bash scripts/containers.sh logs --tail 20 artemis
```

Wait for `Server is now active` in the Artemis log. PostgreSQL should be healthy in `bash scripts/containers.sh ps`.
The first download/build can take several minutes; complete it before session 1.
Ensure `java -version` and `bash mvnw -version` both report Java 21. If needed, set `JAVA_HOME` to your JDK 21 directory and put `$JAVA_HOME/bin` first on `PATH`.

## Run the complete queue example

First build from this folder (also required after changing Java, HTML, SQL, properties or checkpoints):

```bash
bash mvnw clean package
```

Wait for **BUILD SUCCESS**, then open separate Git Bash terminals in this folder:

```bash
# Terminal 1
bash scripts/run.sh a
# Terminal 2
bash scripts/run.sh b1
# Terminal 3, only when the exercise asks for it
bash scripts/run.sh b2
```

- [App A: sender](http://localhost:8080)
- [App B1: stock](http://localhost:8081)
- [App B2: stock](http://localhost:8082)
- [Artemis console](http://localhost:8161/console): username `workshop`, password `workshop`

Send `10`: B1 becomes 110. The sender confirms **queued**, not processed.
`run.sh` only launches a packaged app; it does not build. It checks for a missing package and files newer than the package,
and prints `bash mvnw clean package` when a rebuild is needed. No rebuild is needed for a stop/restart without edits.
Stop each Java application with Ctrl+C in its terminal. `bash scripts/containers.sh stop` preserves data.
`bash scripts/reset.sh` explicitly deletes this workshop's container volumes and local transaction logs; stop all apps first.

`containers.sh` accepts Compose commands and forwards all following arguments:

```bash
bash scripts/containers.sh up -d
bash scripts/containers.sh stop
bash scripts/containers.sh down
bash scripts/containers.sh version
```

`stop` preserves containers and data; `down` removes containers but preserves named data volumes.
`bash scripts/containers.sh --help` (or no arguments) displays usage. Neither this script nor reset builds Java.

## Follow the exercises

Read [the participant guide](docs/participant-guide.md). The checkout initially contains the **queue solution**, ready to demonstrate.
Use `bash scripts/checkpoint.sh starter` to prepare the guided edits; `queue` and `topic` restore complete solutions.
Type `YES` when asked. Only **starter** contains the two numbered TODO placeholders; the default sources and solution checkpoints are complete.
Checkpoint selection replaces only the two route files and two application property files. Save your edits first.
Rebuild with `bash mvnw clean package`, then restart the apps after any edit.

Read [facilitator notes](docs/facilitator.md) for timing, expected stock values, troubleshooting and transaction explanation.

## What to read in the code

| File | Purpose |
| --- | --- |
| `app-a/.../SendRest.java` | Camel REST POST -> `direct:send` |
| `app-a/.../SendRoute.java` | Validate -> JSON -> JMS -> HTTP 202; `createEvent` holds processor logic |
| `app-b/.../StockRoute.java` | JMS -> JSON -> Camel SQL insert/update -> deliberate failure |
| `app-b/.../StockRest.java` | Camel REST GET -> `direct:stock` |
| `app-b/.../StockPage.java` | Camel SQL -> `createStockResponse` builds the response |
| `app-b/src/main/resources/stock.sql` | One consistent stock/event snapshot for the page |
| Each `META-INF/resources/index.html` | Plain browser controls and `fetch` |

`app-b/.../setup/JmsSetup.java`, the lower half of its properties and `infra/` are supplied infrastructure.
The REST classes are supplied for every checkpoint. `direct:` connects routes inside the same application.
Do not expand them into participant coding exercises. There are no JAX-RS resource classes or hand-written JDBC connections in the apps.

## Facilitator verification

Stop all apps. Tests use and reset **these workshop queues and databases**, so do not run them during an exercise.
Select the queue or topic solution, build, and leave the containers running:

```bash
bash mvnw -B clean package
bash mvnw -B -f verification/pom.xml test
bash verification/scripts-test.sh
```

The separate verification project starts/stops the packaged apps and checks successful commit, invalid input,
three failed attempts, rollback, DLQ, buffering, competing consumers, multicast copies and subscriber catch-up.
Logs go to `verification/target/`. Participant code has no test-harness dependency.
See [verification status](docs/verification.md) for the checks actually performed on the development machine.
