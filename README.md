# Integration as code: cardboard boxes

A hands-on workshop a small groups of colleagues, mostly non-developers, in two one-hour sessions.
Build a small integration by connecting a few readable Apache Camel route steps.
This is teaching POC code: one product, short Java classes, plain HTML pages and no frontend framework.

App A sends a stock change asynchronously through ActiveMQ Artemis. App B consumes it and updates
its PostgreSQL database. Its web page shows the stock, initially 100, and committed event IDs.
Running B twice makes the difference between competing consumers and independent subscribers visible.

```text
App A: browser -> Camel REST -> Camel JMS -> Artemis
                                              |
App B: browser <- Camel REST <- Camel SQL      |
                                  database <- Camel SQL <- Camel JMS
```

Session 1 introduces queues, buffering, transaction rollback, retries and the dead-letter queue.
Session 2 demonstrates diverging stock views with a shared queue, then multicast subscriptions and offline catch-up.
The stack is Java 21, Quarkus, Apache Camel, JMS, Artemis and PostgreSQL, with Docker or Podman Compose.

## Start here

- **Participants:** follow the [participant guide](docs/participant-guide.md) from its first preparation step through both sessions. It contains prerequisites, setup commands, exercises and expected results.
- **Facilitators:** read the [facilitator notes](docs/facilitator.md) alongside the participant guide for timing, verification, transaction explanations and troubleshooting.

## Repository structure

| Location | Contents |
| --- | --- |
| `app-a/` | Sender: `SendRest` defines HTTP; `SendRoute` creates the event and publishes to JMS; one plain HTML page |
| `app-b/` | Stock view: `StockRest` defines HTTP; `StockPage` reads through Camel SQL; `StockRoute` consumes JMS and updates the database; one plain HTML page |
| `app-b/.../setup/` | Supplied XA transaction configuration |
| `infra/` and `compose.yaml` | Artemis and PostgreSQL container configuration |
| `scripts/` | Environment checks, container commands, app startup, reset and checkpoint selection |
| `checkpoints/` | Starter placeholders and completed queue/topic exercise snapshots |
| `docs/` | Participant guide, facilitator notes and verification results |
| `verification/` | Facilitator checks for the messaging scenarios and scripts |
| `mvnw`, `mvnw.cmd` and `.mvn/` | Maven Wrapper for the pinned build tool |

The initial sources contain the completed queue example. The participant guide introduces it, then loads
the starter at the appropriate point. Checkpoints contain only the exercise files; the REST classes,
stock query, UI and transaction setup are shared. Follow the guide in order so the stock values and
checkpoint state match each exercise.
