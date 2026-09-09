# Checkpoints

These are snapshots of only the four exercise files, not copies of the applications.

- `starter`: two clearly marked TODO processors to replace. Builds, but intentionally rejects sends/processing.
- `queue`: completed session 1; B instances compete for `stock.work`.
- `topic`: completed session 2; each B consumes its own multicast subscription queue.

Use `bash scripts/checkpoint.sh starter|queue|topic` from the repository root.
The script asks before replacing participant edits. Rebuild and restart afterwards.
Reset broker and database state before moving between queue and topic exercises.
