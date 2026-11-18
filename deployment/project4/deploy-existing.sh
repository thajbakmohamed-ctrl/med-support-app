#!/bin/bash
set -eu
set +x
bundle=${1:?ZIP required}
bucket=${2:?Existing EB bucket required}
public_url=${3:?Trusted HTTPS origin required}
[[ -f "$bundle" && "$bundle" == *.zip ]] || { echo 'ZIP is missing.' >&2; exit 1; }
[[ "$bucket" =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ ]] || exit 1
[[ "$public_url" =~ ^https://[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || exit 1
export AWS_DEFAULT_REGION=us-east-1 AWS_PAGER=''
command -v aws >/dev/null
command -v curl >/dev/null
aws sts get-caller-identity --query Account --output text >/dev/null
# Read only selected non-secret options; never dump environment variables.
app=$(aws elasticbeanstalk describe-environments --environment-names med-support-env \
    --query 'Environments[0].ApplicationName' --output text)
[[ "$app" == med-support ]] || { echo 'Existing target application mismatch.' >&2; exit 1; }
size=$(aws elasticbeanstalk describe-configuration-settings --application-name med-support \
    --environment-name med-support-env \
    --query "ConfigurationSettings[0].OptionSettings[?Namespace=='aws:ec2:instances' && OptionName=='InstanceTypes'].Value | [0]" --output text)
[[ "$size" == t3.small ]] || { echo 'Back up host data and apply the approved t3.small change first.' >&2; exit 1; }
policy=$(aws elasticbeanstalk describe-configuration-settings --application-name med-support \
    --environment-name med-support-env \
    --query "ConfigurationSettings[0].OptionSettings[?Namespace=='aws:elasticbeanstalk:command' && OptionName=='DeploymentPolicy'].Value | [0]" --output text)
[[ "$policy" == AllAtOnce ]] || { echo 'Require AllAtOnce to avoid provisioning additional deployment instances.' >&2; exit 1; }
aws s3api head-bucket --bucket "$bucket"
version="p4-java17-$(date -u +%Y%m%dT%H%M%SZ)-${BUILD_NUMBER:-manual}"
key="med-support/project4/$version.zip"
aws s3 cp "$bundle" "s3://$bucket/$key" --only-show-errors
aws elasticbeanstalk create-application-version --application-name med-support \
    --no-auto-create-application --version-label "$version" \
    --source-bundle "S3Bucket=$bucket,S3Key=$key" >/dev/null
aws elasticbeanstalk update-environment --environment-name med-support-env --version-label "$version" >/dev/null
aws elasticbeanstalk wait environment-updated --environment-names med-support-env
actual=$(aws elasticbeanstalk describe-environments --environment-names med-support-env \
    --query 'Environments[0].VersionLabel' --output text)
[[ "$actual" == "$version" ]] || { echo 'Requested version is not active.' >&2; exit 1; }
for service in identity booking blood notification; do
    curl --fail --silent --show-error --retry 12 --retry-delay 5 --max-time 15 \
        --proto '=https' "$public_url/$service/actuator/health" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'
done
echo "Verified four live HTTPS health endpoints for $version. Authenticated/Kafka/email workflows still require acceptance checks."
