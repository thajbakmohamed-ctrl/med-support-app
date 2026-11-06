# Med Support — Frontend

A responsive HTML/CSS/JavaScript frontend for the Med Support Spring Boot REST API.

## Run with your backend (recommended)

Copy `index.html`, `styles.css`, and `app.js` into your Spring Boot project at:

`src/main/resources/static/`

Run Spring Boot and open `http://localhost:8080/`.

Using the same origin avoids CORS issues. **Do not double-click index.html**; API calls require the running backend.

## Features

- Authentication: register, login, forgot password, email verification callback, password change
- Role-aware navigation for DONOR, HOSPITAL_STAFF, ADMIN (role selection in UI is **not** authorization; backend must enforce JWT roles)
- Hospital list and client-side name/location search
- Blood requests: view, blood-type filtering, sorting, create, status updates
- Donation bookings: create, view, cancel, update status
- Profile: edit name/phone, upload CPR and profile image, deactivate
- Admin: create/update/deactivate/reactivate hospitals, deactivate/reactivate users
- WebSocket booking status notifications via `/ws` and `/topic/donation-bookings`

## Important integration notes

- Based on the available Med Support project ZIP (Oct 7) and conversation updates. Confirm endpoint names, request DTO fields, status enums and response shape against your latest backend before production use.
- The UI selects a role after login for navigation; it does not decode/verify role claims. The backend remains the authorization source.
- The frontend uses external CDN scripts for SockJS and STOMP. For offline deployment, vendor these locally.
- WebSocket `/ws` was opened for testing; secure and authorize WebSocket subscriptions before production.
- The WebSocket notification feed is session-local, not a persisted notification inbox.
- Client-side hospital search is not a server-side search endpoint.
- Rate limiting, database seeding and audit logging are **backend features**, not UI actions.
- The backend ZIP does not expose a user-list endpoint, so admin user actions require a user ID.
- No server or browser integration tests have been run against your local running API.
