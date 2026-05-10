pipeline {
    agent any

    environment {
        DOCKERHUB_USER    = "danieljpc1119"
        GKE_CLUSTER       = "kubernetes-instance-circle-guard"
        GKE_ZONE          = "us-central1-a"
        PROJECT_ID        = "ingesoft-v"
        JENKINS_OPS_URL   = "http://35.239.155.232:8080"
        JENKINS_OPS_JOB   = "circleguard-cd-infra"
        JENKINS_OPS_TOKEN = "circleguard-cd-trigger"
        SERVICES          = "auth-service identity-service promotion-service gateway-service notification-service form-service"
    }

    stages {

        // ════════════════════════════════════════════════════════════════
        // DEVELOP — build + unit tests + push :dev + trigger OPS dev
        // ════════════════════════════════════════════════════════════════

        stage("Build [develop]") {
            when { branch "develop" }
            steps {
                sh "./gradlew build -x test --no-daemon"
            }
        }

        stage("Unit Tests [develop]") {
            when { branch "develop" }
            steps {
                sh "./gradlew test --no-daemon"
                junit "**/build/test-results/test/*.xml"
            }
        }

        stage("Docker Build & Push :dev") {
            when { branch "develop" }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "dockerhub-credentials",
                    usernameVariable: "DOCKER_USER",
                    passwordVariable: "DOCKER_PASS"
                )]) {
                    sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
                    script {
                        env.SERVICES.split(" ").each { svc ->
                            sh """
                                docker build \\
                                  -t ${env.DOCKERHUB_USER}/circleguard-${svc}:dev \\
                                  -f services/circleguard-${svc}/Dockerfile .
                                docker push ${env.DOCKERHUB_USER}/circleguard-${svc}:dev
                            """
                        }
                    }
                }
            }
        }

        stage("Trigger OPS — DEV") {
            when { branch "develop" }
            steps {
                withCredentials([string(
                    credentialsId: "jenkins-ops-trigger-token",
                    variable: "OPS_TOKEN"
                )]) {
                    sh """
                        curl -f -u "jenkins:\${OPS_TOKEN}" \\
                          "\${JENKINS_OPS_URL}/job/\${JENKINS_OPS_JOB}/buildWithParameters\
?token=\${JENKINS_OPS_TOKEN}&IMAGE_TAG=dev&ENVIRONMENT=dev"
                    """
                }
            }
        }

        stage("Smoke Test DEV") {
            when { branch "develop" }
            steps {
                withCredentials([file(
                    credentialsId: "gcp-service-account-key",
                    variable: "GCP_KEY_FILE"
                )]) {
                    sh """
                        gcloud auth activate-service-account --key-file=\$GCP_KEY_FILE
                        gcloud container clusters get-credentials \${GKE_CLUSTER} \\
                          --zone \${GKE_ZONE} --project \${PROJECT_ID}
                        sleep 60
                        kubectl port-forward svc/gateway-service 8087:8087 -n circleguard-dev &
                        sleep 10
                        for i in \$(seq 1 5); do
                          curl -sf -X POST http://localhost:8087/api/v1/gate/validate \\
                            -H 'Content-Type: application/json' \\
                            -d '{"token":"healthcheck"}' && break
                          echo "Retry \$i/5..." && sleep 10
                        done
                        kill %1 2>/dev/null || true
                    """
                }
            }
        }

        // ════════════════════════════════════════════════════════════════
        // RELEASE/* — build + unit + integration + push :staging + E2E
        // ════════════════════════════════════════════════════════════════

        stage("Build [release]") {
            when { branch pattern: "release/.*", comparator: "REGEXP" }
            steps {
                sh "./gradlew build -x test --no-daemon"
            }
        }

        stage("Unit Tests [release]") {
            when { branch pattern: "release/.*", comparator: "REGEXP" }
            steps {
                sh "./gradlew test --no-daemon"
                junit "**/build/test-results/test/*.xml"
            }
        }

        stage("Integration Tests [release]") {
            when { branch pattern: "release/.*", comparator: "REGEXP" }
            steps {
                sh "./gradlew integrationTest --no-daemon"
                junit "**/build/test-results/integrationTest/*.xml"
            }
        }

        stage("Docker Build & Push :staging") {
            when { branch pattern: "release/.*", comparator: "REGEXP" }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "dockerhub-credentials",
                    usernameVariable: "DOCKER_USER",
                    passwordVariable: "DOCKER_PASS"
                )]) {
                    sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
                    script {
                        env.SERVICES.split(" ").each { svc ->
                            sh """
                                docker build \\
                                  -t ${env.DOCKERHUB_USER}/circleguard-${svc}:staging \\
                                  -f services/circleguard-${svc}/Dockerfile .
                                docker push ${env.DOCKERHUB_USER}/circleguard-${svc}:staging
                            """
                        }
                    }
                }
            }
        }

        stage("Trigger OPS — STAGE") {
            when { branch pattern: "release/.*", comparator: "REGEXP" }
            steps {
                withCredentials([string(
                    credentialsId: "jenkins-ops-trigger-token",
                    variable: "OPS_TOKEN"
                )]) {
                    sh """
                        curl -f -u "jenkins:\${OPS_TOKEN}" \\
                          "\${JENKINS_OPS_URL}/job/\${JENKINS_OPS_JOB}/buildWithParameters\
?token=\${JENKINS_OPS_TOKEN}&IMAGE_TAG=staging&ENVIRONMENT=stage"
                    """
                }
            }
        }

        stage("E2E Tests [release]") {
            when { branch pattern: "release/.*", comparator: "REGEXP" }
            steps {
                withCredentials([file(
                    credentialsId: "gcp-service-account-key",
                    variable: "GCP_KEY_FILE"
                )]) {
                    sh """
                        gcloud auth activate-service-account --key-file=\$GCP_KEY_FILE
                        gcloud container clusters get-credentials \${GKE_CLUSTER} \\
                          --zone \${GKE_ZONE} --project \${PROJECT_ID}
                        sleep 90
                        kubectl port-forward svc/auth-service      8180:8180 -n circleguard-stage &
                        kubectl port-forward svc/gateway-service   8087:8087 -n circleguard-stage &
                        kubectl port-forward svc/promotion-service 8088:8088 -n circleguard-stage &
                        kubectl port-forward svc/form-service      8086:8086 -n circleguard-stage &
                        sleep 15
                        ./gradlew e2eTest \\
                          -De2e.auth.url=http://localhost:8180 \\
                          -De2e.gateway.url=http://localhost:8087 \\
                          -De2e.promotion.url=http://localhost:8088 \\
                          -De2e.form.url=http://localhost:8086 \\
                          --no-daemon
                        kill %1 %2 %3 %4 2>/dev/null || true
                    """
                    junit "tests/e2e/build/test-results/test/*.xml"
                    publishHTML(target: [
                        reportDir: 'tests/e2e/build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'E2E Test Report'
                    ])
                }
            }
        }

        // ════════════════════════════════════════════════════════════════
        // MASTER — full pipeline + Locust + approval + PROD deploy + tag
        // ════════════════════════════════════════════════════════════════

        stage("Build [master]") {
            when { branch "master" }
            steps {
                sh "./gradlew build -x test --no-daemon"
            }
        }

        stage("Unit Tests [master]") {
            when { branch "master" }
            steps {
                sh "./gradlew test --no-daemon"
                junit "**/build/test-results/test/*.xml"
            }
        }

        stage("Integration Tests [master]") {
            when { branch "master" }
            steps {
                sh "./gradlew integrationTest --no-daemon"
                junit "**/build/test-results/integrationTest/*.xml"
            }
        }

        stage("Docker Build & Push :latest") {
            when { branch "master" }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "dockerhub-credentials",
                    usernameVariable: "DOCKER_USER",
                    passwordVariable: "DOCKER_PASS"
                )]) {
                    script {
                        env.SHORT_SHA = sh(
                            script: "git rev-parse --short HEAD",
                            returnStdout: true
                        ).trim()
                    }
                    sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
                    script {
                        env.SERVICES.split(" ").each { svc ->
                            sh """
                                docker build \\
                                  -t ${env.DOCKERHUB_USER}/circleguard-${svc}:latest \\
                                  -t ${env.DOCKERHUB_USER}/circleguard-${svc}:v${BUILD_NUMBER} \\
                                  -t ${env.DOCKERHUB_USER}/circleguard-${svc}:${env.SHORT_SHA} \\
                                  -f services/circleguard-${svc}/Dockerfile .
                                docker push ${env.DOCKERHUB_USER}/circleguard-${svc}:latest
                                docker push ${env.DOCKERHUB_USER}/circleguard-${svc}:v${BUILD_NUMBER}
                                docker push ${env.DOCKERHUB_USER}/circleguard-${svc}:${env.SHORT_SHA}
                            """
                        }
                    }
                }
            }
        }

        stage("Performance Tests [master]") {
            when { branch "master" }
            steps {
                withCredentials([file(
                    credentialsId: "gcp-service-account-key",
                    variable: "GCP_KEY_FILE"
                )]) {
                    sh """
                        gcloud auth activate-service-account --key-file=\$GCP_KEY_FILE
                        gcloud container clusters get-credentials \${GKE_CLUSTER} \\
                          --zone \${GKE_ZONE} --project \${PROJECT_ID}
                        kubectl port-forward svc/auth-service      8180:8180 -n circleguard-stage &
                        kubectl port-forward svc/gateway-service   8087:8087 -n circleguard-stage &
                        kubectl port-forward svc/promotion-service 8088:8088 -n circleguard-stage &
                        kubectl port-forward svc/form-service      8086:8086 -n circleguard-stage &
                        sleep 15
                        export AUTH_URL=http://localhost:8180 \\
                               GATEWAY_URL=http://localhost:8087 \\
                               PROMOTION_URL=http://localhost:8088 \\
                               FORM_URL=http://localhost:8086
                        pip install locust --quiet
                        bash tests/performance/run_locust.sh
                        kill %1 %2 %3 %4 2>/dev/null || true
                    """
                    publishHTML(target: [
                        reportDir: 'tests/performance/reports',
                        reportFiles: '*.html',
                        reportName: 'Locust Performance Report'
                    ])
                }
            }
        }

        stage("Release Notes [master]") {
            when { branch "master" }
            steps {
                script {
                    def lastTag = sh(
                        script: "git describe --tags --abbrev=0 2>/dev/null || echo ''",
                        returnStdout: true
                    ).trim()
                    def commits = sh(
                        script: lastTag
                            ? "git log ${lastTag}..HEAD --pretty=format:'- %s (%an)'"
                            : "git log --pretty=format:'- %s (%an)' -20",
                        returnStdout: true
                    ).trim()
                    def serviceList = env.SERVICES.split(" ")
                        .collect { "- ${env.DOCKERHUB_USER}/circleguard-${it}:latest (${env.SHORT_SHA})" }
                        .join("\n")
                    def notes = """\
## Release v${BUILD_NUMBER} — ${new Date().format('yyyy-MM-dd')}

### Cambios
${commits}

### Imágenes desplegadas
${serviceList}

### Métricas
- Build: v${BUILD_NUMBER}
- Commit: ${env.SHORT_SHA}
- Performance: ver reporte Locust adjunto
"""
                    writeFile file: 'RELEASE_NOTES.md', text: notes
                    archiveArtifacts artifacts: 'RELEASE_NOTES.md'
                }
            }
        }

        stage("Aprobación Manual — PRODUCCIÓN") {
            when { branch "master" }
            steps {
                timeout(time: 30, unit: "MINUTES") {
                    input(
                        message: "¿Desplegar v${BUILD_NUMBER} (${env.SHORT_SHA}) a PRODUCCIÓN?",
                        ok: "Aprobar despliegue",
                        submitterParameter: "APPROVER"
                    )
                }
            }
        }

        stage("Trigger OPS — PROD") {
            when { branch "master" }
            steps {
                withCredentials([string(
                    credentialsId: "jenkins-ops-trigger-token",
                    variable: "OPS_TOKEN"
                )]) {
                    sh """
                        curl -f -u "jenkins:\${OPS_TOKEN}" \\
                          "\${JENKINS_OPS_URL}/job/\${JENKINS_OPS_JOB}/buildWithParameters\
?token=\${JENKINS_OPS_TOKEN}&IMAGE_TAG=${env.SHORT_SHA}&ENVIRONMENT=prod"
                    """
                }
            }
        }

        stage("Tag Release [master]") {
            when { branch "master" }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: "token-circle-guard-ingesoft-v",
                    usernameVariable: "GIT_USER",
                    passwordVariable: "GIT_PASS"
                )]) {
                    sh """
                        git config user.email "jenkins@circleguard.edu"
                        git config user.name "Jenkins CI"
                        git tag v${BUILD_NUMBER}
                        git push https://\${GIT_USER}:\${GIT_PASS}@github.com/TU_USUARIO/circle-guard-public.git \\
                          v${BUILD_NUMBER}
                    """
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Pipeline exitoso — OPS job: ${JENKINS_OPS_URL}/job/${JENKINS_OPS_JOB}"
        }
        failure {
            echo "Pipeline fallido. Rollback manual: kubectl rollout undo deployment/gateway-service -n circleguard-prod"
        }
    }
}
