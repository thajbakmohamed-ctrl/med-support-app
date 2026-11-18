#!/bin/bash
set -eu
# Official Apache 3.9.1 distribution; digest obtained from Apache's HTTPS SHA512 file.
digest=1EA204BA73411737A275429CA976D440F007FF0957B90B19BE41DC5A4BAE52617267769BE9F0B5791714D0B3C4C760605BD426FAEA39EDD90763585523FA2CFE
home=/opt/med-support/kafka_2.13-3.9.1
if [[ -f "$home/.distribution-sha512" ]] && [[ "$(cat "$home/.distribution-sha512")" == "$digest" ]]; then
    exit 0
fi
if [[ -e "$home" ]]; then
    echo 'Unrecognized existing Kafka installation; refusing to overwrite it.' >&2
    exit 1
fi
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
curl --fail --silent --show-error --location --retry 3 --proto '=https' --tlsv1.2 \
    https://archive.apache.org/dist/kafka/3.9.1/kafka_2.13-3.9.1.tgz -o "$work/kafka.tgz"
printf '%s  %s\n' "$digest" "$work/kafka.tgz" | sha512sum --check --status
install -d -m 0755 /opt/med-support
tar -xzf "$work/kafka.tgz" -C /opt/med-support
printf '%s' "$digest" > "$home/.distribution-sha512"
