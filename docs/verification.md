# Verification

Verified on 9 September 2026 on Windows 11 with Temurin Java 21, Git Bash, Maven 3.9.11 and Docker Desktop 28.5.1.

| Check | Result |
| --- | --- |
| Both Quarkus applications build and package | Passed |
| Official Maven Wrapper runs in Git Bash | Passed |
| Starter route templates compile with Java 21 | Passed |
| Shell syntax and Docker Compose configuration | Passed |
| Successful JMS delivery commits stock and event ID | Passed |
| Invalid zero change is rejected | Passed |
| Failed SQL change rolls back stock and event ID | Passed |
| Exactly three processing attempts, then DLQ | Passed |
| Queue buffers while B is stopped | Passed |
| Two competing consumers receive one copy collectively | Passed |
| Two multicast subscriptions receive matching event IDs | Passed |
| Durable subscriber catches up after restart | Passed |
| Actual queue/topic checkpoint properties load correctly | Passed |
| No duplicate local JMS commit / closed-session warnings | Passed |
| Browser form sends +10; stock page changes from 100 to 110 | Passed |
| Browser failure checkbox leaves stock at 110; JSON visible in DLQ | Passed |
| Git Bash preflight and queue checkpoint restoration | Passed |
| Reset script recreates containers, clears XA logs, restores both stocks to 100 and event counts to zero | Passed |

The end-to-end test is one scenario covering the workshop progression: **1 test, 0 failures, 0 errors**.
Its latest run took about 48 seconds, excluding Maven startup. Reports and individual application logs are under `verification/target/`.
The explicit Spring JTA manager controls receive and commit; local JMS transactions must not also be enabled.

Podman is not installed on this machine, so its runtime was **not tested**. The scripts select `podman compose`
when `CONTAINER_ENGINE=podman`; a working Compose provider and Podman machine are prerequisites on Windows.
Crash recovery configuration is supplied, but abrupt-crash/XA recovery testing is outside the verified workshop scenarios.
Some extension deprecation/recorder warnings appear at build time; they do not prevent the tested applications from starting.

To reproduce: start containers, stop all apps, run `bash mvnw package`, then `bash mvnw -f verification/pom.xml test`.
The tests reset workshop queue/database contents before and after running. Do not run them during a participant exercise.
