# Healthcare Support System

## Project Description

Healthcare Support System is a backend application designed to connect individuals who want to support the healthcare sector with hospitals and healthcare organizations that need community assistance.

The first version of the platform focuses on blood donation. Hospitals can publish blood requests, while donors can view relevant requests and book donation appointments.

The system is designed to be scalable, allowing future versions to include additional healthcare support services such as medical equipment donation, patient companion services, transportation assistance and other volunteer opportunities.

## Problem Statement

Hospitals and healthcare organizations may face situations where community support is needed, but there is often no centralized system that connects them directly with individuals who are willing to help.

For blood donation specifically, hospitals may need particular blood types within a certain period, while potential donors may not easily know where or when their blood type is needed.

Healthcare Support System aims to provide a centralized platform where healthcare organizations can publish their support needs and community members can find suitable opportunities and respond through an organized process.


## Project Purpose

The purpose of Healthcare Support System is to create a secure and organized platform that makes it easier for individuals to support healthcare organizations.

In the first version, the system focuses on improving the blood donation process by allowing hospitals to publish blood requests and donors to discover these requests and book suitable donation appointments.

The project also aims to provide a strong foundation that can be expanded in the future to support additional healthcare and volunteering services.


## Target Users and Roles

The Healthcare Support System supports three main user roles:

### Donor
A donor is a registered user who wants to support healthcare organizations through blood donation. Donors can manage their profiles, view available blood requests, search for suitable requests, and book or cancel donation appointments.

### Hospital Staff
Hospital staff are authorized users associated with a hospital or healthcare organization. They can create and manage blood requests, view donation bookings, and update booking statuses.

### Admin
The admin manages the overall platform. Admins can manage users and hospitals, deactivate accounts when necessary, and monitor the system.

## Version 1 Scope

The first version of the Healthcare Support System focuses on blood donation support and appointment booking.

## Technologies Used

- **Java 17** - Main programming language.
- **Spring Boot 4.1.1** - Backend application framework.
- **Spring Web MVC** - REST API development.
- **Spring Data JPA** - Database access and persistence.
- **PostgreSQL** - Relational database management system.
- **Spring Security** - Authentication and role-based authorization.
- **JWT (JJWT 0.12.6)** - Token-based authentication.
- **Spring Validation** - Request data validation.
- **Spring Mail** - Email verification and password recovery emails.
- **Spring WebSocket** - Real-time booking notifications.
- **Swagger / OpenAPI** - Interactive API documentation.
- **Lombok** - Reduction of repetitive Java code.
- **Maven** - Dependency management and project build.
- **JUnit / Spring Security Test** - Automated testing.

## General Approach

The Healthcare Support System was developed using a layered backend architecture to keep the application organized, maintainable, and scalable.

The development process started with defining the project scope, user roles, database entities, and relationships. The backend was then implemented using Spring Boot with separate controller, service, repository, model, security, and exception layers.

REST APIs were developed for authentication, user profiles, hospitals, blood requests, and donation bookings. Business rules and validation were implemented in the service layer, while Spring Security and JWT were used to protect endpoints and enforce role-based access.

PostgreSQL is used for persistent data storage, Swagger/OpenAPI is used for API documentation and testing, and automated tests are used to verify important business rules and security behavior.

## User Stories

User stories were created to represent the main requirements of the Healthcare Support System from the perspective of Donors, Hospital Staff, and Admins.

The user stories cover the main system functionality, including user registration, login, email verification, password management, profile management, blood requests, donation bookings, booking status management, cancellation, and administration.

The complete user stories and their development progress are managed in the project Jira board:

https://thajbakmohamed.atlassian.net/jira/software/projects/KAN/boards/2

## Project Planning

The Healthcare Support System was planned and tracked using Jira.

The Jira board was used to organize the project into Epics, Tasks, and User Stories, track development progress, and manage project deadlines using due dates.

Project planning board:

https://thajbakmohamed.atlassian.net/jira/software/projects/KAN/boards/2


### In Scope

- User registration and secure login.
- Email verification.
- Role-based access for Donors, Hospital Staff, and Admins.
- User and donor profile management.
- Profile picture and CPR image upload.
- Hospital management.
- Blood request creation and management.
- Search and filtering of blood requests.
- Donation appointment booking.
- Booking availability management.
- Prevention of conflicting or duplicate bookings.
- Booking cancellation and status updates.
- Email notifications.
- Real-time booking notifications.
- Soft deletion and account deactivation.
- Secure REST API using JWT authentication.
- API documentation using Swagger/OpenAPI.

