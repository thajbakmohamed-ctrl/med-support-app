#!/bin/bash
set -eu
set +x
case "${1:-}:${2:-}" in
    identity-service:8083|booking-service:8084|blood-request-service:8085|notification-service:8086) ;;
    *) echo 'Unexpected Project 4 service/port pair.' >&2; exit 1 ;;
esac
# A predeploy shell assignment does not carry over to Procfile processes.
if [[ -z "${RDS_USERNAME:-}" && -x /opt/elasticbeanstalk/bin/get-config ]]; then
    RDS_USERNAME=$(/opt/elasticbeanstalk/bin/get-config environment -k RDS_USERNAME 2>/dev/null) || RDS_USERNAME=''
    export RDS_USERNAME
fi
if [[ "$1" == booking-service || "$1" == notification-service ]]; then
    deadline=$((SECONDS + 180))
    until (exec 3<>/dev/tcp/127.0.0.1/19092) 2>/dev/null; do
        if (( SECONDS >= deadline )); then
            echo 'Local Kafka did not start within 180 seconds; inspect kafka process logs.' >&2
            exit 1
        fi
        sleep 2
    done
fi
# exec lets EB supervise and signal each JVM independently. No services are merged.
# Low-memory demo profile; t3.small capacity remains unproven.
exec java -Xms32m -Xmx192m -XX:+UseSerialGC -Duser.timezone=UTC \
    -jar "services/$1.jar" \
    --spring.config.additional-location=file:./config/aws.properties \
    --server.address=127.0.0.1 \
    --spring.kafka.bootstrap-servers=127.0.0.1:19092 \
    --server.port="$2"
