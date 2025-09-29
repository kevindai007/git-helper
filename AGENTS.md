This service automates analysis of GitLab Merge Request (MR) code changes and leverages a Large Language Model (LLM) to provide review comments.

Required configuration (via environment variables or application.yml):
- `GITLAB_URL`: GitLab API base URL, e.g. `https://gitlab.com/api/v4`
- `GITLAB_TOKEN`: GitLab access token (sent via `PRIVATE-TOKEN` header)
- `OPENAI_API_KEY`: API key used by Spring AI ChatClient

### 1. Core Flow Overview

1. Receive MR URL: an API endpoint accepts a GitLab MR URL.
2. Parse URL and fetch project info: extract group path, project path, and MR ID, then resolve Group ID and Project ID via GitLab API.
3. Get MR diffs: use Project ID and MR ID to fetch file diffs from GitLab API.
4. Analyze with LLM: submit diffs to the LLM to find issues, bottlenecks, and style problems.
5. Return analysis result: deliver the LLM’s review back to the caller.

-----

### 2. Step Two: Parse URL and Fetch Project Info

Target URL example:
`https://gitlab.com/kevin-group4154039/first_test_proj/-/merge_requests/1`

Parse from the URL:

- Group Path/Namespace: `kevin-group4154039`
- Project Path: `first_test_proj`
- Merge Request ID: `1`

#### Step 2.1: Get Group ID

Search by group path/namespace to find the GitLab Group and get its ID.

API Endpoint:

```bash
GET https://gitlab.com/api/v4/namespaces?search={GROUP_PATH}
```

cURL example:

```bash
curl --location 'https://gitlab.com/api/v4/namespaces?search=kevin-group4154039' \
--header 'PRIVATE-TOKEN: {GITLAB_TOKEN}'
```

Key response field:

- `id`: `116231355` (group_id)

#### Step 2.2: Get Project ID

Use the Group ID and project path to search for the project within the group and retrieve its ID.

API Endpoint:

```bash
GET https://gitlab.com/api/v4/groups/{GROUP_ID}/projects?search={PROJECT_PATH}
```

cURL example:

```bash
curl --location 'https://gitlab.com/api/v4/groups/116231355/projects?search=first_test_proj' \
--header 'PRIVATE-TOKEN: {GITLAB_TOKEN}'
```

Key response field:

- `id`: `74790264` (project_id)

-----

### 3. Steps Three & Four: Fetch Diffs and Analyze

#### Step 3.1: Get Merge Request Diffs

Using the Project ID and MR ID, fetch the file-level diff details.

API Endpoint:

```bash
GET https://gitlab.com/api/v4/projects/{PROJECT_ID}/merge_requests/{MR_ID}/diffs
```

cURL example:

```bash
curl --location 'https://gitlab.com/api/v4/projects/74790264/merge_requests/1/diffs' \
--header 'PRIVATE-TOKEN: {GITLAB_TOKEN}'
```

Key response fields (array):

- `diff`: unified diff format string containing code changes
- `new_path`: file path of the change

#### Step 3.2: Format Diff Content

Concatenate or format each item’s `diff` string into a single LLM-friendly text block. Example:

```
--- File: test.txt ---
@@ -1,4 +1,5 @@
+test file
 this is the original text

\n-we have done a lot of things\n\ No newline at end of file
+this is the new line
+we have done lots of things
...
--- File: test01.txt ---
@@ -0,0 +1 @@
+this is the second test file
...
```

#### Step 3.3: Call the LLM (see LlmAnalysisService)

Submit the formatted complete diff content as the prompt to the LLM API. In practice, `LlmAnalysisService` uses Spring AI `ChatClient` to perform the call.

Backend pseudo-code example:

```python
# Suppose LLM_SERVICE is a client to interact with the LLM
def analyze_merge_request_diff(formatted_diff_content):
    prompt = f"""
    Please perform a professional code review of the following GitLab Merge Request changes.
    Identify potential bugs, performance issues, security vulnerabilities, and style problems.
    List findings by file and provide actionable suggestions.

    --- Code Change Details ---
    {formatted_diff_content}
    """

    # Actually invoked by LlmAnalysisService via ChatClient
    llm_response = LLM_SERVICE.send_message(prompt)

    return llm_response.text  # LLM analysis result
```

### 4. API Design (Step One)

Endpoint: Submit Merge Request URL

| Field | Value |
| :--- | :--- |
| Method | `POST` |
| Path | `/api/v1/mr/analyze` |
| Request Body | `application/json` |

Note: The GitLab access token is read from server configuration (`gitlab.token`) and is not required in the request body.

Request Body example:

```json
{
  "mr_url": "https://gitlab.com/kevin-group4154039/first_test_proj/-/merge_requests/1"
}
```

Response Body example (success):

```json
{
  "status": "success",
  "mrUrl": "...",
  "analysisResult": "Detailed analysis content returned by the LLM, possibly a Markdown review report..."
}
```

Response Body example (failure):

```json
{
  "status": "failure",
  "mrUrl": "...",
  "analysisResult": null,
  "errorMessage": "Error message (e.g., Group not found ...)"
}
```