### Out of Scope for Version 1

The following features are planned for future versions:

- Medical equipment donation and lending.
- Patient companion services.
- Transportation assistance.
- Translation and digital assistance.
- General healthcare volunteering opportunities.
- Volunteer hour tracking and certificates.
- Mobile application.
- SMS notifications.
- Map and location services.
- Advanced analytics and dashboards.

## Database ERD

The following Entity Relationship Diagram represents the database design for the Healthcare Support System.




![Healthcare Support System ERD](docs/healthcare-support-erd%20%283%29.png)




## Project Architecture

The Healthcare Support System follows a layered backend architecture using Java and Spring Boot.

The project is organized into the following packages:

- **controller** - Handles HTTP requests and REST API endpoints.
- **service** - Contains business logic and system rules.
- **repository** - Handles database operations using Spring Data JPA.
- **model** - Contains the system entities and database models.
- **dataTransferObject** - Contains request and response objects used to transfer data between the client and the application.
- **security** - Handles authentication, authorization, and JWT security.
- **exception** - Handles custom exceptions and global error handling.
- **config** - Contains application and security configuration.


### Package Structure

```text
com.app.med_support
│
├── controller
├── service
├── repository
├── model
├── dataTransferObject
│   ├── request
│   └── response
├── security
├── exception
└── config
```

### Application Flow

```text
Client / Postman
        ↓
Controller
        ↓
Data Transfer Object + Validation
        ↓
Service
        ↓
Business Rules
        ↓
Repository
        ↓
PostgreSQL
```



### Application Flow

```text
Client / Postman
        ↓
Controller
        ↓
Data Transfer Object + Validation
        ↓
Service
        ↓
Business Rules
        ↓
Repository
        ↓
PostgreSQL
```
## API Documentation

The REST API is documented and can be tested interactively using Swagger/OpenAPI.

### Authentication Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/auth/register` | Register a new DONOR or HOSPITAL_STAFF account and send an email verification link | Public |
| GET | `/api/auth/verify-email?token={token}` | Verify a user's email using the verification token | Public |
| POST | `/api/auth/login` | Authenticate a verified user and return a JWT token | Public |
| POST | `/api/auth/forgot-password?email={email}` | Send a password reset link to the registered email | Public |
| POST | `/api/auth/reset-password` | Reset the user's password using a valid reset token | Public |
| PUT | `/api/auth/change-password` | Change the password of the authenticated user | Authenticated User |

### User Profile Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| PUT | `/api/users/profile` | Update the authenticated user's profile information | Authenticated User |
| POST | `/api/users/profile/cpr` | Upload a CPR document in PDF, JPEG, or PNG format | Authenticated User |
| POST | `/api/users/profile/image` | Upload a profile image in JPEG or PNG format | Authenticated User |
| DELETE | `/api/users/profile` | Soft delete the authenticated user's account by changing its status to INACTIVE | Authenticated User |

### Hospital Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| GET | `/api/hospitals` | Retrieve all hospitals available in the Med Support system | Authenticated User |
| GET | `/api/hospitals/{hospitalId}` | Retrieve a specific hospital by its ID | Authenticated User |

### Blood Request Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/blood-requests` | Create a blood request for the logged-in staff member's hospital | HOSPITAL_STAFF |
| GET | `/api/blood-requests?page={page}&size={size}&sortBy={field}&direction={asc/desc}` | Retrieve blood requests with pagination and sorting | ADMIN |
| GET | `/api/blood-requests/open` | Retrieve all blood requests with OPEN status | DONOR |
| GET | `/api/blood-requests/open/blood-type/{bloodType}` | Retrieve OPEN blood requests filtered by blood type | DONOR |
| GET | `/api/blood-requests/{bloodRequestId}` | Retrieve a specific blood request by ID | ADMIN / HOSPITAL_STAFF |
| PUT | `/api/blood-requests/{bloodRequestId}/status?newStatus={status}` | Update the status of a blood request belonging to the staff member's hospital | HOSPITAL_STAFF |
| GET | `/api/blood-requests/hospital` | Retrieve all blood requests belonging to the logged-in staff member's hospital | HOSPITAL_STAFF |

