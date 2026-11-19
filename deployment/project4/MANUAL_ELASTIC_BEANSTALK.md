# Deploy Med Support Project 4 to one Elastic Beanstalk instance

Use the existing `med-support` application and `med-support-env` web environment in `us-east-1` if they are still present. The repository's Java SE bundle runs four Spring Boot processes and one Kafka broker on the same EC2 instance. Beanstalk's nginx serves the static frontend and proxies API and WebSocket requests. PostgreSQL stays on the existing private RDS instance, and Notification uses Gmail SMTP. You can complete the deployment from the AWS console and an instance shell.

| Process | Instance listener | Public route through nginx |
| --- | --- | --- |
| Identity (`web`) | `127.0.0.1:8083` | `/identity/` |
| Booking | `127.0.0.1:8084` | `/booking/` |
| Blood Request | `127.0.0.1:8085` | `/blood/` |
| Notification | `127.0.0.1:8086` | `/notification/`, `/ws/notifications` |
| Kafka 3.9.1 | Broker `127.0.0.1:19092`, controller `127.0.0.1:29093` | Private |

## 1. Prepare the application ZIP

Use the [prepared Project 4 ZIP](out/single-java-f7586d57c3314d43b0e7d663ea8110a1/med-support-project4-java17.zip) for the existing release. From the repository root in PowerShell, verify it before uploading:

```powershell
$zip = 'deployment/project4/out/single-java-f7586d57c3314d43b0e7d663ea8110a1/med-support-project4-java17.zip'
Get-FileHash -Algorithm SHA256 $zip
```

Expected SHA-256 is `1B4FA64DE1B1EF3E3CC3587191030946AADC47AD9B71A82267A4C052BC9621ED`. Upload the ZIP as-is. It has `Procfile`, `.platform/` and `.ebextensions/` at the archive root and contains the four executable JARs, frontend and public RDS CA. The root application JAR and the local Compose files are separate runtimes. [AWS Java SE platform](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-platform.html) · [AWS source bundle requirements](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/applications-sourcebundle.html)

For changed source or a checkout without the prepared ZIP, use JDK 17 and Maven from the repository root:

```powershell
mvn -version
foreach ($service in 'identity-service','booking-service','blood-request-service','notification-service') {
    mvn -B -f "$service/pom.xml" clean verify
    if ($LASTEXITCODE -ne 0) { throw "Build failed for $service" }
}
$ca = Join-Path $env:TEMP 'med-support-rds-global-bundle.pem'
Invoke-WebRequest -Uri 'https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem' -OutFile $ca
$zip = & ./deployment/project4/Package-SingleJava.ps1 -RdsCaBundle $ca
Get-FileHash -Algorithm SHA256 $zip
```

Save the ZIP and checksum together. The packaging script selects deployment files explicitly, so local `.env`, database backups and user uploads stay out of the release.

## 2. Check the environment and database

