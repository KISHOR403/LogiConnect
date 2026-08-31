# LogiConnect - Database Architecture & Migrations

PostgreSQL database schemas, versioned Flyway migrations, and development seed fixtures for the LogiConnect enterprise logistics collaboration platform.

## Directory Structure

```
database/
├── migrations/
│   └── V1__initial_schema.sql         # Base initial schema (PostgreSQL DDL)
├── seeds/
│   ├── development/
│   │   └── seed_data.sql              # Minimal development fixtures
│   └── test/
│       └── .gitkeep                   # Test fixtures placeholder
└── README.md
```

## Migration Workflow (Flyway)

1. Migrations are located in `database/migrations/` and synchronized to `backend/src/main/resources/db/migration/`.
2. Initial version: `V1__initial_schema.sql`.
3. To run migrations via Flyway CLI or Spring Boot:
   - When Spring Boot starts with `spring.jpa.hibernate.ddl-auto=validate`, Flyway automatically executes pending SQL migrations in alphabetical/version sequence.

## Development Fixtures

- `database/seeds/development/seed_data.sql`:
  - Standard enterprise roles (`SUPER_ADMIN`, `HR_ADMIN`, `MANAGER`, `TEAM_LEADER`, `EMPLOYEE`).
  - Granular permissions mapped across modules.
  - Core logistics departments and operational teams.
  - Five non-production sample accounts with BCrypt-hashed credentials (`Password@123`).
  - Default company broadcast channels and sample acknowledgement tracking.