### Donation Booking Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/api/donation-bookings` | Create a donation booking for an OPEN blood request | DONOR |
| GET | `/api/donation-bookings/donor` | Retrieve all donation bookings belonging to the logged-in donor | DONOR |
| GET | `/api/donation-bookings/blood-request/{bloodRequestId}` | Retrieve donation bookings for a blood request belonging to the staff member's hospital | HOSPITAL_STAFF |
| PUT | `/api/donation-bookings/{bookingId}/status?newStatus={status}` | Update the status of a donation booking | HOSPITAL_STAFF |
| PUT | `/api/donation-bookings/{bookingId}/cancel` | Cancel one of the logged-in donor's eligible bookings | DONOR |

### Admin Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| PUT | `/api/admin/users/{userId}/reactivate` | Reactivate an inactive user account | ADMIN |
| PUT | `/api/admin/users/{userId}/deactivate` | Deactivate a user account without permanently deleting it | ADMIN |
| POST | `/api/admin/hospitals` | Create a new hospital | ADMIN |
| PUT | `/api/admin/hospitals/{hospitalId}/deactivate` | Deactivate a hospital without permanently deleting it | ADMIN |
| PUT | `/api/admin/hospitals/{hospitalId}/reactivate` | Reactivate a previously deactivated hospital | ADMIN |
| GET | `/api/admin/hospitals` | Retrieve all hospitals for administration | ADMIN |
| PUT | `/api/admin/hospitals/{hospitalId}` | Update an existing hospital's information | ADMIN |

### Swagger / OpenAPI

The Healthcare Support System provides interactive API documentation using Swagger/OpenAPI.

After running the application, the Swagger UI can be accessed at:

http://localhost:8080/swagger-ui/index.html

Swagger can be used to:
- View available API endpoints.
- Review request parameters and request bodies.
- View documented HTTP response codes.
- Test API endpoints.
- Authenticate protected endpoints using a JWT Bearer token.

## Installation and Setup

### Prerequisites

Before running the project, make sure the following are installed:

- Java 17
- PostgreSQL
- Maven or Maven Wrapper
- IntelliJ IDEA or another Java IDE

### 1. Clone the Repository

```bash
git clone https://github.com/thajbakmohamed-ctrl/med-support-app.git
cd med-support-app
```

### 2. Create the PostgreSQL Database

Create a PostgreSQL database named:

```text
med_support
```



### 3. Configure Environment Variables

The application uses environment variables to protect sensitive information.

Configure the following variables:

```text
DB_PASSWORD=your_postgresql_password
MAIL_USERNAME=your_email_address
MAIL_PASSWORD=your_email_app_password
JWT_SECRET=your_jwt_secret
```


```text
http://localhost:8080
```

### 5. Access Swagger

After the application starts, open:

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger can be used to explore and test the available REST API endpoints.

## Challenges

During the development of the Healthcare Support System, several challenges were encountered:

- Implementing JWT authentication and role-based authorization for different user roles.
- Managing email verification and secure password recovery.
- Applying booking business rules to prevent duplicate or conflicting donation appointments.
- Restricting hospital staff operations to their assigned hospital.
- Implementing secure file uploads with file type and size validation.
- Implementing pagination, sorting, filtering, and rate limiting.
- Integrating real-time booking notifications using WebSocket.
- Designing automated tests for business rules and security behavior.

These challenges were addressed during development through testing, validation, exception handling, and improvements to the application architecture.


## Unsolved Problems

There are currently no known critical unresolved issues in Version 1 of the Healthcare Support System.

Future development may reveal additional improvements or issues as new features are introduced and the system is expanded.

## Future Improvements

Future versions of the Healthcare Support System may include:

- Medical equipment donation and lending.
- Patient companion and support services.
- Transportation assistance for patients.
- Translation and digital assistance services.
- General healthcare volunteering opportunities.
- Volunteer hour tracking and certificates.
- Mobile application support.
- SMS notifications.
- Map and location services.
- Advanced analytics and dashboards.


## Credits and External Resources

The following resources were used during the development of this project:

- **General Assembly Java Developer Bootcamp Materials**  
  Used as learning and reference materials for Java, Spring Boot, REST APIs, database integration, and backend development.

- **Project Requirements and README provided by the instructor**  
  Used as the main reference for the project requirements, required backend concepts, documentation structure, and project deliverables.

- **Spring Boot and Spring Security**  
  Used for developing the REST API, authentication, authorization, and application security.

