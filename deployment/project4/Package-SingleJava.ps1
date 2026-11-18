param([Parameter(Mandatory=$true)][string]$RdsCaBundle)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$caPath = (Resolve-Path -LiteralPath $RdsCaBundle).Path
$certificate = [IO.File]::ReadAllText($caPath)
if ($certificate -notmatch '-----BEGIN CERTIFICATE-----' -or $certificate -match 'PRIVATE KEY') {
    throw 'Supply only the public AWS RDS CA bundle.'
}
$files = @{'certs/global-bundle.pem'=$caPath}
foreach ($service in @('identity-service','booking-service','blood-request-service','notification-service')) {
    $jar = Join-Path $repo "$service/target/$service-0.0.1-SNAPSHOT.jar"
    $archive = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $reader = [IO.StreamReader]::new($archive.GetEntry('META-INF/MANIFEST.MF').Open())
        try { $manifest = $reader.ReadToEnd() } finally { $reader.Dispose() }
        if ($manifest -notmatch 'Main-Class: org.springframework.boot.loader.launch.JarLauncher' -or
            $manifest -notmatch 'Start-Class: com.medsupport\.' -or
            -not $archive.GetEntry('org/springframework/boot/loader/launch/JarLauncher.class')) {
            throw "Not an executable Spring Boot JAR: $service"
        }
    } finally { $archive.Dispose() }
    $files["services/$service.jar"] = $jar
}
foreach ($file in @('Procfile','scripts/start-service.sh','config/aws.properties',
    'frontend/config.js','.ebextensions/01-runtime.config',
    '.platform/hooks/predeploy/01-prepare.sh','.platform/nginx/conf.d/00-proxy.inc',
    '.platform/nginx/conf.d/01-forwarded.conf',
    '.platform/nginx/conf.d/elasticbeanstalk/01-project4.conf',
    'config/kafka.properties','config/ip-https.conf','scripts/install-kafka.sh',
    'scripts/prepare-kafka.sh','scripts/start-kafka.sh',
    '.platform/hooks/prebuild/01-kafka.sh',
    '.platform/hooks/postdeploy/02-restore-https.sh',
    'scripts/enable-ip-https.sh','scripts/diagnose-host.sh')) {
    $files[$file] = Join-Path $PSScriptRoot "single-java/$file"
}
foreach ($file in @('index.html','styles.css','app.js')) {
    $files["frontend/$file"] = Join-Path $repo "project4-frontend/$file"
}
$output = Join-Path $PSScriptRoot ('out/single-java-' + [Guid]::NewGuid().ToString('N'))
$bundle = Join-Path $output 'bundle'
New-Item -ItemType Directory -Path $bundle -Force | Out-Null
# Explicit allowlist; no env files, backups, uploads or unrelated repository files.
foreach ($entry in $files.GetEnumerator()) {
    $destination = Join-Path $bundle $entry.Key
    New-Item -ItemType Directory -Path (Split-Path $destination) -Force | Out-Null
    Copy-Item -LiteralPath $entry.Value -Destination $destination
}
$zipPath = Join-Path $output 'med-support-project4-java17.zip'
$zip = [IO.Compression.ZipFile]::Open($zipPath, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in Get-ChildItem -LiteralPath $bundle -Recurse -Force -File) {
        $name = $file.FullName.Substring($bundle.Length + 1).Replace('\', '/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $name) | Out-Null
    }
} finally { $zip.Dispose() }
Write-Output $zipPath
