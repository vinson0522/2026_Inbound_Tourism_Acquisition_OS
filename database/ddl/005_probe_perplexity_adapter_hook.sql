-- B-03b: Perplexity platform_adapter — real web UI hook patterns (EPIC-11)
UPDATE platform_adapter
SET
    dom_selectors_json = '{"input": "textarea, [contenteditable=''true''], #ask-input", "submit": "button[type=submit], button[aria-label=''Submit'']"}',
    api_patterns_json = '{"chatApi": "/rest/sse/perplexity_ask", "sseApi": "/rest/sse/perplexity_ask"}',
    parse_rules_json = '{"citationsPath": "citations", "answerPath": "answer"}',
    updated_at = NOW()
WHERE tenant_id = 1 AND platform = 'perplexity' AND version = '1.0';