1. In **Elastic Beanstalk → Environments → med-support-env**, record the active application version. Under **Configuration**, use the **Java SE / Corretto 17 running on 64bit Amazon Linux 2023** branch, nginx, **Single instance**, x86_64 and the **All at once** application deployment policy. If the environment is absent and you are deploying from scratch, create a **Web server environment** with these settings. You can launch it with the sample application first to establish host access and obtain its public IPv4.
2. Confirm instance access via **EC2 → Instances → Connect → Session Manager**. If it reports offline, inspect the current EC2 instance profile and the agent's network access. Check the instance role's Beanstalk Web Tier permissions, Session Manager permissions and HTTPS connectivity to the required regional endpoints. The last repository handoff reported an offline session. Treat that as historical and test the current host.
3. Before an instance replacement or capacity change, preserve any host-local uploads and Kafka data off the instance, including a custom upload location if configured. Defaults are `/var/app/p4-uploads` and `/var/app/p4-kafka`. Verify you can restore those copies and arrange a maintenance window for a consistent backup. A single host can have downtime during deployments, and an instance replacement can also lose the on-host certificate and change the public IP.
4. Set capacity in **Elastic Beanstalk → Configuration**, where explicit settings can override the ZIP. The ZIP defaults to `t3.small` with four 192 MiB service heaps and a 256 MiB Kafka heap. **2 GiB capacity remains unverified**. For sustained use, start capacity testing at an account-permitted 4 GiB x86_64 size and measure memory under load. If using the 2 GiB demo profile, verify all checks in step 6 before relying on it. Check instance eligibility and cost in your account before applying changes. [AWS configuration precedence](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/command-options.html)
5. In **RDS → Databases**, verify the private PostgreSQL endpoint, database, application role and DB security group. The DB security group should allow its PostgreSQL port, normally TCP 5432, from the Beanstalk instance security group. Verify routing and DNS between the instance and RDS. The JDBC driver uses `sslmode=verify-full`, so use the actual RDS hostname.
6. Confirm the existing DB has the Project 4 schema, including the schema changes in [S005](../../runtime/project4/S005__core_features.sql), and that the application role has the required grants. Hibernate validates the schema and SQL initialization is disabled. For a separately provisioned empty database, review the order in [the consolidated schema file](../../project4-aws-schema.sql). From a host with private RDS access, verify the target, run S001–S004 from the Booking service manual schema directory, then Notification V001, then S005, with `psql` and `ON_ERROR_STOP=1`. Run as the schema owner, verify application role grants, and record each successful migration. On an existing DB, check that history and take a recoverable backup before applying any missing migration once. Preserve the existing PostgreSQL database through app upgrades and rollbacks.
7. Allow TCP 80 to the instance for ACME validation and TCP 443 for users. Keep backend, Kafka and DB ports private. The instance needs DNS and outbound HTTPS for Beanstalk dependencies and the pinned Kafka download from `archive.apache.org` on first deploy. It also needs outbound TCP 587 to `smtp.gmail.com` for STARTTLS. Read the current public IPv4 after capacity changes. [AWS instance HTTPS configuration](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/https-singleinstance.html)

## 3. Set Beanstalk environment properties

Open **Configuration → Updates, monitoring, and logging → Edit → Environment properties**. Use the following names for this bundle:

| Name | Value |
| --- | --- |
| `RDS_HOST` or `RDS_HOSTNAME` | Private RDS hostname, without a scheme or port |
| `RDS_DATABASE` or `RDS_DB_NAME` | Actual database name. Default is `med_support`. |
| `RDS_PORT` | Optional. Default is `5432`. |
| `RDS_USERNAME` | Existing PostgreSQL application role |
| `RDS_PASSWORD` | Password for that role, preferably the existing secret reference |
| `APP_FRONTEND_ORIGIN` | `https://PUBLIC_IPV4` for the included TLS setup, without a trailing slash |
| `JWT_SECRET` | Stable signing secret of at least 32 UTF-8 bytes |
| `IDENTITY_SERVICE_KEY` | Stable internal service credential of at least 32 characters |
| `NOTIFICATION_SERVICE_KEY` | Stable internal service credential of at least 32 characters |
| `MAIL_USERNAME` | Gmail account address |
| `MAIL_PASSWORD` | Gmail app password |
| `PORT` | `8083`, the default nginx upstream |
| `TZ` | `UTC` |
| `APP_UPLOAD_DIR` | Optional absolute writable path outside the app bundle. Default is `/var/app/p4-uploads`. |

Keep existing secrets when redeploying. If using AWS Secrets Manager or SSM Parameter Store, select the secret or parameter as the environment property source. Give the EC2 role access to the specific resource and its KMS key when applicable. Check stale environment properties for datasource, service URL, schema, SMTP or Spring JSON overrides that could conflict with the bundle. Kafka and service-to-service addresses are provided by the bundle. Admin seeding is disabled, so plan to use an existing authorized account if an admin workflow needs to be verified. Review pending mail before bringing Notification online. [AWS environment secret references](https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/AWSHowTo.secrets.env-vars.html)

