# LogiConnect REST & WebSocket API Specification Index

This directory contains the authoritative API contracts and design specifications for the **LogiConnect** platform (`/api/v1`).

---

## Specification Documents

| Document | File Path | Scope & Purpose |
| :--- | :--- | :--- |
| **REST API Design** | [api-design.md](file:///d:/Project/LogiConnect/docs/api/api-design.md) | Complete endpoint specifications across all 18 domains with query/path parameters, authorization rules, success/error codes, and pagination schemes. |
| **DTO Contracts** | [dto-contracts.md](file:///d:/Project/LogiConnect/docs/api/dto-contracts.md) | Jakarta validation rules, field constraints, request payloads, response structures, and JSON samples. |
| **Security Matrix** | [authorization-matrix.md](file:///d:/Project/LogiConnect/docs/api/authorization-matrix.md) | Granular permission enforcement, RBAC mapping, and dynamic organizational access boundaries. |
| **OpenAPI 3.0.3 Spec** | [openapi.yaml](file:///d:/Project/LogiConnect/docs/api/openapi.yaml) | Machine-readable OpenAPI 3.0.3 specification with complete paths, parameters, envelopes, and DTO schemas. |
| **WebSocket Architecture**| [websocket-design.md](file:///d:/Project/LogiConnect/docs/api/websocket-design.md) | Future STOMP/SockJS real-time messaging, typing indicators, presence heartbeat, and Redis Pub/Sub multi-node scaling. |
