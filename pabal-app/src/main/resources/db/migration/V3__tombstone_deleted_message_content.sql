UPDATE message
SET content = '[deleted]'
WHERE status = 'DELETED'
  AND content <> '[deleted]';
