# LogiConnect - Database Architecture, Seeds & Tooling

PostgreSQL database schemas, development seed fixtures, and documentation for the LogiConnect enterprise logistics collaboration platform.

## Authoritative Migration Location

Flyway migrations are executed during Spring Boot startup and are strictly maintained in the backend resources directory:

```
backend/src/main/resources/db/migration/
└── V1__initial_schema.sql         # Authoritative PostgreSQL DDL Initial Migration
```

## Directory Structure

```
database/
├── seeds/
│   ├── development/
│   │   └── seed_data.sql          # Minimal development fixtures (Strictly Non-Production)
│   └── test/
│       └── .gitkeep               # Test fixtures placeholder
└── README.md
```

## Migration Workflow

1. Spring Boot automatically manages and validates schema migrations via Flyway on startup (`spring.jpa.hibernate.ddl-auto=validate`).
2. The initial migration file `V1__initial_schema.sql` configures all 21 relational tables, triggers, and partial indexes.

## Development Fixtures

- `database/seeds/development/seed_data.sql`:
  - **DEVELOPMENT ONLY**: Contains baseline roles (`SUPER_ADMIN`, `HR_ADMIN`, `MANAGER`, `TEAM_LEADER`, `EMPLOYEE`), permissions, logistics departments, teams, and test accounts.
  - Test accounts use a known development password hash (`Password@123`). Production environments must never use default or test credentials.