- **Swagger / OpenAPI**  
  Used to document and test the REST API endpoints.

- **Jira**  
  Used for project planning, Epics, Tasks, User Stories, development tracking, and deadlines.

### General Assembly - RESTful JSON API with Java Spring Boot

**Resource:** General Assembly Java Spring Boot Lesson   
**URL:** https://github.com/Java-FT-01-Bahrain/JDB-Info/blob/e244b561ff864fc267b1bfbef8e087c0f67afef2/Lessons/JavaSpringBoot/Java-Spring-Boot-lecture/README.md

This lesson was used as a learning and reference resource for Spring Boot REST APIs, Spring Profiles, Spring Data JPA, PostgreSQL integration, layered architecture, authentication, Spring Security, and JWT implementation.

### Spring Framework - WebSocket and STOMP

**Resource:** Spring Guide - Using WebSocket to Build an Interactive Web Application  
**URL:** https://spring.io/guides/gs/messaging-stomp-websocket/

Used as a reference for implementing real-time notifications using WebSocket and STOMP messaging.

### Spring Security

**Resource:** Spring Security Documentation  
**URL:** https://docs.spring.io/spring-security/reference/

Used as a reference for authentication, password encryption, endpoint protection, and role-based authorization in the application.

### Swagger / OpenAPI

**Resource:** Springdoc OpenAPI Documentation  
**URL:** https://springdoc.org/

Used as a reference for documenting and testing REST API endpoints through Swagger UI.


# Project 3 — Concurrency & Multithreading

## Project Overview

Project 3 is an extension of the Med Support Blood Donation Management System developed in Project 2.

The main goal of Project 3 is to improve the application's ability to handle multiple operations happening at the same time while maintaining data consistency and preventing booking conflicts.

In this phase, I introduced donation slots with limited capacity, implemented database locking to prevent race conditions, and added background processing using Java multithreading and ExecutorService.

The project also includes automated donation reminders, asynchronous email processing, improved booking management, and automatic blood request completion.

These improvements allow Med Support to manage simultaneous donation bookings more safely while providing a better experience for donors and hospital staff.

## Project 3 Objectives

- Prevent overbooking when multiple donors try to reserve the same donation slot.
- Prevent conflicting bookings for the same donor.
- Protect shared database records using pessimistic locking.
- Maintain database consistency using transactions.
- Use ExecutorService and a thread pool to process background tasks.
- Send booking-related emails asynchronously.
- Process scheduled donation reminders.
- Automatically complete blood requests when the required number of donations is fulfilled.
- Extend booking notifications and hospital request history.
- Test concurrent operations to verify that shared resources are handled safely.


## New Features Introduced in Project 3

Project 3 extends the existing Med Support application with new functionality focused on concurrency, thread safety, and background processing.

The main improvements include:

1. **Donation Slot Management:** Hospital staff can create and manage donation slots with limited capacity. Donors can select available slots when booking appointments.

2. **Concurrency-Safe Booking:** The application uses database locking and transactions to prevent overbooking when multiple donors attempt to reserve the same slot simultaneously.

3. **Duplicate Booking Prevention:** Donor profile locking helps prevent conflicting bookings created by simultaneous requests from the same donor.

4. **Donation Reminders:** Donors can schedule reminders for their upcoming appointments. A scheduled process identifies reminders that are due and submits them for processing.

5. **Asynchronous Processing:** An ExecutorService with a fixed thread pool of three workers processes reminder tasks and booking-related email tasks.

6. **Automatic Blood Request Completion:** When the number of completed donations reaches the required number of blood units, the blood request automatically changes to COMPLETED.

7. **Improved Booking Notifications:** Booking status updates are saved as notifications and delivered through WebSocket after successful database transactions.

8. **Hospital Request History:** Hospital staff can view all blood requests in General History and fully completed requests in Completed History.

## New Database Entities

Three new JPA entities were added during Project 3.

### 1. DonationSlot

The `DonationSlot` entity represents an appointment slot created by a hospital.

It stores the date, start time, end time, capacity, and number of reserved places.

The system uses this entity to control appointment availability and prevent overbooking.

A donation slot is associated with a hospital and can be linked to multiple donation bookings.

### 2. DonationReminder

The `DonationReminder` entity represents a reminder associated with a donation booking.

It allows the application to track scheduled reminders and their processing status.

