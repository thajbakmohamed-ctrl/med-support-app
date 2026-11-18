> Historical report: the user subsequently corrected RDS/SSM and approved a larger instance. Follow FINAL_ACTION_PLAN.md for current work; do not treat the old log errors below as current.

# Recover med-support-env (Corretto 17, us-east-1)

No AWS changes were made by this work. Keep t3.micro; do not switch to Docker EB.
Use the newly packaged Java ZIP, not an older ZIP from Downloads. No existing
files, data, containers or volumes were deleted or restarted.

## Evidence and causes

The downloaded `Downloads/BundleLogs-1794929634206.zip` records the deployment
attempt at **2026-11-17 15:29:37 UTC** stopping in `01-prepare.sh` with
`Missing required environment variable: RDS_HOST`. The four Procfile processes
were not started in that attempt. Static frontend files loading does not prove
the JVMs are running. A proxy with no listening upstream can return 502.
The current AWS URL timed out from the assistant's network; current live 502
responses/latest environment settings could not be independently verified.

Healthd was started, but repeatedly reported a missing
`/var/log/nginx/healthd/application.log.<hour>` file. This is consistent with the
failed deployment before the new proxy was activated. It does not prove an
enhanced-health IAM denial. Fix deployment first, then recheck logs/health IAM.
The bundle now explicitly selects enhanced health and leaves /healthz requests
in the generated EB access/health logs. /healthz is proxy liveness, not dependency
readiness; the four prefixed actuator endpoints must each return UP.

SSM logs show **AccessDenied for ssm:UpdateInstanceInformation** on the existing
`aws-elasticbeanstalk-ec2-role`, followed by failure to acquire management
credentials. This is a confirmed instance-role permissions problem, not a Java
or Kafka code problem. Console automation was unavailable (timed out); no IAM
changes were attempted. The SSM agent is already installed and attempted startup.

Kafka is absent on AWS according to the operator. The Java bundle intentionally
does not install a broker. Do not use localhost:9092 unless an actual broker is
running there, and do not point AWS to a Docker-only name such as p4-kafka.
Without a reachable broker/topics the full notification path is not operational.

## Repository corrections and local checks

- Deployment accepts RDS_HOST or EB's RDS_HOSTNAME, RDS_DATABASE or RDS_DB_NAME,
  and optional RDS_PORT. Private RDS certificate verification remains mandatory.
- Predeploy validation lists ALL missing settings without values, instead of
  forcing one failed deployment per missing variable. It still fails closed.
- Existing Procfile's four independent processes and ports were reviewed and
  retained. nginx preserves REST prefixes, Origin and WebSocket Upgrade; public
  internal APIs remain blocked. No booking/locking/scheduler code was changed.
- Local Kafka health CLI uses a separate 64 MiB heap, avoiding a broker-sized
  second JVM for each probe. The same command successfully described existing
  topics. No running Compose service was recreated to apply it.
- CI retains each tested executable JAR and assembles/uploads a single Java EB
  ZIP using the official public RDS CA. Existing BDD/concurrency gate and GHCR
  publication remain. The updated workflow has not been pushed or remotely run.
- Java 17: Identity 54, Booking 48, Blood Request 22, Notification 41 unit tests
  passed (165 total). Existing baseline/integration suite passed 21 tests,
  including concurrent slot/status races, rollback, Cucumber/HTTP contracts and
  actual Kafka transport. Only disposable Testcontainers databases were modified;
  no existing database/migration history was touched.
- Running local services: all four actuator health endpoints returned HTTP 200
  UP; frontend returned 200; unauthenticated protected APIs returned 401/403 and
  public hospitals returned 200. Both booking-events and booking-events-dlt exist
  with one partition and replication factor 1. WebSocket handshake returned 101
  and rejected an empty token with close code 1008. Authenticated workflows on
  existing user accounts and real Gmail sends were deliberately not exercised.

## Fix the EXISTING AWS environment (operator steps)

