# Facilitator notes

## Keep the workshop small

Ten individual local installations; two sessions of 60 minutes. Allow setup time before the sessions.
The only participant edits are two route lines in session 1 and two destination properties in session 2.
Open code at a large font. Explain one statement at a time. The edits connect a Camel JMS send and a Camel SQL update.
Keep the UI, the read-only page route and XA plumbing supplied.
Read `SendRest` -> `SendRoute` for sending, and `StockRest` -> `StockPage` for polling.
REST DSL is in its own class in each app; JMS consumption stays in `StockRoute`.
Each `.process(this::methodName)` calls a short method below the route in the same class.
Do not add an architecture layer, CSS library, product catalogue or generic event system.

Session 1: introduction 10 min, routes/buffering 15, failure 20, recovery 10, recap 5.
Session 2: second consumer 10 min, divergence 15, topic switch 15, catch-up 10, discussion 10.
If someone falls behind, stop their apps, select the appropriate checkpoint, rebuild and restart.

## Expected values

| Scenario | B1 | B2 |
| --- | --- | --- |
| Initial | 100 | not started |
| Queue +10, then buffered +5 | 115 | not started |
| Failed +7, three attempts then DLQ | 115 | not started |
| New successful +7 | 122 | not started |
| Reset, only B1 receives +10; only B2 receives +5 | 110 | 105 |
| Reset, topic +10 | 110 | 110 |
| B2 offline, topic +5 | 115 | offline, last display 110 |
| B2 returns | 115 | 115 |

## Supplied transaction plumbing

Camel JMS uses a Spring JMS listener container; a small `JtaTransactionManager` adapter delegates to Quarkus Narayana.
The JMS component's explicit JTA manager begins the transaction **before receive**, covering both Camel SQL endpoints.
Do not also enable Camel's local `transacted` flag: XA owns commit, and a second local commit conflicts with the JMS pool.
Pooled JMS enlists the Artemis XA resource. The PostgreSQL XA datasource enlists the SQL connection.
`CACHE_NONE` lets transaction-scoped JMS resources be acquired correctly. There is one consumer per instance.

On success, acknowledgement and SQL commit together. On an exception, both roll back.
The events insert is before the stock update, so throwing after the update proves that **both** changes roll back.
The event table uses the ID as primary key; this is not a general idempotent HTTP API or duplicate-event recovery solution.
The page uses one Camel SQL query (`stock.sql`) for stock and event IDs, so each poll gets one database snapshot.
The LEFT JOIN also returns the stock when the event list is empty.

The consumer does not mark failures as handled. `noErrorHandler()` leaves broker delivery attempts in charge.
Artemis has `max-delivery-attempts=3` (initial delivery plus two retries), a 2000 ms delay and `DLQ` as dead-letter address.
DLQ routing is the broker's action after repeated rollback, not a send within the failed database transaction.

Recovery is enabled. Each B instance has a stable unique node name and a separate persistent log directory.
PostgreSQL enables prepared transactions. Do not delete logs independently of database/broker state.
The reset script deletes all workshop resources together **after apps stop**. An abrupt-crash recovery lab is outside these two sessions.

## Broker console

Open `http://localhost:8161/console` and sign in with `workshop` / `workshop`.
Select **Artemis → Queues**, open the three-dot menu on the queue row, then choose **Browse Messages**.
Use `stock.work` while B is stopped; use `DLQ` after the third failed attempt.
In session 2, inspect `stock.events` and its two queues. With B2 stopped, `stock.b2` holds its copy.
Queue message counts may update more slowly than the browser pages; refresh the console.
If a previously empty Browse page stays empty, return to **Queues** and click its message count to reopen it.
Click the message ID to see the JSON body. Its event ID matches App A; the broker's numeric message ID is a different identifier.

## Troubleshooting

- **Wrong Java:** both `java -version` and `bash mvnw -version` must show 21. Set `JAVA_HOME` and `PATH` in Git Bash.
- **Maven download/proxy:** pre-download before the workshop; use the organisation's normal Maven proxy configuration when needed.
- **Docker/Podman unavailable:** start the engine (and Podman machine on Windows); check `docker info` or `podman info`.
- **Podman Compose:** a Compose provider must be installed; `podman compose version` must work before the session.
- **Container paths rewritten by Git Bash:** use the supplied container script, which disables MSYS path conversion for container arguments.
- **Address already in use:** ports 8080, 8081, 8082, 8161, 61616 and 5432 are required. Stop the conflicting local service before class.
- **Consumer does not start:** check Artemis is live and PostgreSQL is healthy. Read the first startup error, not just later retries.
- **Slow first start:** wait for `Listening on: http://127.0.0.1:...` in each terminal. Cold starts on some Windows machines take tens of seconds; start once before class.
- **B2 shows B1:** use `bash scripts/run.sh b2`; this sets its instance name, port, database and transaction log directory.
- **Both views behave like a queue:** verify the producer says `jms:topic:stock.events`, and B1/B2 consume distinct FQQNs; rebuild and restart.
- **Failure changes committed stock:** stop the exercise; check that `JmsSetup`, pooled JMS XA and JDBC XA configuration are present. Run verification.
- **Checkpoint seems ineffective:** type `YES` at the prompt and reopen the two source files. Only `starter` has TODO 1 and TODO 2.
- **Missing/outdated package:** stop the apps, run `bash mvnw clean package` in the workshop root, wait for BUILD SUCCESS, then run the apps again.
- **Starter sends fail:** intentional until both numbered `.throwException(...)` placeholders are replaced with the guide's `.to(...)` lines.
- **Container commands:** `bash scripts/containers.sh up -d`, `stop`, `down` and `version` pass directly to the selected Compose engine; use `--help` for usage.

## References

- [Artemis address model and FQQNs](https://artemis.apache.org/components/artemis/documentation/latest/address-model.html)
- [Camel Quarkus JMS and XA support](https://camel.apache.org/camel-quarkus/3.27.x/reference/extensions/jms.html)
- [Quarkus transaction management](https://quarkus.io/guides/transaction)
- [Quarkus XA datasource configuration](https://quarkus.io/guides/datasource)
- [Camel REST DSL on Quarkus](https://camel.apache.org/camel-quarkus/3.27.x/reference/extensions/platform-http.html)
- [Camel SQL with the Quarkus datasource](https://camel.apache.org/camel-quarkus/3.27.x/reference/extensions/sql.html)

The kit pins Quarkus/Camel Quarkus 3.27.2, Artemis extension 3.9.0, pooled JMS 2.8.0 via the platform,
Artemis broker 2.40.0, PostgreSQL 17.5, Maven 3.9.11 and Wrapper 3.3.4.