The reminder workflow uses the following states:

- `PENDING` — Waiting to be processed.
- `PROCESSING` — Claimed by a background worker.
- `SENT` — Reminder notification processing completed.
- `FAILED` — Reminder processing failed.

A scheduled task checks for due reminders and submits them to the ExecutorService for processing.

The updated database contains ten JPA entities in total.


### 3. HospitalDepartment

The `HospitalDepartment` entity was added to organize hospital departments within the Med Support system.

Each department belongs to one hospital, while a hospital can have multiple departments.

The entity contains the following fields:

- `id` — Unique department identifier.
- `departmentName` — Name of the department.
- `description` — Description of the department.
- `hospital` — Reference to the associated hospital.

The entity uses a Many-to-One relationship with the existing `Hospital` entity.

A simple REST API allows hospital staff to create departments for their own hospital. Donors and hospital staff can also retrieve departments belonging to a specific hospital.

This feature does not require additional concurrency logic or database locks.


## Concurrency and Race Condition Handling

### Business Scenario

In Med Support, multiple donors may attempt to book the same donation slot at the same time.

Each donation slot has a limited capacity. Without proper concurrency control, simultaneous booking requests could cause overbooking or inconsistent reservation counts.

### Race Condition Example

Consider a donation slot with a capacity of 1.

Two donors attempt to reserve the same slot simultaneously:

1. Donor A checks the slot and sees one available place.
2. Donor B checks the same slot before Donor A finishes booking.
3. Both donors attempt to reserve the last available place.

Without concurrency control, both requests might succeed even though only one place is available.

This situation is called a **Race Condition**, where the final result depends on the timing of concurrent operations.

### Solution: Pessimistic Database Locking

To prevent conflicting database operations, Med Support uses `PESSIMISTIC_WRITE` locking with Spring Data JPA.

The application locks the required database record while a transaction performs its checks and updates.

Other transactions requesting a conflicting lock on the same record must wait until the current transaction releases it.

### Four Database Locks

**1. Donor Profile Lock**

Protects the donor's profile during booking creation.

It prevents simultaneous requests from the same donor from creating conflicting bookings.

**2. Donation Slot Lock**

Protects the donation slot's capacity and reservation count.

It ensures that multiple donors cannot reserve more places than the slot allows.

**3. Donation Booking Lock**

Protects an existing donation booking while its status is being updated.

It helps prevent conflicting changes to the same booking.

**4. Blood Request Lock**

Protects the blood request during donation completion.

It allows the application to safely count completed donations and update the request status when the required blood units have been fulfilled.

### Transaction Management

Med Support uses Spring's `@Transactional` annotation to manage related database operations as a single transaction.

For example, when creating a donation booking:

1. The system locks the donor profile.
2. It checks for conflicting bookings.
3. It locks the selected donation slot.
4. It checks the remaining capacity.
5. It creates the donation booking.
6. It updates the slot's reservation count.
7. The transaction commits if all operations succeed.

If an error occurs, the transaction can roll back its database changes, helping prevent partial updates.

### Expected Concurrent Booking Result

For a donation slot with one available place:

- Donor A successfully books the slot.
- Donor B receives a conflict response if the slot becomes full.
- The final reservation count remains within the slot capacity.

This approach helps maintain database consistency and prevents overbooking when multiple booking requests are processed concurrently.


## Multithreading and Asynchronous Processing


### ExecutorService and Thread Pool

In Project 3, Med Support uses Java's **ExecutorService** to process background tasks asynchronously without blocking the main HTTP request.

The application uses a fixed thread pool of three worker threads, configured in `ReminderExecutorConfig.java`.

The thread pool is created using `Executors.newFixedThreadPool(3)`.

It processes booking-related emails and donation reminders in the background.

Instead of creating a new thread for every task, the application submits tasks to the ExecutorService, which assigns them to available worker threads.

If all three workers are busy, additional tasks wait in a queue until a worker becomes available.


### Asynchronous Booking Emails

When a donation booking is confirmed or cancelled, the application can send an email notification to the donor.

Sending an email may take time because it requires communication with an external mail server.

To avoid blocking the main request, the application submits email tasks to the ExecutorService.

The `afterCommit()` callback is used to ensure that email processing starts only after the related database transaction has committed successfully.

The workflow is:

