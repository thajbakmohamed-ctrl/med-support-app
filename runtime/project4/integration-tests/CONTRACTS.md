# Project 4 Batch 1 frozen baseline

This records the Batch 1 behavior and the later rescheduling endpoint. Changes
to this baseline require deliberate contract review. Production source, applied
migrations, Compose and Project 3 are not changed by this module.

## Booking HTTP contracts

All paths below require a bearer user token. Controllers call Identity to verify
the user; staff operations are scoped to their hospital. Standalone contract tests
mock those remote clients and do not claim to test JWT cryptography or the full
Spring Security filter chain.

| Method | Path | Request | Success |
|---|---|---|---|
| POST | `/api/donation-bookings` | JSON below | 201 Booking |
| PUT | `/api/donation-bookings/{id}/reschedule` | Booking JSON below with the same blood request and a different slot; owning donor | 200 Booking |
| GET | `/api/donation-bookings/my` | Donor token | 200 Booking[] |
| DELETE | `/api/donation-bookings/{id}` | Donor token | 200 Booking (not 204) |
| PUT | `/api/donation-bookings/{id}/status` | `status` query parameter; staff | 200 Booking |
| GET | `/api/donation-bookings/blood-request/{id}` | Staff of owning hospital | 200 Booking[] |
| POST | `/api/donation-slots` | `slotDate,startTime,endTime,capacity`; staff | 201 Slot |
| PUT | `/api/donation-slots/{id}/capacity` | `capacity` query parameter; staff | 200 Slot |
| GET | `/api/donation-slots/my-hospital` | Staff token | 200 Slot[] |
| GET | `/api/donation-slots/available` | `bloodRequestId` query; donor profile | 200 Slot[] |

Booking request:

```json
{"donorProfileId":2,"bloodRequestId":3,"donationSlotId":4,"donationBookingDate":"2099-04-01","donationBookingTime":"10:00:00"}
```

Booking response fields: `id, donorProfileId, bloodRequestId, donationSlotId,
donationBookingDate, donationBookingTime, donationBookingStatus, createdAt,
updatedAt`. No donor medical/contact details or file paths.

Slot response fields: `id, hospitalId, slotDate, startTime, endTime, capacity,
reservedCount, status, createdAt, updatedAt`. Nullable timestamps remain valid.
Dates use ISO local dates; times are local times without an offset. No timezone
semantics are changed in this baseline.

Validation returns 400; missing authentication 401; wrong ownership 403; missing
resources 404; capacity, duplicate and invalid-transition conflicts 409. Booking
exception JSON uses `status` and `message`. Exact messages are not frozen unless
tested. Some downstream clients currently map 403 to 401: no normalization here.

Rescheduling is allowed for an upcoming `PENDING` or `CONFIRMED` booking before
its reminder is queued. The destination must be a future, open slot at the same
hospital, within the request deadline, with capacity. The move preserves the
booking ID, transfers the reservation atomically, resets status to `PENDING`,
and queues `BOOKING_RESCHEDULED`. A full destination returns 409 and preserves
the original booking and reservation.

Frontend support is narrower than backend support: capacity updates are not yet
frontend-routed/CORS-enabled. No close-slot endpoint exists. `newStatus` and the
legacy PUT cancellation URL are not aliases for the current contracts.

## Other supported HTTP interfaces (documented inventory)

This inventory is frozen from controllers. Automated exhaustive coverage of these
non-booking APIs is outside this batch; existing unit/CORS tests remain relevant.

