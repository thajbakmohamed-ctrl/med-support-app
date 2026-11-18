# Final action plan — Project 4

## Current continuation

The newer low-memory t3.small bundle and current host-access blockers are in
[TONIGHT_DEPLOYMENT.md](TONIGHT_DEPLOYMENT.md). Follow that handoff first.
Jenkins build #2 succeeded for the older bundle; it does not verify the newer ZIP.
The sections below retain historical preparation details.

## Prepared now

- Final ZIP: `out/single-java-90df4ec0b0474004a2117daec2d840d9/med-support-project4-java17.zip`
- Size: 289,005,953 bytes (275.62 MiB).
- SHA-256: `96DFA981F7B1CED123995CF0E4765E8B0C3062991B638B43A8B8CD1378BF085C`.
- Reuses the four already-tested JARs; no application tests/builds repeated.
- Adds one supervised native Kafka 3.9.1 process on the SAME host; listeners bind
  only to 127.0.0.1:19092 and 127.0.0.1:29093. No MSK, new instance, bucket, EIP,
  load balancer or Docker EB environment is created. Apache archive SHA-512 was
  verified locally. On EB first deployment it downloads/verifies the same archive
  over HTTPS, so outbound access to archive.apache.org is required.
- Broker data: /var/app/p4-kafka/data. Existing metadata is never reformatted;
  nonempty unknown data causes a safe failure. Topic creation is idempotent for
  booking-events and booking-events-dlt, one partition/replica as before.
- Four JVM heaps capped at 256 MiB each; Kafka at 384 MiB; small admin CLI heaps.
  4 GiB is a practical trial target, not a measured AWS success guarantee.
- Frontend derives all API URLs from its HTTPS origin. HTTP shows a clear secure
  access message; nginx rejects HTTP API/WebSocket requests with 426, including
  spoofed X-Forwarded-Proto requests. Read-only health and ACME paths remain HTTP
  accessible. Native TLS uses the actual nginx scheme, not untrusted headers.
- Jenkinsfile supports SHA-verified reuse (default) and explicit future builds;
  deployment is opt-in, targets only existing med-support/med-support-env, and
  requires AllAtOnce to avoid additional deployment instances. No credentials
  are embedded. The updated job has NOT yet run in authenticated Jenkins.

## 1. Restore direct access and inspect CURRENT AWS state

RDS_HOST/RDS_PASSWORD and AmazonSSMManagedInstanceCore are corrected per the user.
Do not repeat those old fixes. Current cloud configuration could not be inspected:
AWS CLI/credentials are unavailable here; the UI tool stopped because it could
not confidently establish the browser URL. The public endpoint timed out from
this machine. Bring the med-support-env OVERVIEW page to the foreground with its
address bar visible and reply ready for a new UI session, or perform the steps
below yourself. Do not send credentials or secret values.

In the existing instance's Session Manager, read only:

```bash
for s in web booking blood notification kafka healthd amazon-ssm-agent; do
  printf '%s: ' "$s"; systemctl is-active "$s" || true
done
free -m
sudo ss -ltnp
```

Record the latest EB deployment Event, not the earlier downloaded logs. If SSM
is still offline, check the CURRENT role attachment and outbound connectivity;
do not assume the earlier missing-policy error still applies.

## 2. Confirm an allowed 4 GiB instance, then preserve host data

The user approved t3.medium, not a Paid Plan conversion or additional resources.
AWS's published newer Free Tier eligibility list includes c7i-flex.large and
m7i-flex.large but does not list t3.medium. Check what THIS account allows before
applying the change. If the console demands a Paid Plan upgrade for t3.medium,
STOP. A possible minimum alternative is the same existing host resized to
c7i-flex.large (4 GiB) IF marked eligible by this account; that different type
needs explicit approval and matching bundle/deployment-guard updates first.
Do not silently substitute a type or claim credits override plan restrictions.

Before an EB instance-size change, preserve any /var/app/p4-uploads and
/var/app/p4-kafka directories OFF the host using the existing approved admin
method. EB may replace the EC2 instance. RDS data remains in the existing RDS,
but these host directories do not survive replacement automatically. Do not
create snapshots/new backup buckets or delete old data without approval.

