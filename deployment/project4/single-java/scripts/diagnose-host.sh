#!/bin/bash
set -u
# Read-only current-state report. No config values, keys or log bodies are printed.
for service in web booking blood notification kafka healthd amazon-ssm-agent; do
    printf '%s: ' "$service"
    systemctl is-active "$service" || true
done
free -m
df -h /var/app
ss -ltn | grep -E ':(80|443|8083|8084|8085|8086|19092|29093)[[:space:]]' || true
for port in 8083 8084 8085 8086; do
    printf 'health %s: ' "$port"
    curl --silent --show-error --max-time 5 "http://127.0.0.1:$port/actuator/health" || true
    printf '\n'
done
python3 <<'PY'
import json, subprocess
command = '/opt/elasticbeanstalk/bin/get-config'
def read(*args):
    result = subprocess.run([command, *args], capture_output=True, text=True)
    if result.returncode:
        return {}
    return json.loads(result.stdout)
plain = read('environment')
secrets = read('optionsettings', '-n', 'aws:elasticbeanstalk:application:environmentsecrets')
for name in ['RDS_HOST', 'RDS_HOSTNAME', 'RDS_USERNAME', 'RDS_PASSWORD',
             'APP_FRONTEND_ORIGIN', 'JWT_SECRET', 'IDENTITY_SERVICE_KEY',
             'NOTIFICATION_SERVICE_KEY', 'MAIL_USERNAME', 'MAIL_PASSWORD']:
    print(f'{name}: configured={bool(plain.get(name) or secrets.get(name))}')
PY
token=$(curl --silent --max-time 2 -X PUT -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' \
    http://169.254.169.254/latest/api/token) || token=''
if [[ -n "$token" ]]; then
    for field in instance-type public-ipv4; do
        printf '%s: ' "$field"
        curl --silent --max-time 2 -H "X-aws-ec2-metadata-token: $token" \
            "http://169.254.169.254/latest/meta-data/$field" || true
        printf '\n'
    done
fi
