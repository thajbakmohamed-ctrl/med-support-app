# Med Support Project 4 — one Java SE environment

For the confirmed AWS deployment failure, latest fixes, validation results and
exact existing-environment recovery checklist, read [RECOVERY.md](RECOVERY.md).
The downloaded AWS log confirms a missing RDS_HOST setting; no live deployment
success is claimed.

Selected submission: one Elastic Beanstalk application, one Java SE / Corretto 17
on Amazon Linux 2023 environment, one ZIP, four independent executable JARs and
one nginx public entry point. No Docker EB platform is used. Kafka is explicitly
an external private dependency, like the existing private RDS database.

## Package contents and startup

`single-java/` is the current deployment source. `Package-SingleJava.ps1` reuses
all four existing service target JARs; it does not rebuild or merge services.
Procfile starts Identity (`web`, first), Booking, Blood Request and Notification
as four independently supervised JVMs on loopback ports 8083–8086. Explicit CLI
ports prevent collisions. Each JVM uses a 32 MiB initial / 256 MiB maximum heap.
No Maven, container engine, Kafka installation or application compilation is
required on the EB host. Keep the service target JARs and Maven wrappers.

`config/aws.properties` is an additional deployment-only property source. No
existing application.properties, local Compose, Project 3, migrations, locking,
reminders or notification behavior is changed. Database initialization stays
`never`, Hibernate stays `validate`, and UTC is retained. The four services still
share the existing PostgreSQL schema. No schema/data upgrade is run by this ZIP.

The ZIP includes four JARs under services/, Procfile, a startup shell script,
external property configuration, the existing frontend with deployment-only
config.js, nginx extensions, the public AWS RDS CA bundle, one EB options file
and a predeploy hook. It contains no secrets, .env, backups, uploads or Kafka data.
The default EB nginx configuration and health logging remain enabled; extensions
add exact/static frontend routes and prefixed APIs without defining a duplicate
`location /`. Any generated default API fallback is blocked at `/api/`.

## Required environment properties

Set these on the ONE environment before deploying this ZIP:

| Variable | Value |
|---|---|
| RDS_HOST or RDS_HOSTNAME | Existing private RDS endpoint, without scheme/port |
| RDS_DATABASE or RDS_DB_NAME | Existing Project 4 database; default med_support |
| RDS_USERNAME | Application database role, not the RDS master |
| APP_FRONTEND_ORIGIN | Exact HTTPS DNS origin; no port/path/trailing slash |
| KAFKA_BOOTSTRAP_SERVERS | Existing private broker DNS:port list |
| APP_UPLOAD_DIR | Optional absolute durable mount; default /var/app/p4-uploads |

Use native EB Secrets Manager/SSM environment-secret references for:
`RDS_PASSWORD`, `JWT_SECRET`, `IDENTITY_SERVICE_KEY`, `NOTIFICATION_SERVICE_KEY`,
`MAIL_USERNAME`, `MAIL_PASSWORD`. JWT must be stable and at least 32 UTF-8 bytes;
internal keys must be random and at least 32 characters. MAIL_PASSWORD is the
Gmail app password. Grant the EC2 instance role access to only those secret ARNs
and their KMS key when applicable. Do not paste values into the ZIP, Git, shell
commands or logs. Do not add conflicting old SPRING_DATASOURCE_*, SERVER_PORT,
SPRING_APPLICATION_JSON or service URL overrides from previous templates.

RDS TLS uses `sslmode=verify-full` and `/var/app/current/certs/global-bundle.pem`.
This URL also protects Notification's separate JDBC worker-lock connection.
Gmail requires authentication, STARTTLS and server certificate verification;
header/envelope sender uses MAIL_USERNAME. Packaging performs no mail/network
application checks. Admin seeding and recovery remain disabled. Only settings
are changed; database pools, worker concurrency and business logic are retained.