In EB > med-support > med-support-env > Configuration > Capacity, select the
approved available t3.medium, keeping ONE host and SingleInstance. Use AllAtOnce,
not immutable or additional-batch deployment. Do not resize EC2 directly while
leaving the EB launch configuration unchanged. No account upgrade or new AWS
infrastructure is part of this plan. Do not proceed if the account cannot meet
these limits.

## 3. Configure the one existing environment and deploy the final ZIP

Keep Corretto 17 / Amazon Linux 2023. Set these existing environment properties
(no .env file, no values in Git/ZIP):

| Name | Required value |
|---|---|
| RDS_HOST or RDS_HOSTNAME | Actual private RDS hostname |
| RDS_DATABASE or RDS_DB_NAME | Existing database, default med_support |
| RDS_PORT | Optional, default 5432 |
| RDS_USERNAME | Existing application role |
| RDS_PASSWORD | Existing password / existing secret reference |
| APP_FRONTEND_ORIGIN | https://CURRENT_PUBLIC_IPV4, no trailing slash |
| JWT_SECRET | Existing stable JWT secret, >=32 UTF-8 bytes |
| IDENTITY_SERVICE_KEY | Existing matching internal key |
| NOTIFICATION_SERVICE_KEY | Existing matching internal key |
| MAIL_USERNAME / MAIL_PASSWORD | Gmail account / app password |
| APP_UPLOAD_DIR | Optional prepared persistent private mount |

KAFKA_BOOTSTRAP_SERVERS is no longer required: the bundle explicitly uses its
own broker at 127.0.0.1:19092. No broker or controller security-group rule is
needed. RDS remains private with 5432 allowed only from the EB instance group.
Do not overwrite schema/data or run migrations to bypass validation errors.

Get the current public IPv4 from EC2 after the approved resize. Before TLS is
installed, use HTTP only for unauthenticated health checks, never login.
Use Upload and deploy on the EXISTING EB environment, select the final ZIP as-is
and a new version label. Do not choose Create environment, Docker or a paid plan.
On-host download of Apache Kafka occurs once; allow sufficient deployment time. The existing Gmail scheduler can send pending messages as soon as Notification starts: review/approve that delivery before deployment; no Gmail was sent by this preparation.
After success run `sudo bash /var/app/current/scripts/diagnose-host.sh`: this
prints statuses, memory, listeners and configured-variable booleans, not secrets.

## 4. Enable FREE trusted IP HTTPS only after approval

No purchased domain or load balancer is required: Let's Encrypt now supports
trusted public-IP certificates using Certbot 5.4+ and its shortlived profile.
The certificate lasts about SIX DAYS, so renewal is mandatory.

This step is OPT-IN and has NOT been executed. Approve (a) public 443 on the
existing instance security group, (b) public 80 for ACME validation if currently
restricted, (c) Certbot installation and CA terms. Do not open 8083-8086, 19092,
29093 or PostgreSQL publicly. No AWS resource creation is required.

After approval, use SSM on the existing host:

```bash
sudo bash /var/app/current/scripts/enable-ip-https.sh ACTUAL_PUBLIC_IPV4 YOUR_CONTACT_EMAIL --agree-tos
```

The script installs pinned official Certbot 5.10.0 in a Python 3.11 virtualenv,
requests a trusted IP certificate, installs the prepared nginx TLS listener and
sets a twice-daily renewal timer. It does NOT change AWS security groups, buy a
domain or provision resources. It does not disable certificate validation.
The opt-in listener is restored after future app deployments only if certificate
files already exist. EC2 replacement loses these files; a changed IP needs a new
certificate. Never use a self-signed warning bypass for the presentation.

Open `https://ACTUAL_PUBLIC_IPV4`, NOT `https://...elasticbeanstalk.com` with an
IP-only certificate. Frontend API and WSS URLs automatically follow this origin.
Without permission/reachability for trusted HTTPS, secure public login remains
blocked; do not fall back to password/JWT over HTTP.

## 5. Finish Jenkins without repeating work

Jenkins now runs at http://127.0.0.1:8087 using existing jenkinsvol1. The original
jenkins-local container is stopped and retained. Booking still owns 8084. Only
one controller should use this volume at a time. Existing job food and Jenkins
security settings were not changed.