## 4. Upload and deploy

1. On the environment overview, choose **Upload and deploy**. Select the ZIP from step 1 and enter a unique label, for example `project4-java17-manual-01`.
2. Follow **Events** until deployment succeeds. Beanstalk installs Kafka if needed, validates required settings, prepares the local directories and starts the Procfile processes. The first run can take longer while it downloads Kafka.
3. Check `http://ENVIRONMENT_URL/healthz` for `proxy alive`. This tests nginx only. The four prefixed actuator endpoints also allow unauthenticated HTTP health checks. Use HTTPS for login, API requests with credentials and WebSocket tests.

## 5. Enable trusted HTTPS on the instance

For the included public-IP configuration, connect to the instance after deployment. With ports 80 and 443 configured, run the following after reviewing the Let's Encrypt terms. Replace both placeholders:

```bash
sudo bash /var/app/current/scripts/enable-ip-https.sh PUBLIC_IPV4 CONTACT_EMAIL --agree-tos
sudo nginx -t
sudo systemctl status med-support-cert-renew.timer --no-pager
```

Open `https://PUBLIC_IPV4/` using the same address as `APP_FRONTEND_ORIGIN`. The certificate covers that IP and lasts just over six days. The script installs a twice-daily renewal timer. After replacement or an IP change, confirm the address, update `APP_FRONTEND_ORIGIN`, and issue a certificate for the current IP. [Let's Encrypt IP certificates in Certbot](https://letsencrypt.org/2026/03/11/shorter-certs-certbot)

## 6. Verify the deployment and know how to recover

On the EC2 instance:

```bash
sudo bash /var/app/current/scripts/diagnose-host.sh
sudo journalctl -k --since '30 minutes ago' | grep -Ei 'out of memory|oom-kill|killed process'
KAFKA_HEAP_OPTS='-Xms16m -Xmx64m' /opt/med-support/kafka_2.13-3.9.1/bin/kafka-topics.sh \
  --bootstrap-server 127.0.0.1:19092 --list
```

Confirm all five processes are active, the service and Kafka ports are loopback-only, and Kafka lists `booking-events` and `booking-events-dlt`. From local PowerShell, check each service through the actual HTTPS origin:

```powershell
$origin = 'https://PUBLIC_IPV4'
foreach ($service in 'identity','booking','blood','notification') {
    $health = Invoke-RestMethod "$origin/$service/actuator/health"
    if ($health.status -ne 'UP') { throw "$service is $($health.status)" }
    "$service UP"
}
```

Open the frontend and use authorized test accounts and synthetic records to verify login, a booking, the notification arriving in the inbox, and the WebSocket updating at `wss://PUBLIC_IPV4/ws/notifications`. Use a controlled recipient for an intentional email test. Watch free memory and service restarts during startup and these checks. Four green endpoints alone do not verify the booking-to-notification workflow.

| Symptom | Where to look |
| --- | --- |
| Upload or hook failure | EB Events, `/var/log/eb-engine.log` and `/var/log/eb-hooks.log`. Check ZIP structure, required settings and Kafka HTTPS egress. |
| Proxy 502 or service DOWN | Instance diagnostics and the affected process log under `/var/log/`. Check RDS reachability, TLS hostname, credentials, grants, schema and JVM restarts. |
| HTTPS certificate warning or HTTP 426 on API requests | Verify the IP certificate, ports 80/443, exact frontend origin and nginx TLS listener. |
| Booking works but notifications fail | Check the Kafka process, topic list and notification logs. Inspect the booking outbox and notification inbox through authorized access. Check Gmail separately if only email is missing. |
| Memory exhaustion | Inspect kernel OOM events, available RAM and service restart counts. If needed, schedule an approved larger single-instance type after protecting host-local data. |

For app rollback, use **Elastic Beanstalk → Application versions** to redeploy the previously active version to `med-support-env`, after confirming it can read the current schema. Restore any changed environment properties separately and rerun the same health and workflow checks. Keep host and database recovery copies until those checks pass.
