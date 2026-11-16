# Offline Compose validation. Never reads populated .env files or prints models.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$base = Join-Path $repo 'docker-compose.project4.yml'
$override = Join-Path $repo 'docker-compose.project4.gmail.yml'
$template = Join-Path $PSScriptRoot '.env.example'
function Check([bool]$ok, [string]$message) { if (-not $ok) { throw $message } }
function Canonical($value) {
    if ($null -eq $value) { return $null }
    if ($value -is [System.Management.Automation.PSCustomObject]) {
        $ordered = [ordered]@{}
        foreach ($key in ($value.PSObject.Properties.Name | Sort-Object)) {
            $ordered[$key] = Canonical $value.$key
        }
        return $ordered
    }
    if ($value -is [array]) { return ,@($value | ForEach-Object { Canonical $_ }) }
    return $value
}
function Model([bool]$gmail) {
    $arguments = @('compose', '--project-directory', $repo, '--env-file', $template, '-f', $base)
    if ($gmail) { $arguments += @('-f', $override) }
    $output = & docker @arguments --profile runtime config --format json
    Check ($LASTEXITCODE -eq 0) 'Compose validation failed (model withheld).'
    return (($output -join "`n") | ConvertFrom-Json)
}
$names = @('P4_GMAIL_USERNAME', 'P4_GMAIL_APP_PASSWORD')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    [Environment]::SetEnvironmentVariable($names[0], 'smtp-validation@example.invalid', 'Process')
    [Environment]::SetEnvironmentVariable($names[1], 'synthetic-validation-only', 'Process')
    $mailpit = Model $false
    $gmail = Model $true
    foreach ($service in @('identity-service', 'notification-service')) {
        $dev = $mailpit.services.$service.environment
        $real = $gmail.services.$service.environment
        Check ($dev.SPRING_MAIL_HOST -eq 'p4-mailpit' -and $dev.SPRING_MAIL_PORT -eq '1025') 'Mailpit endpoint changed.'
        Check ($dev.SPRING_MAIL_USERNAME -eq '' -and $dev.SPRING_MAIL_PASSWORD -eq '') 'Mailpit credentials must be empty.'
        foreach ($flag in @('AUTH', 'STARTTLS_ENABLE', 'STARTTLS_REQUIRED', 'SSL_ENABLE')) {
            Check ($dev."SPRING_MAIL_PROPERTIES_MAIL_SMTP_$flag" -eq 'false') 'Mailpit TLS/auth mismatch.'
        }
        Check ($real.SPRING_MAIL_HOST -eq 'smtp.gmail.com' -and $real.SPRING_MAIL_PORT -eq '587') 'Gmail endpoint mismatch.'
        Check ($real.SPRING_MAIL_USERNAME -eq 'smtp-validation@example.invalid' -and $real.MAIL_USERNAME -eq $real.SPRING_MAIL_USERNAME) 'Gmail username mismatch.'
        Check ($real.SPRING_MAIL_PASSWORD -eq 'synthetic-validation-only' -and $real.MAIL_PASSWORD -eq $real.SPRING_MAIL_PASSWORD) 'Gmail password reference mismatch.'
        $json = $real.SPRING_APPLICATION_JSON | ConvertFrom-Json
        foreach ($key in @('auth', 'starttls.enable', 'starttls.required', 'ssl.checkserveridentity')) {
            Check ($json."spring.mail.properties.mail.smtp.$key" -eq 'true') 'Gmail security property mismatch.'
        }
        Check ($json.'spring.mail.properties.mail.smtp.ssl.enable' -eq 'false') 'Implicit SSL must be off on 587.'
        foreach ($key in @('mail.from', 'mail.smtp.from')) {
            Check ($json."spring.mail.properties.$key" -eq $real.SPRING_MAIL_USERNAME) 'Sender mismatch.'
        }
        Check ($json.'spring.jpa.properties.hibernate.jdbc.time_zone' -eq 'UTC') 'Hibernate timezone lost.'
        # Remove only the permitted mail changes, then compare the entire model.
        foreach ($property in @($real.PSObject.Properties)) {
            if ($property.Name -match '^(MAIL_|SPRING_MAIL_)' -or $property.Name -eq 'SPRING_APPLICATION_JSON') {
                $real.($property.Name) = $dev.($property.Name)
            }
        }
    }
    # Compose retains the override's YAML helper extension in its rendered model.
    $gmail.PSObject.Properties.Remove('x-gmail-environment')
    Check (((Canonical $mailpit) | ConvertTo-Json -Depth 100 -Compress) -ceq ((Canonical $gmail) | ConvertTo-Json -Depth 100 -Compress)) 'Non-mail configuration changed.'
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $null, 'Process') }
    $ErrorActionPreference = 'Continue'
    & docker compose --env-file $template -f $base -f $override --profile runtime config --quiet 2>$null
    $missingCredentialsExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    Check ($missingCredentialsExit -ne 0) 'Gmail must fail closed without credentials.'
    Write-Output 'PASS: Mailpit default; Gmail endpoint, credentials references, TLS, senders, exact JavaMail keys; non-mail model unchanged; missing credentials rejected.'
    Write-Output 'NOT VERIFIED: Spring runtime binding, Docker egress, Google authentication or email delivery. No containers started and no email sent.'
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
}
