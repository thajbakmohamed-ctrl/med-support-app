#!/bin/bash
# MANUAL OPT-IN ONLY, after approval for HTTPS listener/security group and CA terms.
# Usage: sudo bash /var/app/current/scripts/enable-ip-https.sh PUBLIC_IP EMAIL --agree-tos
set -eu
ip=${1:?Existing public IPv4 required}
email=${2:?Certificate contact email required}
[[ "${3:-}" == --agree-tos ]] || { echo 'Review CA terms and approve HTTPS setup before running with --agree-tos.' >&2; exit 1; }
[[ "$EUID" == 0 ]] || { echo 'Run as root after approval.' >&2; exit 1; }
python3 - "$ip" <<'PY'
import ipaddress, sys
address = ipaddress.ip_address(sys.argv[1])
if address.version != 4 or not address.is_global:
    raise SystemExit('Supply the existing EC2 public IPv4, not a new allocation.')
PY
# Does not create a domain, load balancer, EIP, secret resource or firewall rule.
# HTTP port 80 must already be reachable for ACME. Approve 443 separately in AWS.
dnf install -y python3.11 python3.11-pip
if [[ ! -x /opt/med-support/certbot/bin/certbot ]]; then
    python3.11 -m venv /opt/med-support/certbot
    /opt/med-support/certbot/bin/pip install certbot==5.10.0
fi
/opt/med-support/certbot/bin/certbot certonly --non-interactive --agree-tos \
    --email "$email" --cert-name med-support-ip --preferred-profile shortlived \
    --webroot --webroot-path /var/www/letsencrypt --ip-address "$ip" \
    --deploy-hook 'nginx -t && systemctl reload nginx'
install -m 0644 /var/app/current/config/ip-https.conf /etc/nginx/conf.d/med-support-ip-https.conf
nginx -t
systemctl reload nginx
cat > /etc/systemd/system/med-support-cert-renew.service <<'EOF'
[Unit]
Description=Renew existing Med Support IP certificate
[Service]
Type=oneshot
ExecStart=/opt/med-support/certbot/bin/certbot renew --quiet --deploy-hook "nginx -t && systemctl reload nginx"
EOF
cat > /etc/systemd/system/med-support-cert-renew.timer <<'EOF'
[Unit]
Description=Check short-lived IP certificate renewal twice daily
[Timer]
OnCalendar=*-*-* 00,12:00:00
RandomizedDelaySec=1800
Persistent=true
[Install]
WantedBy=timers.target
EOF
systemctl daemon-reload
systemctl enable --now med-support-cert-renew.timer
echo "Trusted HTTPS configured for https://$ip ; set APP_FRONTEND_ORIGIN to that exact origin."
