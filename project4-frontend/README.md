# Project 4 frontend

Plain HTML, CSS and JavaScript served at **http://localhost:5500**. Project 3's
frontend is unchanged. Public API URLs are in `config.js`; no credentials or
internal service keys belong in browser files.

| Service | Local URL |
|---|---|
| Identity | http://localhost:8083 |
| Booking | http://localhost:8084 |
| Blood Request | http://localhost:8085 |
| Notification | http://localhost:8086 |

## Connected workflows

- Donor registration, verification, login, password recovery/change, profile,
  CPR/photo upload, donor onboarding, request filtering, slot booking and cancellation.
- Staff request creation/status changes, slots and booking review/status updates.
- Admin hospital/staff management, paginated user search, account activation and
  deactivation/reactivation. Activation does not bypass email verification.
  Administrator accounts are protected from deactivation.
- Notifications: authenticated, paginated own inbox at `/api/notifications/my`.
  The browser cannot choose a recipient email or access internal notification APIs.
- Live inbox: native WebSocket `/ws/notifications`; bearer authentication is sent
  in the first frame rather than the URL. Identity revalidates the active verified
  account before snapshots. The server checks for changes every three seconds.
  Reconnect reloads the latest persisted inbox; REST remains available on failure.
- Departments: authenticated users browse hospital departments; staff add a
  department only to their assigned active hospital.
- Appointment reminders: Booking checks every minute and queues one reminder per
  upcoming PENDING/CONFIRMED booking within 24 hours. The existing outbox/Kafka
  path delivers it to the inbox and SMTP worker. My Donations explains this.

## Required forward migration

`runtime/project4/S005__core_features.sql` adds only `p4_hospital_department` and
`p4_donation_reminder`. It has NOT been applied. Arrange a separate, verified
manual Project 4 upgrade before running the new department/reminder code. Do not
rerun the five existing migrations. No startup migration runner was added.

Reminder receipt and outbox insertion share a transaction; the booking primary
key prevents duplicate queuing across scheduler instances/restarts. Already
queued reminders cannot be recalled if a booking is later cancelled. The text
asks users to check current booking status. SMTP can still duplicate delivery
around a crash, as documented in the existing mail runbook.

## Configuration and safety

Use the dedicated Project 4 Compose setup, never Project 3/root Compose. CORS and
WebSocket origin checks allow the configured development frontend origin;
`127.0.0.1:5500` is not an alias for the approved origin. Use HTTPS/WSS when
hosting outside local development.

The API helper allowlists methods/routes. Protected calls use the JWT; saved
sessions are rechecked with Identity. Sign-out closes the socket and clears
account data. User-controlled text is escaped before display.

Existing uploads can be submitted, but authorized saved-file preview/download
is still unavailable. No unread/read notification controls or optional dashboard
were added. Department duplicate names within a hospital return a conflict.

These paths are connected in source and the existing safe unit tests are run;
new feature browser/live-deployment verification is still required after the
manual migration and a separately authorized application rollout. No running
application or database was changed during implementation.

For optional Gmail configuration see [Gmail instructions](../runtime/project4/GMAIL.md).
Mailpit remains the default; real SMTP was not used for testing.

Sharing an available request produces a current-origin `?request=ID` link. It opens
a public, read-only page with hospital, blood type, units, urgency, date and status;
closed/deleted requests are unavailable. Sign-in/registration keeps the request in
the URL and returns to it after login. Booking still requires an eligible donor.
Copy link, WhatsApp and (where supported) native sharing are available. Clipboard
and native sharing may require HTTPS; the displayed link can also be copied manually.
Local links are reachable only where that frontend host is accessible. External
sharing requires a public deployment, public API URLs in config.js and the deployed
frontend origin allowed by service CORS. New links then use that deployed origin;
previously copied localhost links do not change.

Hospital directions use exact normalized names, not partial matching. Salmaniya
Medical Complex uses its Google Maps address (6H8C+JVW, Road No 2904, Manama).
Bahrain Defence Force Hospital / BDF Hospital uses Wadi Al Sail, West Riffa;
the generic Military Hospital alias additionally requires that exact Bahrain location.
Unknown names intentionally have no directions button. These links open Google Maps
in a new tab without an API key or database updates. Sources:
[Salmaniya Maps listing](https://www.top-rated.online/cities/Manama/place/p/5076416/Salmaniya%2BMedical%2BComplex),
[BDF official contact and Google map](https://rms.bh/en/contact-us/).
