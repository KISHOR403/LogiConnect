# LogiConnect - Database

PostgreSQL database schemas, versioned Flyway migrations, and development seed data.

## Migration Files

- `V001__initial_schema.sql`: Base extensions, schemas, and common metadata
- `V002__users.sql`: User accounts and authentication credentials
- `V003__employees.sql`: Employee directory profiles and logistics roles
- `V004__departments.sql`: Department units and organizational hierarchy
- `V005__roles_permissions.sql`: RBAC system permissions and role mappings
- `V006__conversations.sql`: One-on-one and group conversation threads
- `V007__messages.sql`: Messages, delivery statuses, and attachments
- `V008__channels.sql`: Department and operational channels
- `V009__announcements.sql`: Enterprise broadcast notices and acknowledgements
- `V010__meetings.sql`: Meeting scheduling, shift handoffs, and video links
- `V011__documents.sql`: Operational documents, policy files, and attachments
- `V012__notifications.sql`: User notifications and delivery records
- `V013__audit_logs.sql`: Security and activity audit logging