1. In IAM, open the EC2 instance role actually attached to med-support-env.
   Attach `AmazonSSMManagedInstanceCore` or equivalent least-privilege permissions.
   Keep existing EB permissions; do not replace the instance profile or attach
   account administrator access. Check that the instance also has EB WebTier
   enhanced-health permissions and the EB service role has enhanced-health
   permissions if No Data persists. IAM changes do not require a paid instance
   or plan upgrade. This repository does not apply them automatically.
2. Allow outbound HTTPS/DNS from the existing instance to SSM/ssmmessages,
   Elastic Beanstalk, S3 and the selected secret store. Reuse existing network
   egress; do not create paid NAT/endpoints automatically. Wait for the SSM
   credential retry (the captured log showed a long backoff). If you already
   have authorized SSH/EC2 access, `sudo systemctl restart amazon-ssm-agent`
   retries sooner. Never open SSH to the world. Confirm Session Manager Online.
3. EB > med-support > med-support-env > Configuration > environment properties
   (Software / Updates, monitoring and logging label varies by console version).
   Set these **before** Upload and deploy:

   | Name | Value (do not publish secret values) |
   |---|---|
   | RDS_HOST or RDS_HOSTNAME | Actual private RDS endpoint, no scheme/port |
   | RDS_DATABASE or RDS_DB_NAME | Actual existing Project 4 database |
   | RDS_PORT | 5432 unless the existing database uses another port |
   | RDS_USERNAME | Existing application role |
   | RDS_PASSWORD | Actual DB password through a secret reference |
   | APP_FRONTEND_ORIGIN | Exact working HTTPS origin, no trailing slash |
   | KAFKA_BOOTSTRAP_SERVERS | Reachable private broker DNS:port(s) |
   | JWT_SECRET | Stable random secret, at least 32 UTF-8 bytes |
   | IDENTITY_SERVICE_KEY | Matching internal credential, >=32 characters |
   | NOTIFICATION_SERVICE_KEY | Matching internal credential, >=32 characters |
   | MAIL_USERNAME / MAIL_PASSWORD | Gmail account / app password |
   | APP_UPLOAD_DIR (optional) | Writable private persistent mount |

   Use native Secrets Manager/SSM references if already available; grant the
   instance role access only to those ARNs/KMS keys. No new paid secret resources
   are created here. Remove conflicting old datasource/service URL/port/JSON
   overrides; the deployment property file supplies the loopback service URLs.
   Never copy local .env into a ZIP or paste passwords into commands/logs.
4. Existing RDS remains private: same reachable VPC/routes; DB security group
   allows 5432 from the EB instance group only. Confirm schema/permissions match
   the code, including any separately reviewed S005 upgrade. Do not blindly rerun
   migrations or enable Hibernate create/update to bypass schema failures.
5. Provide an **existing private Kafka broker** and the two existing topic
   definitions; allow its listener only from the EB instance group. If none is
   available, the complete system remains blocked by infrastructure. Do not
   deploy a paid broker or silently disable Kafka/notifications. Installing Kafka
   onto this already constrained t3.micro is not a validated solution. Kafka
   TLS/SASL settings must match the chosen broker; IAM auth would need additional
   configuration and is not implemented by this bundle.
6. The supplied EB URL is HTTP. Current deployment frontend intentionally
   requires HTTPS/WSS; HTTP-only hosting cannot safely exercise login/JWT flows.
   Use existing approved HTTPS ingress/certificate/domain. Do not create a paid
   ALB, buy a domain or weaken TLS automatically. Without HTTPS, restrict checks
   to unauthenticated health/smoke requests; full public acceptance is blocked.
7. Keep Corretto 17 / Amazon Linux 2023 and t3.micro. Choose **Upload and deploy**
   in the existing environment and upload the new ZIP as-is with a unique label
   only when the above dependencies/capacity decision are resolved. No new app,
   environment, Docker platform or plan upgrade is necessary. This work has not
   deployed the ZIP.

