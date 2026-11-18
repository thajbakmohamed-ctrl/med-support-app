pipeline {
    agent { label "${params.AGENT_LABEL ?: 'built-in'}" }
    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        timestamps()
    }
    parameters {
        string(name: 'AGENT_LABEL', defaultValue: 'built-in',
            description: 'Reuse tonight on built-in. Changed-source mode needs an existing Linux agent with Java 17, Maven, Node, Docker and pwsh.')
        booleanParam(name: 'REUSE_VERIFIED_BUNDLE', defaultValue: true,
            description: 'Tonight: reuse the already tested ZIP; no test rerun or JAR rebuild. Set false only for changed source.')
        string(name: 'BUNDLE_NAME', defaultValue: 'med-support-project4-java17.zip',
            description: 'ZIP filename in /var/jenkins_home/project4-artifacts (no directory).')
        string(name: 'BUNDLE_SHA256', defaultValue: '96DFA981F7B1CED123995CF0E4765E8B0C3062991B638B43A8B8CD1378BF085C', description: 'Required SHA-256 for reuse mode.')
        booleanParam(name: 'DEPLOY_EXISTING', defaultValue: false,
            description: 'Opt in to deployment to existing med-support-env only; requires existing AWS credentials, t3.medium and backups.')
        string(name: 'EXISTING_EB_BUCKET', defaultValue: '', description: 'Existing EB S3 bucket; no bucket is created.')
        string(name: 'PUBLIC_HTTPS_URL', defaultValue: '', description: 'Trusted HTTPS URL, e.g. https://existing-public-IP (no trailing slash).')
    }
    environment {
        AWS_DEFAULT_REGION = 'us-east-1'
        AWS_PAGER = ''
    }
    stages {
        stage('Reuse tested artifact') {
            when { expression { params.REUSE_VERIFIED_BUNDLE } }
            steps {
                script {
                    if (!(params.BUNDLE_NAME ==~ /[A-Za-z0-9][A-Za-z0-9._-]*\.zip/)) {
                        error('BUNDLE_NAME must be a ZIP filename, not a path.')
                    }
                    if (!(params.BUNDLE_SHA256 ==~ /[A-Fa-f0-9]{64}/)) {
                        error('Supply the SHA-256 of the verified artifact.')
                    }
                }
                sh '''
                    set -eu
                    mkdir -p release
                    cp "/var/jenkins_home/project4-artifacts/$BUNDLE_NAME" release/med-support-project4-java17.zip
                    printf '%s  %s\n' "$BUNDLE_SHA256" release/med-support-project4-java17.zip | sha256sum --check
                '''
                echo 'Reused the verified artifact. Previously completed tests and JAR builds were NOT rerun.'
            }
        }
        stage('Checkout changed source') {
            when { expression { !params.REUSE_VERIFIED_BUNDLE } }
            steps {
                git branch: 'feature/project4-microservices', url: 'https://github.com/thajbakmohamed-ctrl/med-support-app.git'
            }
        }
        stage('Build and test changed source') {
            when { expression { !params.REUSE_VERIFIED_BUNDLE } }
            steps {
                sh '''
                    set -eu
                    java -version 2>&1 | grep -F 'version "17.'
                    command -v node
                    command -v docker
                    command -v pwsh
                    node --check project4-frontend/app.js
                    node --check project4-frontend/config.js
                    mvn -B -f runtime/project4/integration-tests/pom.xml -Pintegration test
                    for service in identity-service booking-service blood-request-service notification-service; do
                        mvn -B -f "$service/pom.xml" '-Dtest=*Test' verify
                    done
                    curl --fail --silent --show-error --location --proto '=https' --tlsv1.2 \
                        https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem --output public-rds-ca.pem
                '''
                pwsh '''
                    $zip = & ./deployment/project4/Package-SingleJava.ps1 -RdsCaBundle ./public-rds-ca.pem
                    New-Item -ItemType Directory -Path release -Force | Out-Null
                    Copy-Item -LiteralPath $zip -Destination release/med-support-project4-java17.zip
                '''
            }
        }
        stage('Archive verified release') {
            steps {
                sh 'sha256sum release/med-support-project4-java17.zip > release/SHA256SUMS'
                archiveArtifacts artifacts: 'release/med-support-project4-java17.zip,release/SHA256SUMS', fingerprint: true
            }
        }
        stage('Deploy existing environment') {
            when { expression { params.DEPLOY_EXISTING } }
            steps {
                input message: 'Deploy ONLY to existing med-support-env? Confirm backups, approved t3.medium, trusted HTTPS and existing-role AWS credentials. No new infrastructure/security changes will be made.'
                script {
                    if (!(params.EXISTING_EB_BUCKET ==~ /[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]/)) {
                        error('Supply the existing EB bucket name.')
                    }
                    if (!(params.PUBLIC_HTTPS_URL ==~ /https:\/\/[A-Za-z0-9][A-Za-z0-9.-]*/)) {
                        error('Supply the trusted public HTTPS origin without a path.')
                    }
                    env.DEPLOY_HELPER = params.REUSE_VERIFIED_BUNDLE ? '/var/jenkins_home/project4-artifacts/deploy-existing.sh' : 'deployment/project4/deploy-existing.sh'
                }
                // This helper is provisioned from this reviewed repository for reuse jobs.
                sh '''
                    set -eu
                    bash "$DEPLOY_HELPER" \
                        release/med-support-project4-java17.zip "$EXISTING_EB_BUCKET" "$PUBLIC_HTTPS_URL"
                '''
            }
        }
    }
    post {
        always {
            junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
        }
    }
}
