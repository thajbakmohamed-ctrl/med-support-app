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

![Healthcare Support System ERD](docs/healthcare-support-erd.png)

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
| GET | `/api/blood-requests?page={page}&size={size}&sortBy={field}&direction={asc/desc}` | Retrieve blood requests with pagination and sorting | Authenticated User |
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

### 4. Run the Application

On Windows:

```bash
.\mvnw spring-boot:run
```

The application will run on:

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