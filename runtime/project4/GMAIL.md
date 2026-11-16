# Project 4 optional Gmail SMTP

Preparation only: no running containers were changed and no email was sent.
The base `docker-compose.project4.yml` remains unchanged: Mailpit on
`p4-mailpit:1025`, empty credentials, auth/TLS off; UI http://localhost:8025.
Only an explicit second `-f docker-compose.project4.gmail.yml` selects Gmail
for Identity and Notification. Other services retain Mailpit.

## Credentials and properties

Privately populate `P4_GMAIL_USERNAME` (full Gmail address) and
`P4_GMAIL_APP_PASSWORD` (Google App Password) in the ignored
`runtime/project4/.env`, or supply them through a trusted process environment.
Keep the template blank. Do not paste credentials into command arguments,
source, documentation, chat, logs or frontend configuration. Existing
`MAIL_USERNAME` / `MAIL_PASSWORD` in other configuration are neither read nor
copied. The override maps the dedicated names to both `MAIL_*` placeholders
and `SPRING_MAIL_*` properties. Missing or empty dedicated values fail validation.
Google App Password availability depends on the account's security policy and
2-Step Verification; use an App Password rather than the account password.

Gmail uses `smtp.gmail.com:587`, SMTP auth and required STARTTLS; implicit SSL
is off, certificate identity checks on, debug off. Exact JavaMail map keys are
provided in `SPRING_APPLICATION_JSON`, retaining the base Hibernate UTC property.
Identity's source defaults to Gmail and Notification's source defaults to
localhost Mailpit, but Project 4 Compose overrides both. Neither service calls
`setFrom`; `mail.from` supplies the default message From and `mail.smtp.from`
the envelope sender, both equal to the Gmail username. Existing SMTP timeouts
remain in each service's application properties. No Java change is required.

## Offline validation (safe now)

From the repository root in PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ./runtime/project4/Validate-Configuration.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File ./runtime/project4/Validate-MailConfiguration.ps1
docker compose --env-file runtime/project4/.env -f docker-compose.project4.yml --profile runtime config --quiet
# Only after privately setting the optional Gmail variables:
docker compose --env-file runtime/project4/.env -f docker-compose.project4.yml -f docker-compose.project4.gmail.yml --profile runtime config --quiet
```

The mail validator uses synthetic credentials and the public template, compares
the full rendered models allowing only mail differences, and checks rejection
of missing credentials. It never prints the rendered models. Avoid plain
`docker compose config`, `config --environment`, container inspection of env,
or SMTP debug output with real credentials. Docker environment variables are
accessible to users with Docker daemon access; restrict that access.

## Later Docker connectivity and exactly one message

These commands are provided for later execution; they were not run during
preparation. They create disposable Python containers on the existing isolated
Project 4 network, do not start Java applications, and mount only the probe
script read-only. Docker may need to pull the Python image.

```powershell
$probe = (Resolve-Path ./runtime/project4/Test-Gmail.py).Path
# DNS, TCP, STARTTLS, certificate verification; no auth and no message:
docker run --rm --network med-support-p4-local-network-v1 --mount "type=bind,source=$probe,target=/probe.py,readonly" python:3.13-alpine python /probe.py connect
# Later, deliberately send ONE message to an address you control:
docker run --rm -it --network med-support-p4-local-network-v1 --mount "type=bind,source=$probe,target=/probe.py,readonly" python:3.13-alpine python /probe.py send-one
```

The second command prompts inside the container for the address and hidden App
Password, then requires `SEND ONE`. No credentials are stored in Docker env,
files or shell history. This tests Docker SMTP, authentication and delivery;
it does not prove Spring runtime binding or the application's mail workflows.

## Later activation and rollback

Do not run activation during this preparation. Notification automatically polls
pending records and consumes Kafka, so switching its running service to Gmail
may send existing queued messages immediately. Review the queue and arrange a
separate controlled rollout before activation. Disabling recovery alone does
not disable normal sending. The disposable probe above avoids those workers.

When that rollout is explicitly authorized, these commands replace only the two
mail applications, without starting dependencies or building images:

```powershell
docker compose --env-file runtime/project4/.env -f docker-compose.project4.yml -f docker-compose.project4.gmail.yml --profile runtime up -d --no-deps --no-build identity-service notification-service
```

To return to Mailpit later, omit the Gmail override and recreate only those
services using the same project name and dedicated environment:

```powershell
docker compose --env-file runtime/project4/.env -f docker-compose.project4.yml --profile runtime up -d --no-deps --no-build --force-recreate identity-service notification-service
```

All PostgreSQL, Kafka, Mailpit and upload volumes stay attached and unchanged.
Do not use `down -v`, volume removal, SQL, migrations, or root/Project 3 Compose.
Queued emails may then go to Mailpit. Already sent real emails cannot be recalled.

References: [Google SMTP](https://developers.google.com/workspace/gmail/imap/imap-smtp),
[Google App Passwords](https://support.google.com/accounts/answer/185833),
[Jakarta Mail session properties](https://jakartaee.github.io/mail-api/docs/api/jakarta.mail/jakarta/mail/Session.html).
