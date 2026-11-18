# Project 4 reduced-memory capacity check

Status: INCONCLUSIVE. Do not deploy or recommend t3.small as validated.

## Follow-up after permission to stop local Project 4

The user approved temporarily stopping only the existing Project 4 apps, Kafka,
frontend and mail, with all containers and volumes preserved. The follow-up
first attempted to record the current running-container inventory. That Docker
API call hung and returned HTTP 500 for `/containers/json` on
`dockerDesktopLinuxEngine`. The command aborted before any selective stop/start
action. A bounded server-version check returned 29.9.2, but container operations
remained unavailable. Thus engine-version responsiveness is not evidence that
container control is working. Current stop state and health cannot be confirmed.
No additional capacity run, rebuild, test suite, AWS action or cleanup was done.

Recovery likely requires restarting Docker Desktop. That affects unrelated
projects and Jenkins too, so it is outside the permission to stop only Project 4.
Obtain explicit approval or have the user perform that recovery. Do not use
Reset/Clean/Purge, delete containers, or remove volumes. Then re-inventory,
stop only approved Project 4 services, perform the isolated check, and restore
the exact recorded set. The prior disposable-resource stop request remains
unverified until container inspection works.

Only two deployment startup scripts changed. Candidate settings:

- Each existing Java 17 service JAR: `-Xms32m -Xmx192m -XX:+UseSerialGC`.
- Kafka 3.9.1: `-Xms64m -Xmx256m`; existing G1 defaults retained.
- Maximum combined Java heap: 1,024 MiB. Heap is not total process memory.
- Business logic, locks, concurrency, schedulers and connection pools unchanged.

Experiment used the existing four deployment JARs and cached official Kafka
archive. Five JVMs shared one Docker cgroup capped at 1,536 MiB with swap disabled,
reserving 512 MiB of a proposed 2 GiB host for Linux/nginx/EB/SSM agents.
PostgreSQL 17 and a mail sink were outside this budget, representing external
RDS/SMTP. A fresh disposable database received a schema-only copy, not user data;
no migrations were reapplied and no Gmail connection was used.

The disposable database needed to finish initialization before schema import.
After import, the disposable runner was restarted. A temporary harness file
update caused a shell error on an earlier attempt; it was corrected before the
last start. No JAR compilation or full test suite was run.

Docker Desktop reports 4,016,271,360 bytes total memory (about 3.74 GiB) and was
already running the existing Project 4, other projects and Jenkins. During the
last attempt, service logs stayed empty and no service readiness was established.
The partial cgroup samples reached about 191 MiB before inspection calls stalled.
Docker `top` subsequently returned HTTP 500 from dockerDesktopLinuxEngine.
These partial samples are NOT application working-set or capacity evidence.
Host contention is suspected, not proven as the sole cause.

Shutdown requested ONLY for `p4-capacity-run`, `p4-capacity-db` and
`p4-capacity-mail`. At report creation Docker had not acknowledged completion.
Those test containers, their anonymous database storage, network and ignored
`out/capacity-2g/` evidence are retained; no destructive cleanup was performed.
Existing containers, named volumes and database records were not stopped/reset.

Next: restore responsive Docker access; confirm the three disposable containers
are stopped. With permission, temporarily stop selected existing application
containers to free Docker host memory, preserving all volumes; then repeat ONLY
this focused check. Require four healthy services, synthetic login/booking,
Kafka outbox-to-inbox delivery, WebSocket updates, zero cgroup OOM events and
adequate measured headroom during startup and a short representative workload.
This is not a substitute for a final check on Corretto 17 / AL2023.

t3.small remains the smallest x86 Free Plan candidate, not a reliability claim.
If it fails a valid check, c7i-flex.large (4 GiB) is the next RAM size in the
published eligible x86 list, subject to approval and account/AZ availability.
Do not choose it automatically. t3.medium is absent from that published list.

The previous Jenkins-verified ZIP remains unchanged at
`out/single-java-90df4ec0b0474004a2117daec2d840d9/med-support-project4-java17.zip`,
SHA-256 `96DFA981F7B1CED123995CF0E4765E8B0C3062991B638B43A8B8CD1378BF085C`.
It still has the previous JVM limits and t3.medium deployment setting. No new
ZIP was packaged or staged in Jenkins, and no AWS setting was changed.