1. Hospital staff confirms or cancels a donation booking.
2. The booking status is updated in the database.
3. The database transaction commits successfully.
4. The email task is submitted to the thread pool.
5. An available worker thread processes the email task.

This separates the database operation from the potentially slower email-sending process.

### Scheduled Donation Reminders

Project 3 introduces donation reminders for upcoming appointments.

Donors can schedule reminders for their donation bookings.

The application uses Spring's `@Scheduled` functionality to check for reminders that are due.

The scheduler runs every ten seconds and identifies pending reminders that should be processed.

Due reminders are submitted to the ExecutorService for background processing.

### Reminder Processing Workflow

Each reminder follows a processing lifecycle:

1. **PENDING:** The reminder is waiting for its scheduled time.
2. **PROCESSING:** A worker claims the reminder for processing.
3. **SENT:** The reminder notification has been processed successfully.
4. **FAILED:** Reminder processing failed.

To prevent multiple workers from processing the same reminder at the same time, the application uses an atomic database update to claim the reminder.

Only the worker that successfully changes the reminder status from `PENDING` to `PROCESSING` can continue processing it.

This protects the system from creating duplicate reminder notifications when concurrent workers attempt to process the same reminder.

### Why Multithreading Was Used

Multithreading improves Med Support by allowing background tasks to run independently of the main request-processing flow.

It provides the following benefits:

- Booking-related emails can be processed without blocking the main HTTP request.
- Multiple background tasks can execute concurrently.
- The number of worker threads is controlled through a fixed-size pool.
- Scheduled donation reminders can be processed automatically.
- Atomic reminder claiming helps prevent duplicate processing by concurrent workers.

The thread pool improves task execution, while database transactions and concurrency controls protect shared data.



## Automatic Blood Request Completion

Project 3 introduces automatic blood request completion based on the number of successfully completed donations.

Previously, blood requests required manual status management. The updated system can automatically change a blood request from `OPEN` to `COMPLETED` when the required blood units have been fulfilled.

### Business Rule

**One completed donation equals one blood unit.**

For example, if a hospital creates a blood request requiring two blood units:

1. Donor A completes a donation.
2. The system counts one completed donation.
3. The blood request remains `OPEN` because one more unit is required.
4. Donor B completes a donation.
5. The system counts two completed donations.
6. The blood request automatically changes to `COMPLETED`.

### Database Consistency

The application uses `@Transactional` and `PESSIMISTIC_WRITE` locking on the blood request when updating a booking's status.

When a booking becomes `COMPLETED`, the application:

1. Updates the donation booking status.
2. Flushes the updated booking to the database.
3. Counts bookings with `COMPLETED` status for the same blood request.
4. Compares the completed donation count with the required blood units.
5. Updates the blood request to `COMPLETED` when the required number is reached.

These operations are performed within a database transaction to help maintain consistency during concurrent updates.

### Example Tested

Blood Request #22 required two blood units.

Two donation bookings were updated through the following statuses:

`PENDING → CONFIRMED → COMPLETED`

After the first completed donation, the blood request remained `OPEN`.

After the second completed donation, the application automatically updated the blood request to `COMPLETED`.

The application logged the successful completion:

`Blood request completed. Request ID: 22`

---

## Real-Time Booking Notifications

Med Support uses Spring WebSocket and STOMP to provide real-time booking status notifications.

When hospital staff updates a donation booking, the application creates a notification for the donor.

The notification is saved in the database and published through WebSocket after the transaction commits successfully.

This ensures that real-time booking updates are sent only after the corresponding database changes have been saved successfully.

### Notification Workflow

1. Hospital staff updates a donation booking.
2. The application updates the booking status.
3. A notification is created and saved in the database.
4. The database transaction commits.
5. The application publishes the notification through WebSocket.
6. The donor can receive the notification through the frontend.

Booking-related emails are also processed asynchronously using the ExecutorService.

---

## Hospital Blood Request History

Project 3 also improves the hospital staff dashboard by introducing separate views for blood request history.

### Active Requests

Displays blood requests that are currently `OPEN` and have not passed their required date.

### General History

Displays the hospital's blood requests regardless of their status, allowing staff to review past and current requests.

### Completed History

Displays only blood requests with the backend status `COMPLETED`.

A request appears in Completed History after the backend confirms its completion. A fully booked donation slot does not automatically mean that the required blood donations have been completed.

These views help hospital staff manage active requests and review completed donation workflows more clearly.



