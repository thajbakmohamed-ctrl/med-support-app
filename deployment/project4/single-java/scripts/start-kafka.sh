#!/bin/bash
set -eu
home=/opt/med-support/kafka_2.13-3.9.1
export LOG_DIR=/var/app/p4-kafka/logs
# Low-memory demo profile; t3.small capacity remains unproven.
export KAFKA_HEAP_OPTS='-Xms64m -Xmx256m'
"$home/bin/kafka-server-start.sh" "$PWD/config/kafka.properties" &
broker_pid=$!
cleanup() {
    kill -TERM "$broker_pid" 2>/dev/null || true
    wait "$broker_pid" 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 143' TERM
trap 'exit 130' INT
deadline=$((SECONDS + 180))
ready=0
while (( SECONDS < deadline )); do
    if ! kill -0 "$broker_pid" 2>/dev/null; then
        echo 'Kafka exited during startup; see its process log.' >&2
        exit 1
    fi
    if KAFKA_HEAP_OPTS='-Xms16m -Xmx64m' timeout 10 "$home/bin/kafka-topics.sh" \
        --bootstrap-server 127.0.0.1:19092 --list >/dev/null 2>&1; then
        ready=1
        break
    fi
    sleep 2
done
if (( ! ready )); then
    echo 'Kafka did not become ready within 180 seconds.' >&2
    exit 1
fi
for topic in booking-events booking-events-dlt; do
    KAFKA_HEAP_OPTS='-Xms16m -Xmx64m' "$home/bin/kafka-topics.sh" \
        --bootstrap-server 127.0.0.1:19092 --create --if-not-exists \
        --topic "$topic" --partitions 1 --replication-factor 1
done
wait "$broker_pid"
