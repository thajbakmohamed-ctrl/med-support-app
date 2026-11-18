# Project 4 — current deployment

Use [FINAL_ACTION_PLAN.md](FINAL_ACTION_PLAN.md) as the single current plan.

The selected bundle runs four unchanged Java 17 JARs and a loopback-only Kafka
3.9.1 broker on one existing Java SE / Corretto 17 EB host. The user approved
changing the existing instance to t3.medium, subject to account eligibility; no
AWS change has been made here. If Free Plan blocks t3.medium, stop rather than
upgrade the account plan or choose another type without permission.

Jenkins is restored locally at http://127.0.0.1:8087 with its existing data volume.
The new Project 4 job and SHA-verified release are staged. Existing login and a
configuration reload are required; authentication was not disabled or bypassed.

No application tests or JAR builds were repeated in this continuation. Only new
configuration, pipeline syntax, Kafka download integrity and release packaging
were checked. Earlier readiness/recovery reports are historical; their missing
RDS/SSM findings must not be presented as current after the user's corrections.