The predeploy hook validates required variable presence and the HTTPS origin,
creates the upload directory for webapp, and makes static files readable by
nginx. It prints no values. Current EB platforms expose environment properties to
hooks; use a current Corretto 17 platform supporting native environment secrets.
A missing setting makes deployment fail explicitly rather than silently using a
local database or broker.

## Public routing and HTTPS

| URL under APP_FRONTEND_ORIGIN | Target |
|---|---|
| / and /index.html | Existing frontend |
| /identity/api/... | 127.0.0.1:8083/api/... |
| /booking/api/... | 127.0.0.1:8084/api/... |
| /blood/api/... | 127.0.0.1:8085/api/... |
| /notification/api/... | 127.0.0.1:8086/api/... |
| /ws/notifications | 127.0.0.1:8086/ws/notifications |

Frontend URLs derive from the one HTTPS origin. JWT first-frame WebSocket
authentication is preserved. nginx forwards the original Origin/Host and the
trusted HTTPS scheme, supports Upgrade and a 300-second read timeout. The new
JARs include APP_FRONTEND_ORIGIN CORS support; Notification already had it.
Internal service-key APIs are blocked at the public proxy; internal callers use
loopback directly. No service port or Kafka port should be opened publicly.
`/healthz` is proxy liveness only; verify all four `/SERVICE/actuator/health`
endpoints and actual workflows separately after an authorized deployment.

The template retains the instructor's **t3.micro / SingleInstance** setting.
SingleInstance does NOT provide HTTPS automatically. Before real user access,
provide a domain/certificate and TLS ingress. Either arrange single-instance TLS
termination separately, or (within the same application/environment) select
LoadBalanced with an ALB, MinSize=MaxSize=1, an ACM HTTPS 443 listener and HTTP to
HTTPS redirect. That keeps one application host but changes the environment type
and adds ALB charges. No certificate, listener, DNS or security group is silently
created by these files. Trust forwarded headers only from that configured
controlled ingress, and restrict instance port 80 appropriately. HTTP-only EB
URLs intentionally fail the frontend's HTTPS check.

## Kafka, storage and instance capacity — unresolved infrastructure

Kafka is REQUIRED and is not inside this Java ZIP. Use an existing durable broker
on separate private EC2 capacity or an appropriate managed Kafka service. Broker
advertised listeners must resolve privately from EB. Retain `booking-events` and
`booking-events-dlt`, current consumer group and existing topic definitions.
Never open Kafka/controller ports publicly. With private PLAINTEXT brokers, allow
only the EB instance security group in a trusted VPC. If the selected managed
broker requires TLS/SASL/IAM, configure compatible Spring Kafka security settings
before deployment; this ZIP adds no IAM Kafka authentication dependency. Do not
claim notifications work until the private broker/topics/connectivity exist.

EB must reach existing private RDS in the proper VPC/subnets. Allow RDS 5432 only
from the EB instance security group. Keep DB encryption/backups and the correct
application schema/permissions. Private instances need outbound access to EB,
secrets and Gmail TCP 587 (possibly paid NAT). Never expose 8083–8086 or PostgreSQL.
No AWS resources or settings have been created or changed by this preparation.

Uploads default outside the replaceable application version, so redeploying the
JARs does not overwrite them. The default directory still DOES NOT survive EC2
replacement; use a prepared persistent private mount/backup strategy before
storing real files. The mount must permit webapp ownership/access. Source/local
uploads and existing database backups are never copied into this ZIP.

No full-stack RSS/load measurement is available. For planning, four Spring JVMs
may need about 1.2–2.0 GiB combined, plus roughly 0.4–0.8 GiB for Linux, EB and
nginx. The four maximum heaps alone total 1 GiB; native memory, stacks, direct
buffers and page cache are additional. Even with Kafka external, **one t3.micro
is not a supported recommendation**. Smaller heap caps or swap do not prove it
will run correctly. Start testing on **t3.medium (4 GiB)** as a practical candidate;
this requires an instructor exception and does not establish a measured minimum.
If t3.micro is strict, multiple hosts/environments or a changed assignment scope
would be needed, which conflicts with the selected single-host constraints.
Kafka capacity is separate in all cases. Do not upload/deploy and promise success
on t3.micro merely because the ZIP is structurally valid.

