# Offline model validation only: no engine requests, containers, SQL or file writes.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$composePath = Join-Path $repoRoot 'docker-compose.project4.yml'
$templatePath = Join-Path $PSScriptRoot '.env.example'

function Assert-P4([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

# Capture the rendered configuration; never print it (it can contain env values).
$rendered = & docker compose --project-directory $repoRoot --env-file $templatePath -f $composePath --profile runtime config --format json
Assert-P4 ($LASTEXITCODE -eq 0) 'Compose model validation failed.'
$model = ($rendered -join "`n") | ConvertFrom-Json
Assert-P4 ($model.name -eq 'med-support-p4-local') 'Unexpected Compose project.'
Assert-P4 (@($model.services.PSObject.Properties).Count -eq 9) 'Expected nine services including topic initialization and frontend.'
$expectedPorts = @{ 'identity-service'=8083; 'booking-service'=8084; 'blood-request-service'=8085; 'notification-service'=8086 }
foreach ($service in $expectedPorts.Keys) {
    $app = $model.services.$service
    Assert-P4 ($app.environment.SPRING_DATASOURCE_URL -ceq 'jdbc:postgresql://p4-postgres:5432/med_support') "$service database target mismatch."
    Assert-P4 ($app.environment.SPRING_JPA_HIBERNATE_DDL_AUTO -eq 'validate') "$service schema writes enabled."
    Assert-P4 ($app.environment.SPRING_SQL_INIT_MODE -eq 'never') "$service SQL initialization enabled."
    Assert-P4 ($app.environment.SPRING_MAIL_HOST -eq 'p4-mailpit') "$service mail target mismatch."
    Assert-P4 ($app.environment.SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH -eq 'false') "$service SMTP auth override mismatch."
    Assert-P4 ($app.environment.SERVICES_IDENTITY_URL -eq 'http://identity-service:8083') "$service Identity URL mismatch."
    Assert-P4 ($app.environment.SPRING_KAFKA_BOOTSTRAP_SERVERS -eq 'p4-kafka:19092') "$service Kafka target mismatch."
    Assert-P4 ($app.profiles -contains 'runtime') "$service must require runtime profile."
    Assert-P4 ($app.ports[0].published -eq [string]$expectedPorts[$service]) "$service published port mismatch."
    Assert-P4 (Test-Path (Join-Path $app.build.context 'Dockerfile')) "$service missing Dockerfile."
}
$postgres = $model.services.'p4-postgres'
Assert-P4 ($postgres.environment.POSTGRES_DB -eq 'med_support') 'Unexpected database name.'
Assert-P4 ($postgres.ports[0].published -eq '5434' -and $postgres.ports[0].target -eq 5432) 'Project 4 must map host 5434 to PostgreSQL container 5432.'
Assert-P4 ($postgres.volumes.Count -eq 1 -and $postgres.volumes[0].source -eq 'p4-postgres-data') 'Unexpected PostgreSQL storage or initialization mounts.'
Assert-P4 ($model.volumes.'p4-postgres-data'.name -eq 'med-support-p4-local-postgres17-v1') 'Unsafe PostgreSQL volume name.'
foreach ($volume in $model.volumes.PSObject.Properties) {
    Assert-P4 (-not $volume.Value.external) 'External volumes are forbidden in this local configuration.'
}
foreach ($service in $model.services.PSObject.Properties) {
    foreach ($port in $service.Value.ports) {
        Assert-P4 ($port.host_ip -eq '127.0.0.1') 'Published ports must bind only to loopback.'
        Assert-P4 ($port.published -ne '5432') 'Protected host port 5432 must not be published.'
    }
}
Assert-P4 ($model.services.'p4-kafka'.environment.KAFKA_ADVERTISED_LISTENERS -eq 'INTERNAL://p4-kafka:19092,EXTERNAL://localhost:19092') 'Kafka listener mismatch.'
Assert-P4 ($model.services.'p4-kafka'.ports[0].published -eq '19092' -and $model.services.'p4-kafka'.ports[0].target -eq 9092) 'Project 4 must map host 19092 to Kafka external container listener 9092.'
Assert-P4 ($model.services.'notification-service'.environment.APP_NOTIFICATION_RECOVERY_ENABLED -eq 'false') 'Recovery must remain disabled for initial rollout.'
$initCommand = $model.services.'p4-kafka-init'.command -join ' '
Assert-P4 ($initCommand.Contains('--topic booking-events ') -and $initCommand.Contains('--topic booking-events-dlt ')) 'Topic provisioning missing.'
Assert-P4 ($model.services.'p4-frontend'.ports[0].published -eq '5500') 'Frontend origin mismatch.'
foreach ($migration in @(
    'booking-service/src/main/resources/db/manual/shared/S001__hospital_and_shared_audit.sql',
    'booking-service/src/main/resources/db/manual/shared/S002__identity_tables.sql',
    'booking-service/src/main/resources/db/manual/shared/S003__blood_requests_and_donation_slots.sql',
    'booking-service/src/main/resources/db/manual/shared/S004__donation_bookings_and_outbox.sql',
    'notification-service/src/main/resources/db/manual/V001__notification_service_schema.sql'
)) {
    Assert-P4 (Test-Path -LiteralPath (Join-Path $repoRoot $migration)) 'Required manual migration file is missing.'
}
Write-Output 'PASS: Compose syntax, service profiles, database isolation model, ports, mail target, Kafka listeners/topics, Dockerfiles and schema safety settings.'
Write-Output 'NOT VERIFIED: engine, image pulls/builds, physical volume identity, PostgreSQL schema, application readiness or runtime delivery.'
