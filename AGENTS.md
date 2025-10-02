AGENTS.md is for coding agents. It complements README.md with detailed build steps, tests, conventions, and internal architecture that would clutter a human-focused README. If you’re building or using coding agents, you can adopt this pattern.

Why separate from README
- Keep README concise for human contributors (what it is, how to run it)
- Give agents a predictable place for instructions and context
- Provide precise, agent-focused guidance that complements README/docs

---

## Build & Run

Requirements
- JDK 21, Maven 3.9+
- PostgreSQL for Spring Data JPA

Environment
- LLM + GitLab:
  - `OPENAI_API_KEY` (Spring AI)
  - `GITLAB_URL` (e.g., `https://gitlab.com/api/v4`)
  - `GITLAB_TOKEN` (sent via `PRIVATE-TOKEN`)
- Database:
  - `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

Run
- `mvn spring-boot:run`
- Tests: `mvn test`

Spring config (src/main/resources/application.yml)
- Reads OpenAI key, GitLab URL/token, and JDBC settings from env vars

---

## Core Flow
1) Parse MR URL → group, project, MR id
2) Resolve Group/Project via GitLab API
3) Fetch MR diffs
4) Select prompt by type using strategy scoring
5) Call LLM → return structured JSON findings
6) Persist JSON report on MR row and per-finding rows

---

## Prompt Selection Architecture
- `MrContext`: precomputes file/line stats from diffs
- `PromptType` (enum): `GENERIC`, `JAVA`, `PYTHON`, `JAVASCRIPT`
- `PromptStrategy`: returns `type()` and `score(context)`
  - Implementations: `GenericStrategy`, `JavaStrategy`, `PythonStrategy`, `JavaScriptStrategy`
- `ScoreCalculator` + `PromptSelectionProperties`: configurable weights
- `PromptProvider`: maps `PromptType` → system prompt content
- `LlmAnalysisService#selectPromptForFiles`: build context → score → choose best type → fetch prompt

Configuration
- `prompt.selection.fileCountWeight` (default 20.0)
- `prompt.selection.lineWeight` (default 1.0)

Extension (add a new type)
- Add enum value to `PromptType`
- Create strategy (`PromptStrategy`) to score that type
- Provide the prompt via `PromptProvider` (or externalize to resource file)
- Optional: tune weights and add tests

---

## LLM Output Structure
Prompts end with an "Output Format (Strict)" section instructing models to return a single JSON object.

DTOs
- `LlmAnalysisReport`: `schemaVersion`, `promptType`, `findings[]`, `stats?`, `summaryMarkdown?`
- `Finding`: `id`, `severity`, `category`, `title`, `description`, `location`, `evidence?`, `remediation?`, `confidence?`, `tags?`
- `Location`: `file`,`line_type`, `startLine?`, `endLine?`, `startCol?`, `endCol?`
- `Remediation`: `steps?`, `diff?`
- `Stats`: `countBySeverity?`, `countByCategory?`

Recommended (when integrating parser)
- Validate JSON with a schema; retry on parsing failures
- Include model metadata in the report for traceability

---

## Persistence Model

Tables (see `db/mr_info.sql`)
- `mr_info`: MR metadata and the raw structured JSON report (`analysis_result` TEXT)
- `mr_analysis_detail`: per-finding rows linked to `mr_info` via `mr_info_id`
  - Columns include `rule_id`, `severity`, `category`, `title`, `description`, `file`, location fields, `evidence`, remediation fields, `confidence`, `tags_json`, timestamps

Entities
- `com.kevindai.git.helper.entity.MrInfoEntity`
- `com.kevindai.git.helper.entity.MrAnalysisDetailEntity`

Repositories
- `MrInfoEntityRepository`
- `MrAnalysisDetailRepository` (supports `deleteByMrInfoId`)

Service write path
- `MrAnalyzeService` orchestrates:
  - Loads MR details and diffs
  - Calls `LlmAnalysisService.analyzeDiff(...)` → `LlmAnalysisReport`
  - Saves JSON report into `mr_info.analysis_result`
  - Calls `LlmAnalysisService.persistAnalysisDetails(mrInfo, report)` to (re)populate `mr_analysis_detail`

Data lifecycle
- MR unchanged (same `sha`) and `analysis_result` present → skip LLM call
- On re-analysis for same MR row: details table is cleared for `mr_info_id` then re-inserted
- Deleting `mr_info` cascades and removes detail rows

Migration notes
- New deployments should run `db/mr_info.sql` to ensure `mr_analysis_detail` exists
- For existing DBs, add the new table and indexes; no changes to existing columns

---

## Diff Handling Notes
- Count added/removed lines from unified diff; ignore `@@`, `---`, `+++` markers
- Use `new_path` when available; fall back to `old_path`
- Skip generated/binary artifacts and large files when needed

---

## API (for reference)
- `POST /api/v1/mr/analyze`
  - Body: `{ "mr_url": "https://gitlab.com/<group>/<project>/-/merge_requests/<id>" }`
  - Returns: `{ status, mrUrl, analysisResult | errorMessage }`

---

## Conventions
- Java 21, Spring Boot 3.5, Lombok
- Keep changes minimal and focused; avoid unrelated refactors
- Log using `slf4j` (`@Slf4j`) at appropriate levels
- Add unit tests close to changed code; avoid adding new frameworks

---

## Troubleshooting
- OpenAI/GitLab credentials missing → set env vars as above
- Maven offline failures → ensure network access for dependency downloads
- JSON parse issues → enforce strict JSON in prompts; add a retry with “JSON only” instruction

---

## Extension Ideas (for agents)
- Selection
  - Per-type weight multipliers via config, label overrides, hybrid merges
- Prompting
  - Externalize prompts to `classpath:/prompts/{type}.md`, keep `PromptProvider` as fallback
- Parsing
  - Add JSON schema validation + retry loop; attach model metadata to report
- Posting
  - Map `mr_analysis_detail` locations to GitLab inline comments; update idempotently

