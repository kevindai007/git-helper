# MR Describe SSE 接口前端对接指南

本指南说明如何对接服务端的 MR 描述流式接口（SSE，Server-Sent Events），以实现实时分段输出渲染，契合 AI Agent 流式反馈的最佳实践。

- 接口地址：`POST /api/v1/mr/describe`
- 请求头：
  - `Content-Type: application/json`
  - `Accept: text/event-stream`
- 请求体：
  - `{ "mr_new_url": "<GitLab 新建 MR 的 URL>" }`
- 响应类型：`text/event-stream`（SSE）

注意：标准 `EventSource` 仅支持 GET。如果后端仅开放了 POST SSE（当前实现如此），请使用 `fetch` + `ReadableStream` 进行流式消费。若后端另行开放了 GET 版本（例如 `/api/v1/mr/describe/stream?mr_new_url=...`），则可用 `EventSource`（本文也给出示例）。

---

## 事件协议（Event Schema）
服务端使用命名事件（named events）。前端需要解析每个 SSE 事件帧：

- event: `start`
  - data（JSON）：
    - `mrNewUrl`：string，请求的 MR URL
    - `status`：`IN_PROGRESS`
    - `ts`：ISO 时间戳
    - `correlationId`：请求关联 ID（可用于日志与追踪）

- event: `delta`
  - data（string）：模型增量输出的文本片段（token/chunk）。前端应将这些片段顺序拼接并渲染。

- event: `error`
  - data（JSON）：
    - `message`：错误消息
    - `correlationId`：关联 ID
    - `ts`：ISO 时间戳
  - 收到后应终止本次会话并给出错误提示。

- event: `done`
  - data（JSON）：形如：
    ```json
    {
      "status": "SUCCESS",
      "mrNewUrl": "...",
      "description": "<最终聚合的 Markdown 文本>"
    }
    ```
  - 收到后应停止读取并完成渲染。

事件顺序通常为：`start` -> 多个 `delta` -> `done`（出现异常则 `error` 提前结束）。

---



## 请求与安全注意事项
- CORS：若前端与后端不同域名，请在后端开启相应的 CORS 配置（允许 `Content-Type`, `Accept`，并放行 `text/event-stream`）。
- 认证：如有网关/鉴权，按需在请求中携带凭证（如 Cookie / Authorization header）。SSE 建议避免使用需要复杂握手的认证方式。
- 连接管理：
  - POST + fetch 方案请使用 `AbortController` 在组件卸载或用户中断时取消请求。
  - 网络抖动可能导致连接中断，前端可在 `error` 后按需退避重试（幂等由服务端自行保证）。

## 渲染与 UX 建议
- 实时渲染：根据 `delta` 事件流式追加文本，配合“正在生成...”状态提示。
- Markdown 渲染：`done` 的 `description` 为 Markdown，建议在完成后再进行一次整体渲染以规避中间态的闪烁。
- 复制导出：提供“复制 Markdown”“导出 MD 文件”能力。

## 故障排查（Troubleshooting）
- 看到 `net::ERR_INCOMPLETE_CHUNKED_ENCODING` 或请求早退：
  - 检查是否使用了代理/CDN 并且对 `text/event-stream` 做了缓冲；
  - 确认请求头 `Accept: text/event-stream`；
  - 开启后端/网关的 keep-alive。
- 前端收不到事件：
  - 确认解析逻辑按 SSE 协议使用“空行分隔帧”，并处理 `event:` 与 `data:` 多行场景；
  - 确保没有将响应当作 JSON 一次性读取。

## 最小化对接清单
- [ ] 使用 `POST /api/v1/mr/describe`，请求头携带 `Accept: text/event-stream`。
- [ ] 解析事件：`start` / 多个 `delta` / `done`（或 `error`）。
- [ ] 渲染策略：`delta` 实时拼接，`done` 做最终收尾渲染。
- [ ] 断连处理：错误提示 + 视需要重试，或用户可手动重试。
- [ ] 取消能力：组件卸载或用户中断时调用 `AbortController.abort()`。

---

如需联调支持或需要 GET 版本的 SSE 端点（方便 `EventSource` 对接），请与服务端同学确认是否开放 `/api/v1/mr/describe/stream`。
