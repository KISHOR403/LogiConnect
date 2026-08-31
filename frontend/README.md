# LogiConnect - Frontend

Next.js 14+ (App Router), TypeScript, and Tailwind CSS client application for the LogiConnect platform.

## Architecture

- `src/app/`: Route definitions grouped by route groups: `(auth)`, `(dashboard)`, and `admin`.
- `src/components/`: Reusable UI, layout, navigation, and domain components.
- `src/features/`: Feature-scoped business state and hooks.
- `src/services/`: REST API and WebSocket client communication services.
- `src/store/`: Global client state management.
- `src/types/`: TypeScript type and interface definitions.
