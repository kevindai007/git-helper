# Git Helper – GitLab MR LLM Reviewer

A small Spring Boot service that analyzes GitLab Merge Request (MR) diffs with an LLM and returns structured review findings.

- Parses an MR URL, resolves project info, and fetches diffs from GitLab
- Selects a language/domain-specific review prompt automatically
- Calls an LLM and returns a structured JSON report of findings

## Quick Start

Requirements
- JDK 21, Maven 3.9+
- A PostgreSQL instance (used by Spring Data JPA)
- API keys/tokens:
  - `OPENAI_API_KEY` (Spring AI)
  - `GITLAB_URL` (e.g., `https://gitlab.com/api/v4`)
  - `GITLAB_TOKEN` (GitLab access token)

Recommended environment variables
- Database: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- LLM + GitLab: `OPENAI_API_KEY`, `GITLAB_URL`, `GITLAB_TOKEN`

Run
- `mvn spring-boot:run`

## API

Endpoint: `POST /api/v1/mr/analyze`

Request
```
{
  "mr_url": "https://gitlab.com/<group>/<project>/-/merge_requests/<id>"
}
```

Response (success)
```
{
  "status": "success",
  "mrUrl": "...",
  "analysisResult": { /* structured JSON report from the LLM */ }
}
```

Response (failure)
```
{
  "status": "failure",
  "mrUrl": "...",
  "analysisResult": null,
  "errorMessage": "..."
}
```

## Contributing

- Keep changes focused and small
- Add/adjust tests where applicable: `mvn test`
- Follow existing code style and package structure

For deeper build details, conventions, and agent-specific guidance, see `AGENTS.md`.