## Reproduce the ZIP

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./deployment/project4/Package-SingleJava.ps1 -RdsCaBundle ./deployment/project4/out/certs/global-bundle.pem
```

The script prints a fresh ignored out/single-java-*/med-support-project4-java17.zip.
No tests, dependencies or application features are added. Existing target JARs
are reused byte-for-byte, and their executable Boot manifests are checked. Local
JARs must be rebuilt only if source changes after this preparation. Use the
public AWS RDS CA download as the certificate input, never a private key.

## Exact AWS console upload steps (future action, not performed)

1. Resolve the t3.micro capacity and private Kafka blockers first. Confirm RDS
   schema, networking and persistent uploads. Have HTTPS ingress ready.
2. In Elastic Beanstalk, create/select ONE application and ONE Web server
   environment. Choose **Java SE**, **Corretto 17**, **Amazon Linux 2023** (not
   Tomcat or Docker). Use current platform version supporting environment secrets.
3. Choose **Upload your code**, select the final ZIP as-is, and use a unique
   version label. Do not unzip it or wrap it in an enclosing directory. For an
   existing matching environment use **Upload and deploy** after configuring it.
4. Before launching/deploying, configure the VPC/subnets, instance profile,
   security groups, all environment properties and native secret references.
   Confirm t3.micro is the instructor setting; an approved larger size must be
   explicitly selected and must override/update the bundled InstanceTypes option.
   Do not expect it to fit simply by selecting the requested type.
5. Configure the chosen HTTPS ingress/domain. If choosing ALB, explicitly override
   the bundled SingleInstance environment setting and keep exactly one host.
6. Only after separate authorization, launch/deploy. Check all four EB process
   logs, the four prefixed health endpoints, RDS TLS, broker/topic connectivity,
   REST login/booking and WSS. Gmail acceptance sends need separate authorization.
   `/healthz` alone is not proof of application readiness or capacity.

## Cleanup scope

Only prior generated per-service/Compose/frontend output trees and superseded
AWS deployment templates/packagers under deployment/project4 are removed after
verifying the new ZIP. The official CA input, new ZIP/staging tree, current
single-java sources/packager and documentation are retained. Historical Java and
Compose readiness notes remain in archive/ for submission context, clearly marked
superseded. Local Docker Compose, Project 3, all service code, Maven wrappers,
tests/Cucumber, CI/CD, migrations, target JARs, backups and Docker volumes are not
cleanup targets. Existing unrelated untracked Project 3 files remain untouched.

References: [Java SE Procfile](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-procfile.html),
[nginx extensions](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/platforms-linux-extend.proxy.html),
[EB platform hooks](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/platforms-linux-extend.hooks.html).

## Recorded validation and cleanup

The final ZIP contains exactly 17 allowlisted files. Every packaged file matched
its source SHA-256, including all four original executable JARs. Each JAR has its
Boot launcher and application main class, Java 17 bytecode, and the deployment
CORS property. No env files, prohibited resource paths or literal credential
properties were found. The frontend config and PowerShell packager passed syntax
checks; both shell scripts passed Bash -n. Official nginx 1.28.0 accepted the
proxy snippets in a representative EB default-server syntax check. The temporary
nginx download was removed; no server, application, database or container started.
Exact hosted EB/nginx integration, HTTPS/RDS/Kafka connectivity and full-stack
memory/workflow acceptance remain unverified.

Cleanup removed 12 superseded generated output directories (six redundant ZIPs,
about 888.6 MiB), the compose/frontend/java/notification deployment prototype
directories, and Package-Compose.ps1, Package-Service.ps1, Package-Frontend.ps1
and environment.env.example. The final ZIP's SHA-256 was unchanged after cleanup.
Only the public CA and final single-Java ZIP/staging output remain under out/.
All uncertain historical submission notes were retained in archive/; no files
outside deployment/project4 were cleanup targets.
