[CmdletBinding()]
param([switch]$Integration, [switch]$ExistingUnitTests, [string]$Maven)
$ErrorActionPreference = 'Stop'
$repo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$userProfilePath = [Environment]::GetFolderPath('UserProfile')
if (-not $Maven) {
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) { $Maven = $command.Source }
    else {
        $cache = Join-Path $userProfilePath '.m2/wrapper/dists/apache-maven-3.9.16'
        $Maven = Get-ChildItem -LiteralPath $cache -Filter mvn.cmd -File -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
    }
}
if (-not $Maven -or -not (Test-Path -LiteralPath $Maven)) {
    throw 'Install Maven or pass -Maven with the path to a Maven executable. No services were started.'
}
if ($Integration) {
    & docker version --format '{{.Server.Version}}'
    if ($LASTEXITCODE -ne 0) { throw 'Docker unavailable. Integration tests NOT RUN; no fallback database is allowed.' }
}
$arguments = @()
$localRepository = Join-Path $userProfilePath '.m2/repository'
if (Test-Path -LiteralPath $localRepository) { $arguments += "-Dmaven.repo.local=$localRepository" }
if ($ExistingUnitTests) {
    foreach ($service in @('identity-service','booking-service','blood-request-service','notification-service')) {
        # *Test excludes unsafe/default-config ApplicationTests context smoke tests.
        & $Maven @arguments -f (Join-Path $repo "$service/pom.xml") '-Dtest=*Test' test
        if ($LASTEXITCODE -ne 0) { throw "$service unit tests failed." }
    }
}
$arguments += @('-f', (Join-Path $PSScriptRoot 'pom.xml'))
if ($Integration) { $arguments += '-Pintegration' }
& $Maven @arguments test
if ($LASTEXITCODE -ne 0) { throw 'Baseline failed. Inspect target/surefire-reports.' }
