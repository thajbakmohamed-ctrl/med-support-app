#!/bin/bash
set -eu
set +x
# EB injects environment properties. Fall back to its platform helper for the
# plain-text database username if it is absent from this hook's environment.
if [[ -z "${RDS_USERNAME:-}" && -x /opt/elasticbeanstalk/bin/get-config ]]; then
    RDS_USERNAME=$(/opt/elasticbeanstalk/bin/get-config environment -k RDS_USERNAME 2>/dev/null) || RDS_USERNAME=''
fi
# Validate presence only. Never log secret values or enable shell tracing.
missing=0
if [[ -z "${RDS_HOST:-${RDS_HOSTNAME:-}}" ]]; then
    echo 'Missing required environment variable: RDS_HOST (or RDS_HOSTNAME)' >&2
    missing=1
fi
for name in RDS_USERNAME RDS_PASSWORD APP_FRONTEND_ORIGIN \
    JWT_SECRET IDENTITY_SERVICE_KEY \
    NOTIFICATION_SERVICE_KEY MAIL_USERNAME MAIL_PASSWORD; do
    if [[ -z "${!name:-}" ]]; then
        echo "Missing required environment variable: $name" >&2
        missing=1
    fi
done
if (( missing )); then
    echo 'Configure all listed variables in the existing EB environment before retrying. No services were started.' >&2
    exit 1
fi
if [[ ! "$APP_FRONTEND_ORIGIN" =~ ^https://[A-Za-z0-9][A-Za-z0-9.-]*$ ]]; then
    echo 'APP_FRONTEND_ORIGIN must be an HTTPS DNS origin without a trailing slash.' >&2
    exit 1
fi
upload_dir="${APP_UPLOAD_DIR:-/var/app/p4-uploads}"
if [[ "$upload_dir" != /* || "$upload_dir" == / || "$upload_dir" == /var/app/current* ]]; then
    echo 'APP_UPLOAD_DIR must be an absolute storage path outside the application version.' >&2
    exit 1
fi
install -d -o webapp -g webapp -m 0700 "$upload_dir"
chmod 0755 /var/app/staging /var/app/staging/frontend
chmod 0644 /var/app/staging/frontend/*
install -d -o webapp -g webapp -m 0700 /var/app/p4-kafka /var/app/p4-kafka/data /var/app/p4-kafka/logs
install -d -m 0755 /var/www/letsencrypt
bash scripts/prepare-kafka.sh