A new med-support-project4 job configuration and the verified ZIP/deploy helper
are staged in Jenkins home, owned by the Jenkins user. Existing authentication
blocks anonymous API access (403); no password reset, API token creation or
security bypass was attempted. Exact manual actions:

1. Sign in at http://127.0.0.1:8087 with the existing Jenkins account.
2. Manage Jenkins > Reload Configuration from Disk (or open /reload and confirm).
3. Open med-support-project4 > Build with Parameters.
4. Keep AGENT_LABEL=built-in, REUSE_VERIFIED_BUNDLE=true and DEPLOY_EXISTING=false.
   The ZIP name and SHA are already supplied. Run the job. It verifies/copies and
   archives the release; it does NOT rerun the 186 already-passed tests or rebuild
   JARs. Check Finished: SUCCESS and download the archived ZIP/SHA256SUMS.
5. For an actual Jenkins deployment, first supply an EXISTING role-backed AWS
   credential chain and AWS CLI on the executor, the existing EB bucket and the
   trusted HTTPS URL; opt into DEPLOY_EXISTING and approve the promotion input.
   It checks existing app/type/AllAtOnce, uploads to the existing bucket, updates
   the existing environment and verifies four live HTTPS health endpoints. It
   does not create infrastructure or modify security settings. No AWS credential
   chain/CLI is currently available on this controller, so this CD stage is not
   yet verified. Console upload in step 3 is the quickest current deployment path.

For FUTURE changed source, select REUSE_VERIFIED_BUNDLE=false on an existing
Linux agent with JDK17, Maven, Node, Docker access and pwsh. The current controller
uses Java21 and lacks that full build toolchain. Do not expose the Docker socket
or create a privileged agent without approval. Existing GitHub Actions already
provides the Java17/Docker CI path. New Jenkinsfile/source changes have not been
committed/pushed; inline job staging permits tonight's reuse run without a push.

## 6. Verify before claiming deployment success

- Four public HTTPS /identity/actuator/health, /booking/actuator/health,
  /blood/actuator/health and /notification/actuator/health return UP.
- Native Kafka process remains running; both topics exist and broker/controller
  ports listen only on loopback. Check /var/log/kafka.stdout.log or its journal.
- HTTP API/WS requests return 426 even with a forged X-Forwarded-Proto header.
- HTTPS login with synthetic accounts, booking/concurrency, outbox-to-inbox event
  delivery and WSS updates work; test Gmail sends only when explicitly approved.
- healthd reports current data; no JVM restarts/OOM kills; capture CPU/memory
  after all FIVE JVMs are running. This is still an unverified 4 GiB demo target.
- Jenkins has a real successful reuse/promote build, not only a syntax check.

No live AWS success is claimed. The current RDS and SSM corrections are accepted
per the user, not re-diagnosed from old logs. The remaining blockers are direct
AWS access, allowed instance capacity/plan, HTTPS approval/configuration and
Jenkins authentication/CD credentials. Local app tests are already complete.

Sources: [AWS eligible instance list](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-free-tier-usage.html),
[Let's Encrypt IP certificates/Certbot](https://letsencrypt.org/2026/03/11/shorter-certs-certbot),
[Apache Kafka 3.9 KRaft](https://kafka.apache.org/39/operations/kraft/).

## Validation recorded for this continuation

Only changed work was validated: Groovy syntax parsed with Jenkins' installed
Groovy version; all deployment Bash syntax passed; changed frontend JS syntax
passed; the official Kafka archive matched the pinned SHA-512; the final ZIP has
26 allowlisted source-matching entries and unchanged service JARs. A temporary
local nginx check verified the new TLS config and HTTP gate: login over HTTP
returned 426 with both normal and forged X-Forwarded-Proto headers; HTTPS with
an explicitly validated temporary test certificate reached the backend's normal
401/403 authentication response. The temporary proxy/key/certificate were removed;
no OS/browser trust settings changed. No existing application tests/JAR builds
were repeated. Live Kafka startup, Let's Encrypt issuance, Jenkins build execution
and actual AWS endpoints remain unverified until the manual steps above occur.
