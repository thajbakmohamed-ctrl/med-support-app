# Current deployment handoff

Use the newly packaged low-memory ZIP, not the older Jenkins build #2 artifact:

`out/single-java-f7586d57c3314d43b0e7d663ea8110a1/med-support-project4-java17.zip`

Its adjacent SHA256SUMS records the checksum. Contains 26 allowlisted entries,
four byte-identical executable Java17 JARs, native private Kafka, frontend,
Procfile, RDS public CA, nginx and deployment hooks. Bash syntax checks passed.
No full tests or Jenkins build repeated. No env files, user uploads, SQL backups
or private keys are packaged. Kafka is installed from the previously pinned
official archive on the host; it requires outbound HTTPS on first deployment.

Four service heaps: Xms32m/Xmx192m + SerialGC; Kafka heap Xms64m/Xmx256m.
The bundle requests one t3.small. Capacity is UNPROVEN; the user accepted this
demo risk. No AWS settings have been changed and this ZIP is not deployed.
The original Jenkins-verified artifact is retained unchanged for rollback.

## Actual current blocker

SSM Connect was attempted against i-0f82a182b81b5388c and returned
"instance is not connected". An enabled Connect link alone is not readiness.
The console shows an older credential error, not sufficient to diagnose current
IAM permissions. Do not reapply policies blindly.

Existing alternate EC2 Instance Connect mode reports TCP22 not authorized and
specifies the us-east-1 EC2 Instance Connect source range 18.206.107.24/29.
No network rule was changed. Proposed minimal temporary access, requiring
explicit approval: add inbound TCP22 from ONLY that range to the existing
instance security group sg-028943ec91aebf437, use ec2-user, inspect/preserve data,
repair SSM if appropriate, then remove ONLY that temporary rule. Never open
SSH to 0.0.0.0/0, nor public PostgreSQL/Kafka/backend ports. No new endpoint,
instance, load balancer, bucket or account-plan upgrade is needed for this route.

After host access, inspect counts/sizes/mounts (not file contents or credentials):

```bash
sudo du -sh /var/app/p4-uploads /var/app/p4-kafka /var/app/current/uploads /home/ec2-user/uploads 2>/dev/null
sudo find /var/app /home/ec2-user -maxdepth 4 -type d \( -iname '*upload*' -o -iname '*kafka*' \) -print
sudo findmnt -T /var/app
free -m
```

Check custom upload paths through configuration privately without printing
secret values. Preserve any nonempty host data OFF the instance with a verified
copy before changing EB capacity (instance replacement). An on-host archive
alone does not protect against replacement. Do not delete, reformat, or apply
migrations. Existing private RDS remains authoritative.

## After data protection

Verify account/AZ permits t3.small without Paid Plan conversion. Published Free
Plan list includes it. Retain x86_64, SingleInstance and AllAtOnce. No immutable
or additional-batch deployment. Modify capacity through EB, not EC2 alone.

Existing EB properties last observed: platform defaults, PORT, TZ and
RDS_PASSWORD as an existing Secrets Manager reference. Required additions:
RDS_HOST (or RDS_HOSTNAME), RDS_USERNAME, existing RDS_DATABASE if not med_support,
JWT_SECRET (stable >=32 UTF8 bytes), IDENTITY_SERVICE_KEY,
NOTIFICATION_SERVICE_KEY, MAIL_USERNAME, MAIL_PASSWORD, APP_FRONTEND_ORIGIN.
Use existing secret values/references; never put them in source or this ZIP.
Retain the existing RDS_PASSWORD reference and verify instance-role access.
KAFKA_BOOTSTRAP_SERVERS is fixed internally to 127.0.0.1:19092 by startup.
Service URLs are loopback; frontend API/WSS URLs follow its HTTPS origin.
Review pending Gmail deliveries before starting Notification; test messages
must not be sent without authorization.

Upload this ZIP to EXISTING med-support-env. Native TLS is opt-in:
scripts/enable-ip-https.sh PUBLIC_IP CONTACT_EMAIL --agree-tos.
This needs approval at action time for CA terms and for any required TCP443/80
security-group changes. Do not purchase a domain or add a load balancer. IP
certificates require automatic renewal. HTTP login/API/WS remains blocked.

Do not claim success until four HTTPS health endpoints, private Kafka topics,
outbox-to-inbox delivery, booking/login and WSS notifications are verified on
AWS, along with no OOM/restarts and adequate memory. No live success yet.
