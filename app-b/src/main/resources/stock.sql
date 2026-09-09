-- One query gives the page a consistent snapshot, including when no events exist.
SELECT stock.quantity, recent.id
FROM stock
LEFT JOIN (
    SELECT id, sequence FROM events ORDER BY sequence DESC LIMIT 10
) recent ON true
WHERE stock.id = 1
ORDER BY recent.sequence DESC
