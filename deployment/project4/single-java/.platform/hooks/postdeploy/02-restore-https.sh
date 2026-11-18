#!/bin/bash
set -eu
if [[ -f /etc/letsencrypt/live/med-support-ip/fullchain.pem && -f /etc/letsencrypt/live/med-support-ip/privkey.pem ]]; then
    install -m 0644 config/ip-https.conf /etc/nginx/conf.d/med-support-ip-https.conf
    nginx -t
    systemctl reload nginx
fi
