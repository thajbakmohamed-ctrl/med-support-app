# Historical notes — superseded by the single Java SE bundle

Retained for submission context; paths and deployment instructions below describe a superseded approach. Use ../README.md for the current instructions.

# One Elastic Beanstalk Docker Compose environment

Use the current **Docker on Amazon Linux 2023** platform (not Java SE, Tomcat or
ECS Docker). This is a preparation bundle, not a tested live deployment. Do not
deploy the full stack on the configured t3.micro without resolving capacity.
No resources, databases, containers or Gmail messages are created by packaging.

## Contents and image provenance

Four unchanged Java 17 service images are pinned to linux/amd64 manifest digests
from successful push workflow commit 38cbe1b8d88ce86adae6c7c9ef0ff6078dc89674:
[workflow run](https://github.com/thajbakmohamed-ctrl/med-support-app/actions/runs/43846088752).
Kafka is apache/kafka:3.9.1, with the existing single-broker KRaft setup and topic
definitions. nginx:1.28.0-alpine serves the existing HTML/CSS/JS plus a public-only
deployment config and reverse proxies all APIs and WebSocket connections.
kafka-init is a one-shot topic initializer, not an extra permanent broker.
No PostgreSQL container, Mailpit, migrations, JARs or source builds are included.

The published Identity/Booking/Blood Request images predate the local CORS
parameterization. They are usable here because the browser and every API share
one HTTPS origin; nginx preserves Host/Origin and supplies forwarded scheme/port,
and Spring's SERVER_FORWARD_HEADERS_STRATEGY=framework restores that origin.
Same-origin requests do not require cross-origin permission. nginx additionally
rejects nonempty Origin headers other than APP_FRONTEND_ORIGIN. No Origin is
spoofed or removed, and no wildcard CORS permission is added. If you later split
frontend/API origins, publish the updated service images first. Notification's
existing APP_FRONTEND_ORIGIN controls the native WebSocket handshake.

Public routes, using APP_FRONTEND_ORIGIN=https://app.your-domain.com:

| Browser base/path | Container target |
|---|---|
| / | Existing frontend |
| /identity/api/... | identity-service:8083/api/... |
| /booking/api/... | booking-service:8084/api/... |
| /blood/api/... | blood-request-service:8085/api/... |
| /notification/api/... | notification-service:8086/api/... |
| /ws/notifications | notification-service:8086/ws/notifications |

Internal Identity/Notification APIs are blocked at the public nginx proxy.
Internal clients use Docker DNS/container ports directly. Only nginx publishes
host port 80; Kafka/controller and all four service ports have no host mappings.
Browser config derives absolute HTTPS API URLs from window.location.origin;
it contains no environment credentials. WebSocket first-frame JWT authentication
is unchanged. Real HTTPS/WSS routing and forwarded-header behavior still need
verification after authorized deployment.

## Required EB configuration (not included as secret values)

Set normal environment properties:

- RDS_HOST: existing private RDS endpoint, without scheme/port.
- RDS_DATABASE: existing Project 4 database (default med_support).
- RDS_USERNAME: non-master application database role.
- APP_FRONTEND_ORIGIN: exact public HTTPS DNS origin, no path/trailing slash.

Configure the following through EB native environmentsecrets references to
Secrets Manager/SSM; never enter values in tracked files, ZIPs or CLI arguments:

- RDS_PASSWORD.
- P4_JWT_SECRET: stable random value of at least 32 UTF-8 bytes.
- P4_IDENTITY_SERVICE_KEY and P4_NOTIFICATION_SERVICE_KEY: separate random values
  of at least 32 characters, automatically matched between callers/receivers.
- P4_GMAIL_USERNAME and P4_GMAIL_APP_PASSWORD: Gmail account and app password.

EB generates its runtime .env from environment settings. The ZIP deliberately
contains no .env; do not copy local env files to it. Compose explicitly forwards
only required settings to containers. Mail config requires authentication,
STARTTLS and certificate/hostname verification on Identity and Notification,
with matching header/envelope sender. Never print `docker compose config` or
container environments with real values; use `config --quiet` for validation.
EB's generated env file has Compose dollar-sign escaping behavior; use strong
base64url secrets without dollar signs for newly generated JWT/internal keys.
Verify existing RDS/Gmail secrets are passed unchanged, without displaying them;
do not rotate existing database passwords as part of preparation.

All services use the existing shared schema, ddl-auto=validate and SQL init=never.
The JDBC URL enforces sslmode=verify-full with the bundled official public AWS
CA file; this also covers Notification's separate worker-lock connection.
UTC, outbox, topic names, consumer group, reminders, scheduling pool and database
locking behavior are retained. Only deployment JVM heap/container limits change.

## Networking, HTTPS and persistence blockers

- EB instances must reach the existing RDS through its VPC/subnets/routes. RDS
  stays private; permit 5432 only from the EB instance security group. Never open
  5432, 8083-8086, 19092 or 29093 publicly. Existing RDS schema/migration state
  must match these services; this bundle performs no migration or data reset.
- The template requests an Application Load Balancer, exactly ONE EC2 host, and
  t3.micro to reflect the instructor constraint. Configure an HTTPS 443 listener
  with the correct ACM certificate and DNS before use; configure HTTP redirect to
  HTTPS. Restrict instance port 80 to the ALB security group. Only ALB ingress may
  supply trusted forwarded headers. The 443 listener/certificate, VPC and security
  groups are environment-specific and intentionally not guessed in the ZIP.
- Allow outbound HTTPS/DNS to GHCR/Docker Hub/EB/secrets endpoints and outbound
  TCP 587 to Gmail. Private EC2 subnets need suitable egress (usually paid NAT);
  placing EC2 in a public subnet does not make the private RDS public. Budget for
  ALB, public IPv4, RDS, disks/logs and any NAT, not just EC2.
- Docker volumes use distinct AWS names, never local Project 3/4 volume names.
  They survive container recreation on the same host, NOT instance replacement.
  Durable Kafka storage/backups and private upload storage are still required
  before relying on persisted messages/files. Do not scale this Compose stack to
  multiple EC2 hosts: it would duplicate the single-broker topology. Limits stay
  MinSize=MaxSize=1. A restart/replacement can interrupt requests and sockets.
- /healthz checks nginx only, not business readiness. Per-service TCP checks order
  startup; they are not substitutes for RDS/Kafka/workflow acceptance. Healthd
  logs are emitted on the host; no runtime log collection is claimed here.

## Memory: t3.micro is not a viable recommendation

Planning estimates, not measured RSS:

| Component | Expected light-load range | Configured limit |
|---|---:|---:|
| Four Spring JVMs combined | 1.2-2.0 GiB | 4 x 512 MiB |
| Kafka including native/page-cache/health CLI | 0.5-0.75 GiB | 768 MiB |
| nginx/static frontend | 15-64 MiB | 64 MiB |
| Linux, Docker and EB agents | 400-800 MiB | Host overhead |
| Topic initialization (transient) | 100-256 MiB | 256 MiB |

Spring heaps are capped at 256 MiB each; Kafka at 384 MiB. Heap caps do not include
metaspace, native/thread stacks, direct buffers or the kernel/page cache. Kafka's
health CLI has a separate 64 MiB heap cap. Combined light-load planning range is
roughly 2.1-3.6 GiB plus initialization spikes. Even the five configured maximum
heaps alone total 1,408 MiB, exceeding t3.micro's entire 1 GiB before overhead.
Limits are ceilings, not observed usage or reservations, and can cause OOM under
load. Docker Desktop is paused, so no full-stack memory test was performed.

**t3.medium (4 GiB)** is the minimum practical starting candidate for this bounded
single-host demo, subject to cold-start, concurrent booking, Kafka, SMTP and
WebSocket testing. t3.small (2 GiB) has inadequate planning headroom. For more
startup/load headroom use t3.large (8 GiB). None is proven by a Compose parse.
Do not silently change the instructor's t3.micro setting: obtain an exception
before using t3.medium. CPU credits and disk growth also need monitoring.

If t3.micro is strict, use the previously prepared four separate service
environments (one t3.micro each), with Kafka on separate capacity and the frontend
on suitable hosting. That can retain t3.micro for the application services but
requires instructor acceptance of multiple environments and separate Kafka.
There is no demonstrated solution meeting BOTH one t3.micro host and the full
unchanged stack; adding more hosts to this single Compose environment does not
divide the containers between them.

## Package and validate

From the repository root, with the previously downloaded official public CA:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./deployment/project4/Package-Compose.ps1 -RdsCaBundle ./deployment/project4/out/certs/global-bundle.pem
```

The script prints a fresh ignored out/compose-*/med-support-project4-compose.zip.
It allowlists nine files and writes Linux ZIP paths, including .platform and
.ebextensions. It packages only the public CA, frontend, Compose and proxy/config
files. No registry credentials, local envs, JARs, backups or user uploads enter
the archive. Run Compose config --quiet only with dummy process values during
preparation; never run this deployment file locally against RDS for validation.

Sources: [AWS Compose setup](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/docker-compose-quickstart.html),
[EB Docker env/proxy behavior](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/create_deploy_docker.container.console.html),
[AWS T3 capacities](https://aws.amazon.com/ec2/instance-types/t3/).

Validation during preparation: all four pinned GHCR manifests were accessible
anonymously and all their image blobs were downloaded/hashed successfully (18
unique blobs, no registry credentials required). Kafka and nginx linux/amd64
manifests were verified. Docker Compose v5.6.1 parsed the configuration using
dummy process values; no populated env file was loaded. Docker engine pull and
nginx/container runtime checks could not run because Docker Desktop is manually
paused; it was left paused to avoid resuming local databases. No full-stack load,
Gmail, RDS or public routing test has been performed.