| Owner | Methods and routes | Current contract |
|---|---|---|
| Identity | POST `/api/auth/register`, `/login` | Donor-only public registration; login returns token/email/role |
| Identity | GET `/api/auth/verify-email?token=...` | Verification result; invalid/expired 400 |
| Identity | POST `/api/auth/forgot-password?email=...`, `/reset-password` | Generic recovery response; reset JSON `passwordResetToken,newPassword` |
| Identity | PUT `/api/auth/change-password` | JSON `currentPassword,newPassword`; authenticated |
| Identity | GET/PUT/DELETE `/api/users/me` | UserResponse / name+phone update / self-deactivation 204 |
| Identity | POST `/api/users/staff`; PUT `/api/users/{id}/hospital/{hospitalId}` | Admin-only staff creation/assignment |
| Identity | POST `/api/users/profile/cpr`, `/image` | Multipart `file`; upload receipt |
| Identity | POST `/api/donor-profile`; GET/PUT `/api/donor-profile/me` | Own donor profile; missing profile 404 |
| Identity | GET `/api/internal/donors/{id}/email` | Internal service key required; email DTO |
| Blood Request | POST/GET `/api/blood-requests` | Staff creation / admin paginated list |
| Blood Request | GET/PUT/DELETE `/api/blood-requests/{id}` | Admin/staff detail; own-hospital staff edit/delete; delete 204 |
| Blood Request | PATCH `/api/blood-requests/{id}/status?status=...` | Own-hospital staff transition |
| Blood Request | GET `/api/blood-requests/open`, `/open/filter?bloodType=...`, `/open/{id}` | Donor list/filter/detail |
| Blood Request | GET `/api/blood-requests/hospital` | Own-hospital staff list |
| Blood Request | GET `/api/hospitals`, `/{id}` | Public list/detail |
| Blood Request | POST `/api/hospitals`; PUT `/{id}`; PATCH `/{id}/activate`, `/{id}/deactivate` | Admin mutation; creates inactive hospital |
| Notification | POST `/api/notifications/internal` | Internal service key; create notification |
| Notification | POST `/api/notifications/internal/{id}/send` | Internal service key; send processing result |
| Notification | GET `/api/notifications/internal/recipient?email=...`, `/booking/{id}`, `/pending` | Internal service key; arrays |

Blood request response fields: `id,hospitalId,neededDate,bloodRequestStatus,
requiredBloodType,requiredBloodUnits,bloodRequestUrgency,bloodRequestDescription`.
Create/update request fields: `hospitalId,requiredBloodType,requiredBloodUnits,
bloodRequestUrgency,bloodNeededDate,bloodRequestDescription`.
Preserve the differing `bloodNeededDate` request / `neededDate` response names.

## Kafka notification contract

Topic `booking-events`; key is decimal booking ID (`"7"`). One meaningful event
per booking mutation, persisted in the same database transaction via outbox.

```json
{"eventId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","bookingId":7,"recipientEmail":"donor@example.invalid","notificationType":"BOOKING_CREATED","title":"Donation Booking Created","message":"Synthetic test message"}
```

Exact fields: `eventId,bookingId,recipientEmail,notificationType,title,message`.
Event ID is UUID; booking ID positive; text fields nonblank. Supported producer
types: `BOOKING_CREATED`, `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`,
`BOOKING_COMPLETED`, `BOOKING_RESCHEDULED`. The reminder also publishes `APPOINTMENT_REMINDER`
using the same JSON fields. Consumer uses its own record class, not the producer's Java
package name. Type headers may be present; consumer configuration ignores them.
Notification persistence deduplicates event IDs. Transport is at least once;
SMTP delivery is not exactly once. DLT is `booking-events-dlt`.

The Kafka integration test exercises the actual producer through a real broker,
deserializes into Notification's event class, then invokes the actual consumer
with mocked persistence. It does NOT verify listener-container configuration,
DLT routing, database event deduplication or SMTP.

## Existing invariants

- Request row lock, donor transaction advisory lock and slot row lock coordinate
  creation. Current order is request -> donor -> slot. Do not redesign it here.
- `0 <= reserved_count <= capacity`.
- Reservations count all non-CANCELLED bookings, including COMPLETED bookings.
- Donor duplicates mean identical donor/date/time with status other than
  CANCELLED. Different times remain permitted; cancellation permits rebooking.
- Request and slot hospital must match; date must match slot and be no later
  than needed date; slot time interval is start-inclusive/end-exclusive.
- Only OPEN requests/slots accept booking; future date/time required.
- PENDING -> CONFIRMED/CANCELLED; CONFIRMED -> COMPLETED/CANCELLED.
- COMPLETED/CANCELLED bookings are terminal.
- Cancellation decrements the reservation once; completion retains it.
- Completing enough donations atomically changes OPEN request to COMPLETED.
- Closing/deleting a request serializes with creation; requests with any
  bookings cannot be deleted.
- Booking, capacity mutation, audit and outbox commit/rollback together.

No idempotency-key, overlap prevention, request ownership transfer, new service,
new table, or production schema change is introduced in Batch 1.
