# Historical notes — superseded by the single Java SE bundle

Retained for submission context; paths and deployment instructions below describe a superseded approach. Use ../README.md for the current instructions.

# Project 4 AWS preparation

No resources are created by these files. Local Compose, local frontend config and
all application.properties remain unchanged. Only three CORS configurations now
accept APP_FRONTEND_ORIGIN, defaulting to http://localhost:5500. Notification
already uses that property for both REST and the WebSocket handshake.

## Selected layout and capacity

Use four separate Elastic Beanstalk **Java SE / Corretto 17 on Amazon Linux 2023**
environments, each with one **t3.micro**. Each service ZIP sets SingleInstance and
t3.micro; it does not configure HTTPS, networking, IAM or storage for you.
Use one independent private RDS PostgreSQL 17 database with the existing shared
public schema. Keep Kafka on separate private capacity with durable storage.
Host the static frontend over HTTPS. This requires multiple instances; it is not
a complete single-t3.micro deployment. Per-service t3.micro capacity also needs
runtime measurement. No heap/pool/worker tuning or concurrency behavior changes
are included. Do not deploy four JVMs plus Kafka onto one t3.micro without a
measured 1 GiB memory budget and concurrent workflow evidence.

Kafka is not a Java SE deployment artifact. Its private DNS, persistence and
hosting must be arranged separately (additional instance or managed broker).
Preserve topics booking-events and booking-events-dlt, the existing consumer group,
outbox and retry settings. Provision topics using the existing definitions only
when the new broker is ready. Do not expose broker/controller ports publicly;
advertised listeners must resolve privately from the service environments.

## IntelliJ and build

Keep Project/module SDK, Maven importer and runner on installed JDK 17. The root
project is Project 3: build the four service POMs individually. The shell may use
Java 26; explicitly set JAVA_HOME to the installed JDK 17 before using Maven.
All four POMs target Java 17 and existing CI/Dockerfiles use Java 17.

With Maven available, for each service (replace SERVICE):

```powershell
mvn -B -f SERVICE/pom.xml -Dmaven.test.skip=true package
```

Use SERVICE/target/SERVICE-0.0.1-SNAPSHOT.jar (not .jar.original). This skips tests;
it is only a compilation/packaging check. Rebuild Identity, Booking and Blood
Request after the CORS change. A previously built image/JAR will retain hardcoded
localhost origins. Reuse Notification's existing executable JAR if it matches
the intended current source. Existing CI publishes four SHA-tagged GHCR images,
not downloadable JAR artifacts; do not assume a JAR artifact exists in Actions.
The selected Java SE route reuses locally/CI-built executable JARs, so no Docker
image pulls or registry credentials are needed. No workflow changes are required
for this preparation. CI can later upload the executable JARs and these ZIPs.

## Package one service

Obtain the public AWS RDS global CA certificate bundle from AWS and verify its
source. No certificate was downloaded or bundled by this change.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./deployment/project4/Package-Service.ps1 -Service identity-service `
  -Jar ./identity-service/target/identity-service-0.0.1-SNAPSHOT.jar `
  -RdsCaBundle C:/path/to/global-bundle.pem
```

Repeat for booking-service, blood-request-service and notification-service.
The script makes a fresh ignored out/ directory and prints the ZIP path. Its
allowlist includes only app.jar, Procfile, .ebextensions, .platform and the public
CA certificate. No env files are included. The JAR must have been built without
embedded credentials; the packager cannot certify arbitrary input JARs are
secret-free. Existing service properties use credential placeholders.

ZIP entries are at the root (no enclosing directory). Notification additionally
gets the /ws/notifications nginx Upgrade location. All ZIPs allow 6 MB requests
and block public /api/internal/ and /api/notifications/internal access. Internal clients use private port 5000,
not the public nginx endpoint. Procfile uses Java from the selected Corretto 17
platform; both PORT and SERVER_PORT are 5000.

## Environment and secrets

environment.env.example is documentation, not an automatically loaded env file.
Apply only the variables each service needs, through EB environment properties.
The runtime config already supplies UTC, ddl-auto=validate, SQL init=never and
SQL logging=false. Do not replace application.properties or use the local .env.

| Environment | Additional values |
|---|---|
| All four | RDS URL/user/password, APP_FRONTEND_ORIGIN |
| Identity | JWT_SECRET, IDENTITY_SERVICE_KEY, APP_FRONTEND_URL, durable APP_UPLOAD_DIR, Gmail |
| Booking | SERVICES_IDENTITY_URL, SERVICES_BLOOD_REQUEST_URL, SERVICES_NOTIFICATION_URL, IDENTITY_SERVICE_KEY, NOTIFICATION_SERVICE_KEY, Kafka |
| Blood Request | SERVICES_IDENTITY_URL |
| Notification | SERVICES_IDENTITY_URL, SERVICES_BOOKING_URL, SERVICES_BLOOD_REQUEST_URL, NOTIFICATION_SERVICE_KEY, Kafka, Gmail |

Use native EB `aws:elasticbeanstalk:application:environmentsecrets` references to
Secrets Manager/SSM ARNs for passwords, JWT and service keys. Grant the instance
role access only to the required secrets (and KMS key if applicable). Enter real
values in the secret service, never in tracked files, ZIPs, command history or
browser config. Keep JWT stable across Identity restarts. Match internal keys
between caller and receiver. Keep admin seeding and notification recovery disabled.

Gmail: supply MAIL_USERNAME/MAIL_PASSWORD and SPRING_MAIL_USERNAME/
SPRING_MAIL_PASSWORD from the same secrets; MAIL_PASSWORD is a Gmail app password.
Set the SMTP overrides in the example on Identity and Notification. Their local
defaults differ, so enabling STARTTLS/auth explicitly on both is necessary.
For a non-secret verified sender address, set SPRING_APPLICATION_JSON in each mail
environment to the following, preserving the exact Hibernate key:

```json
{"spring.jpa.properties.hibernate.jdbc.time_zone":"UTC","spring.mail.properties.mail.from":"sender@example.invalid","spring.mail.properties.mail.smtp.from":"sender@example.invalid"}
```

Replace the sender with the authenticated Gmail account. No SMTP checks or sends
are part of preparation. Do not use Mailpit in the hosted configuration.

## Frontend and WebSocket

frontend/config.js shows deployment-only public HTTPS placeholders. Keep local
project4-frontend/config.js unchanged. Generate a ready-to-host static directory:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./deployment/project4/Package-Frontend.ps1 `
  -IdentityUrl https://identity.your-domain.com `
  -BookingUrl https://booking.your-domain.com `
  -BloodRequestUrl https://blood.your-domain.com `
  -NotificationUrl https://notification.your-domain.com
