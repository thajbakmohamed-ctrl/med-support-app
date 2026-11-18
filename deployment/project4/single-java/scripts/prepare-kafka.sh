#!/bin/bash
set -eu
home=/opt/med-support/kafka_2.13-3.9.1
data=/var/app/p4-kafka/data
export LOG_DIR=/var/app/p4-kafka/logs
if [[ -f "$data/meta.properties" ]]; then
    # Existing broker data is authoritative; never reformat it on redeployment.
    exit 0
fi
if find "$data" -mindepth 1 -print -quit | grep -q .; then
    echo 'Kafka data is nonempty without metadata; refusing to format existing data.' >&2
    exit 1
fi
cluster_id=$(runuser -u webapp -- env KAFKA_HEAP_OPTS='-Xms16m -Xmx64m' \
    "$home/bin/kafka-storage.sh" random-uuid)
runuser -u webapp -- env KAFKA_HEAP_OPTS='-Xms16m -Xmx64m' \
    "$home/bin/kafka-storage.sh" format -t "$cluster_id" -c "$PWD/config/kafka.properties"
