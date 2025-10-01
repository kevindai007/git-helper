# TODO – Structured LLM Output

Goal: Make LLM return a fixed, structured JSON result and add DTOs for parsing.

Tasks
- [ ] Update all prompt templates to request strict JSON (single object, no prose)
- [ ] Add DTOs: `LlmAnalysisReport`, `Finding`, `Location`, `Remediation`, `Stats`
- [ ] (Later) Add JSON parsing + validation + retry-on-parse-failure in service
- [ ] (Later) Wire posting findings to MR discussions using locations

Notes
- Keep existing prompt content; just append a strict "Output Format" section with schema and example.
- Keep schema stable and versioned via `schemaVersion`.