```

The script copies only index.html, styles.css, app.js and generated config.js.
The command-line ExecutionPolicy override applies only to that PowerShell process;
it does not change the machine's script policy.
Use DNS origins, not API path prefixes. Notification's browser URL becomes
wss://notification.your-domain.com/ws/notifications; existing first-frame JWT
authentication remains unchanged. Set APP_FRONTEND_ORIGIN on all services to the
exact frontend HTTPS origin without a trailing slash. Set Identity's
APP_FRONTEND_URL to the same URL for verification/reset emails. Configure the
HTTPS ingress to preserve Origin, Host and X-Forwarded-Proto, support WebSocket
Upgrade, and allow long-lived connections (for example a 300-second idle timeout).

## Required infrastructure before deployment

- RDS must be private, encrypted, in the application VPC; allow 5432 only from
  application security groups. Use a non-master application role with required
  shared-schema table/sequence privileges, backups and rds.force_ssl enabled.
  The example JDBC URL uses verify-full and the bundled public CA. TLS is in the
  URL so Notification's separate worker-lock connection uses the same protection.
- Restore only an independently verified Project 4 backup to the new database.
  Review migration history including S005; these artifacts never generate, apply
  or rerun migrations. Keep shared tables/locks intact. Never touch local volumes.
- Arrange private DNS/service discovery that survives EB instance replacement.
  Permit private port 5000 only between the application security groups; never
  open 5000, 5432 or Kafka ports to 0.0.0.0/0 or ::/0. Restrict SSH as well.
- Provide HTTPS for every public API and frontend before using real accounts.
  SingleInstance EB does not automatically terminate HTTPS: provision appropriate
  TLS ingress/certificates separately, or explicitly choose a load-balanced
  environment with HTTPS (which overrides the SingleInstance template and costs
  more). Public HTTP EB defaults are not a finished deployment.
- Mount persistent private storage at Identity's APP_UPLOAD_DIR before uploads;
  ordinary EB instance storage is replaceable. EFS mounting needs additional
  environment-specific configuration and is not silently created here.
- Check /actuator/health on every service, real booking concurrency, outbox/Kafka,
  inbox/WebSocket reconnect and SMTP only during an authorized deployment check.
  Compilation does not prove capacity, connectivity or end-to-end operation.

Costs include four EC2 instances plus Kafka capacity, RDS, disks/backups, public
IPv4, logs, secrets, persistent uploads and any TLS load balancers/NAT. No fixed
regional price or free-tier eligibility is assumed.

## Preparation validation

All four service compile goals passed using JDK 17.0.20.1 and Maven 3.9.16
(offline cache, no tests). Both packaging scripts passed PowerShell parsing;
the frontend packager also ran successfully with example.com origins into ignored
out/. The deployment frontend config passed JavaScript syntax parsing, and
git diff --check passed. No executable service JARs or RDS CA bundle were available,
so service ZIPs were not generated and the service packager was not exercised
end to end. No nginx/EB runtime, RDS/Kafka connectivity, SMTP, TLS or capacity
validation is claimed. Existing CI, Dockerfiles and local configuration are unchanged.

References: [EB Java SE Procfile](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-procfile.html),
[AL2023 nginx extensions](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/platforms-linux-extend.proxy.html),
[instance options](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/command-options-general.html),
[EB secrets](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/AWSHowTo.secrets.env-vars.html),
[RDS PostgreSQL TLS](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/PostgreSQL.Concepts.General.SSL.html).
