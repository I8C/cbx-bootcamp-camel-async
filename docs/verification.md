# Verification

Verified on 9 September 2026 on Windows 11 with Temurin Java 21, Git Bash, Maven 3.9.11 and Docker Desktop 28.5.1.

| Check | Result |
| --- | --- |
| Both Quarkus applications build and package | Passed |
| Official Maven Wrapper runs in Git Bash | Passed |
| Starter route templates compile with Java 21 | Passed |
| Shell syntax and Docker Compose configuration | Passed |
| Successful JMS delivery commits stock and event ID | Passed |
| Camel REST returns JSON and HTTP 202 after sending | Passed |
| Zero, out-of-range, malformed JSON and null requests return HTTP 400 | Passed |
| Failed SQL change rolls back stock and event ID | Passed |
| Exactly three processing attempts, then DLQ | Passed |
| Queue buffers while B is stopped | Passed |
| Two competing consumers receive one copy collectively | Passed |
| Two multicast subscriptions receive matching event IDs | Passed |
| Durable subscriber catches up after restart | Passed |
| Actual queue/topic checkpoint properties load correctly | Passed |
| No duplicate local JMS commit / closed-session warnings | Passed |
| Guide quotes exact starter placeholders and replacement lines | Passed |
| Queue/topic route snapshots agree | Passed |
| Container command/argument forwarding for Docker and Podman | Passed with command stubs |
| Missing/stale build rejection and instance selection in run.sh | Passed with disposable files and command stubs |
| All checkpoint copies and numbered starter TODOs | Passed |
| Browser form sends +10; stock page changes from 100 to 110 | Passed before Camel REST migration; unchanged HTML |
| Browser failure checkbox leaves stock at 110; JSON visible in DLQ | Passed before Camel REST migration; unchanged HTML |
| Git Bash preflight and queue checkpoint restoration | Passed |
| Reset script recreates containers, clears XA logs, restores both stocks to 100 and event counts to zero | Passed |

After separating the REST classes and extracting processor methods: **2 tests, 0 failures, 0 errors** (workshop progression and guide/checkpoint consistency).
Both apps build, and the sender starter compiles together with its shared REST class.
The latest run took about 59 seconds, excluding Maven startup. Reports and individual application logs are under `verification/target/`.
The separate `bash verification/scripts-test.sh` checks also passed. Actual Docker Compose `version` and configuration validation passed through `containers.sh`.
The migrated endpoints were checked over HTTP; the browser was not re-tested after this change.
The explicit Spring JTA manager controls receive and commit; local JMS transactions must not also be enabled.

Podman is not installed on this machine, so its runtime was **not tested**. The scripts select `podman compose`
when `CONTAINER_ENGINE=podman`; a working Compose provider and Podman machine are prerequisites on Windows.
Crash recovery configuration is supplied, but abrupt-crash/XA recovery testing is outside the verified workshop scenarios.
Some extension deprecation/recorder warnings appear at build time; they do not prevent the tested applications from starting.

To reproduce: select a solution checkpoint, start containers, stop all apps, run `bash mvnw clean package`, then `bash mvnw -f verification/pom.xml test` and `bash verification/scripts-test.sh`.
The tests reset workshop queue/database contents before and after running. Do not run them during a participant exercise.