## Verification checklist after an authorized deployment

- EB Events: deployment succeeded; no hook failure. Four processes remain up:
  `systemctl is-active web booking blood notification`.
- Inspect `/var/log/eb-engine.log`, `/var/log/eb-hooks.log`, nginx error log and
  the four process stdout/journal logs. Look for missing placeholders, PostgreSQL
  TLS/auth/schema errors, broker connection failures and OOM kills; do not share
  unredacted logs or print `/opt/elasticbeanstalk/bin/get-config environment`.
- Instance listeners: `sudo ss -ltnp` shows 8083-8086 on 127.0.0.1 only. No public
  application, PostgreSQL, Kafka or controller ports; nginx is the public entry.
- On the instance, `curl -fsS http://127.0.0.1:8083/actuator/health` and repeat
  for 8084, 8085, 8086. Each must return UP. Check public /identity/actuator/health,
  /booking/actuator/health, /blood/actuator/health and /notification/actuator/health.
- Healthd: health log file exists after requests; agent is active and reporting.
  If No Data remains, inspect `healthd/daemon.log`, EB service role, instance role
  and outbound EB communication rather than editing booking code.
- HTTPS frontend/config.js use the same origin, REST routes have no 502, internal
  routes return 404, unauthenticated protected APIs reject access, and WSS uses
  /ws/notifications with first-frame JWT authentication. Test authorized user
  isolation and inbox delivery using synthetic records only when permitted.
- Kafka: broker reachable privately, both topics exist, consumer group connects,
  outbox drains, inbox receives the persisted event and retries do not duplicate
  inbox records. Verify email delivery only with explicit permission to send.
- Monitor `free -m`, `systemctl status` and `journalctl -k` for OOM kills, startup
  restarts and sustained memory pressure; do not infer capacity from static HTML.

## t3.micro constraint

Measured local service containers used approximately **337.8 + 415.7 + 352.3 +
426.8 = 1,532.6 MiB** combined, excluding Kafka (445.7 MiB), PostgreSQL and the
host OS. Local heaps/settings differ from the bounded Java EB bundle, so this is
not an AWS benchmark or proof of an exact minimum. It is evidence that the
complete workload has not been shown to fit 1 GiB. The EB four maximum heaps
alone total 1 GiB before native/OS memory. t3.micro remains unchanged; no larger
instance, swap workaround, paid Kafka or plan upgrade was applied. Full AWS
success cannot be claimed under these unresolved constraints.

References: [AWS enhanced health](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/health-enhanced.html),
[SSM permissions](https://docs.aws.amazon.com/systems-manager/latest/userguide/diagnosing-ec2-category-types.html),
[EB environment settings](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/environments-cfg-softwaresettings.html).

## Final artifact and proxy verification

Current artifact: `out/single-java-db82ab5acd204969bb08baf06aff9089/med-support-project4-java17.zip`
(288,999,899 bytes / 275.61 MiB), SHA-256
`7B00BE3DE208A6112D15C500D9FF09ADCD6146053A75FFC03A00DC31328EE583`.
The 17 allowlisted ZIP entries match the freshly tested JARs and current source
hashes; JDK 17 executable manifests/main classes/bytecode/CORS and credential
placeholders were verified. No envs, backups or user uploads were included.
Earlier ZIPs are retained to comply with the no-deletion instruction; use only
this newly identified artifact for the next authorized deployment.

The updated nginx snippets also ran in a temporary localhost-only nginx 1.28.0
proxy against the existing local services: frontend/config.js, /healthz and all
four prefixed actuator endpoints returned 200; unprefixed/internal API routes
returned 404; proxied WebSocket returned 101 and rejected an empty token with
1008; /healthz populated a health-format access log. The temporary proxy/binary
were stopped/removed. This used a representative EB server configuration with
Windows filesystem paths substituted for frontend roots, not the live EB host.
No live deployment or authorized-user/Gmail end-to-end success is claimed.
