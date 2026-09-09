# Checkpoints

These are snapshots of only the four exercise files, not copies of the applications.

- `starter`: numbered TODO 1 (JMS send) and TODO 2 (SQL update) `.throwException(...)` placeholders. Compiles, but intentionally rejects sends/processing until edited.
- `queue`: completed session 1; B instances compete for `stock.work`.
- `topic`: completed session 2; each B consumes its own multicast subscription queue.

Use `bash scripts/checkpoint.sh starter|queue|topic` from the repository root.
The default app sources match `queue`, so they have no TODO placeholders until you load `starter` and type `YES`.
The script asks before replacing participant edits. Run `bash mvnw clean package`, wait for BUILD SUCCESS, then restart afterwards.
Reset broker and database state before moving between queue and topic exercises.
