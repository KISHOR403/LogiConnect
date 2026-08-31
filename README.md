# LogiConnect

[![LogiConnect CI](https://github.com/KISHOR403/LogiConnect/actions/workflows/ci.yml/badge.svg)](https://github.com/KISHOR403/LogiConnect/actions/workflows/ci.yml)

Enterprise Communication & Collaboration Platform for Logistics Operations.

---

## 1. What LogiConnect Is

**LogiConnect** is a secure, unified internal communication and operations collaboration platform designed specifically for logistics enterprises with ~2,000+ employees across administrative hubs, sorting centers, warehouses, and fleet operations.

## 2. Why It Exists

Currently, enterprise operational communication relies heavily on unmanaged WhatsApp groups, resulting in:
- Information fragmentation and loss of audit trails.
- Data governance and operational security compliance risks.
- Lack of role-based access control and organizational directory integration.
- Inability to tie operational chats to structured entities (departments, shifts, shipments, announcements, and official records).

**LogiConnect** replaces WhatsApp with a structured, compliant, high-availability platform tailored for logistics workflows.

## 3. Technology Stack

- **Frontend:** Next.js (App Router), React, TypeScript, Tailwind CSS
- **Backend:** Java 21, Spring Boot 3.x, Spring Security, Spring Data JPA, Hibernate, WebSocket (STOMP)
- **Database:** PostgreSQL 16
- **Infrastructure:** Docker, Docker Compose, Nginx Reverse Proxy
- **Architecture:** Modular Monolith with clean boundary separation

## 4. Project Structure

```
LogiConnect/
├── frontend/             # Next.js App Router frontend application
├── backend/              # Spring Boot modular monolith backend service
├── database/             # Flyway migrations and database seed configurations
├── infrastructure/       # Docker configurations, Nginx proxy, and monitoring
├── docs/                 # Enterprise architecture, API, security, and PRD specifications
├── scripts/              # Setup, database, dev, and deployment utility scripts
├── tests/                # System-level integration, API, security, and E2E test suites
├── .env.example          # Root environment template
├── .gitignore            # Git ignore specification
├── docker-compose.yml    # Multi-container orchestration specification
└── README.md             # Project overview and operational guide
```

## 5. Frontend & Backend Communication Model

- **REST API (`/api/v1/*`):** Stateless HTTP requests for CRUD operations, authentication, profile management, and document metadata handling.
- **WebSocket / STOMP (`/ws`):** Full-duplex real-time communication for instant messaging, group channel broadcasts, presence tracking, and dynamic notification dispatch.
