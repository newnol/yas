/**
 * Monorepo CI Pipeline - YAS (Yet Another Shop)
 *
 * Flow:
 *   1. Detect changed services from git diff
 *   2. Gitleaks secret scan (toàn bộ repo)
 *   3. Test  - Maven surefire (Java) / Jest (Node)
 *             - Publish JUnit results
 *             - Publish JaCoCo coverage report
 *   4. Coverage gate  — fail if line coverage < MIN_LINE_COVERAGE (default 70%)
 *   5. SonarQube analysis + Quality Gate
 *   6. Snyk dependency vulnerability scan
 *   7. Docker build & push  (main branch only)
 *
 * Required Jenkins plugins:
 *   - Pipeline, Pipeline: Stage View
 *   - Git, GitHub Branch Source
 *   - JUnit, JaCoCo
 *   - HTML Publisher
 *   - SonarQube Scanner
 *   - Snyk Security Scanner
 *   - Docker Pipeline
 *   - Credentials Binding
 *
 * Required credentials (Manage Jenkins > Credentials):
 *   - sonar-token          : Secret text  — SonarQube auth token
 *   - snyk-token           : Secret text  — Snyk auth token
 *   - docker-registry-creds: Username/password — Docker registry (ghcr.io)
 *
 * Required tools on the Jenkins agent PATH:
 *   - java / javac  (JDK 17+)
 *   - mvn           (Maven 3.9+)
 *   - node / npm    (Node 20+)  — only needed for storefront / backoffice
 *   - gitleaks      (optional)  — secret scanning
 *   - Snyk plugin   named "snyk-latest" in Global Tool Configuration
 */

// ─── Service registry ──────────────────────────────────────────────────────────
// Thêm / bỏ service tại đây khi cần
def JAVA_SERVICES = [
    'backoffice-bff',
    'cart',
    'customer',
    'inventory',
    'location',
    'media',
    'order',
    'payment',
    'payment-paypal',
    'product',
    'promotion',
    'rating',
    'recommendation',
    'search',
    'storefront-bff',
    'tax',
    'webhook',
]

def NODE_SERVICES = [
    'backoffice',
    'storefront',
]

// ─── Pipeline ──────────────────────────────────────────────────────────────────
pipeline {

    agent any

    environment {
        DOCKER_REGISTRY       = 'ghcr.io'
        DOCKER_IMAGE_PREFIX   = "${DOCKER_REGISTRY}/your-org/yas"
        MIN_LINE_COVERAGE     = '70'
        SONAR_HOST_URL        = 'http://your-sonar-host:9000'
    }

    parameters {
        booleanParam(
            name        : 'FORCE_BUILD_ALL',
            defaultValue: false,
            description : 'Bỏ qua git diff, build & test TẤT CẢ services'
        )
        string(
            name        : 'SERVICE_OVERRIDE',
            defaultValue: '',
            description : 'Chỉ build service này (vd: media). Để trống = tự detect từ git diff.'
        )
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 60, unit: 'MINUTES')
    }

    stages {

        // ── 1. Checkout ────────────────────────────────────────────────────────
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // ── 2. Detect changed services ─────────────────────────────────────────
        stage('Detect Changed Services') {
            steps {
                script {
                    def changedJava = []
                    def changedNode = []

                    // Priority 1: explicit SERVICE_OVERRIDE parameter
                    if (params.SERVICE_OVERRIDE?.trim()) {
                        def svc = params.SERVICE_OVERRIDE.trim()
                        if (JAVA_SERVICES.contains(svc)) {
                            changedJava = [svc]
                        } else if (NODE_SERVICES.contains(svc)) {
                            changedNode = [svc]
                        } else {
                            error "SERVICE_OVERRIDE '${svc}' không tồn tại trong danh sách service."
                        }
                        echo "SERVICE_OVERRIDE mode: chỉ build '${svc}'"

                    // Priority 2: FORCE_BUILD_ALL parameter
                    } else if (params.FORCE_BUILD_ALL) {
                        changedJava = JAVA_SERVICES.collect()
                        changedNode = NODE_SERVICES.collect()
                        echo "FORCE_BUILD_ALL = true: build tất cả services"

                    // Priority 3: auto-detect từ git diff
                    } else {
                        def changedFiles = getChangedFiles()
                        echo "Changed files:\n  ${changedFiles.join('\n  ')}"

                        // Không có previous commit (first run) → build tất cả
                        if (!env.GIT_PREVIOUS_SUCCESSFUL_COMMIT) {
                            echo "First run (no previous successful build) — building all services"
                            changedJava = JAVA_SERVICES.collect()
                            changedNode = NODE_SERVICES.collect()
                        } else {
                            boolean rootPomChanged = changedFiles.any { it == 'pom.xml' }

                            changedJava = rootPomChanged
                                ? JAVA_SERVICES.collect()
                                : JAVA_SERVICES.findAll { svc -> changedFiles.any { f -> f.startsWith("${svc}/") } }

                            changedNode = rootPomChanged
                                ? NODE_SERVICES.collect()
                                : NODE_SERVICES.findAll { svc -> changedFiles.any { f -> f.startsWith("${svc}/") } }
                        }
                    }

                    // Persist across stages via env vars
                    env.CHANGED_JAVA = changedJava.join(',')
                    env.CHANGED_NODE = changedNode.join(',')

                    if (!changedJava && !changedNode) {
                        echo 'No service changes detected — skipping CI.'
                        currentBuild.result = 'NOT_BUILT'
                    } else {
                        echo "Java services to build : ${changedJava ?: 'none'}"
                        echo "Node services to build : ${changedNode ?: 'none'}"
                    }
                }
            }
        }

        // ── 3. Gitleaks secret scan ────────────────────────────────────────────
        stage('Secret Scan (Gitleaks)') {
            when { expression { currentBuild.result != 'NOT_BUILT' } }
            steps {
                sh '''
                    if command -v gitleaks >/dev/null 2>&1; then
                        gitleaks detect \
                            --source . \
                            --config gitleaks.toml \
                            --report-format json \
                            --report-path gitleaks-report.json \
                            --exit-code 1 || true
                    else
                        echo "WARNING: gitleaks not found, skipping secret scan."
                    fi
                '''
                archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
            }
        }

        // ── 4. Test ────────────────────────────────────────────────────────────
        stage('Test') {
            when { expression { currentBuild.result != 'NOT_BUILT' } }
            steps {
                script {
                    def jobs = [:]

                    for (svc in splitEnv('CHANGED_JAVA')) {
                        def s = svc
                        jobs["Test · ${s}"] = {
                            sh """
                                mvn clean verify \
                                    -pl ${s} -am \
                                    -Dcheckstyle.output.file=${s}-checkstyle-result.xml \
                                    -B --no-transfer-progress
                            """
                        }
                    }

                    for (svc in splitEnv('CHANGED_NODE')) {
                        def s = svc
                        jobs["Test · ${s}"] = {
                            dir(s) {
                                sh 'npm ci --prefer-offline'
                                sh 'npm test -- --coverage --watchAll=false --ci'
                            }
                        }
                    }

                    parallel jobs
                }
            }
            post {
                always {
                    script {
                        // Publish JUnit results for every changed Java service
                        for (svc in splitEnv('CHANGED_JAVA')) {
                            junit(
                                testResults          : "${svc}/**/surefire-reports/TEST*.xml",
                                allowEmptyResults    : true,
                                keepLongStdio        : true
                            )
                            // JaCoCo HTML report
                            publishHTML(target: [
                                allowMissing         : true,
                                alwaysLinkToLastBuild: false,
                                keepAll              : true,
                                reportDir            : "${svc}/target/site/jacoco",
                                reportFiles          : 'index.html',
                                reportName           : "${svc} Coverage Report"
                            ])
                        }

                        // Publish Jest/lcov results for Node services
                        for (svc in splitEnv('CHANGED_NODE')) {
                            publishHTML(target: [
                                allowMissing         : true,
                                alwaysLinkToLastBuild: false,
                                keepAll              : true,
                                reportDir            : "${svc}/coverage/lcov-report",
                                reportFiles          : 'index.html',
                                reportName           : "${svc} Coverage Report"
                            ])
                        }
                    }
                }
            }
        }

        // ── 5. Coverage gate (>= MIN_LINE_COVERAGE %) ─────────────────────────
        stage('Coverage Gate') {
            when { expression { currentBuild.result != 'NOT_BUILT' } }
            steps {
                script {
                    int threshold = env.MIN_LINE_COVERAGE.toInteger()

                    for (svc in splitEnv('CHANGED_JAVA')) {
                        def xmlPath = "${svc}/target/site/jacoco/jacoco.xml"
                        if (!fileExists(xmlPath)) {
                            echo "WARN: ${xmlPath} not found — skipping coverage check for ${svc}"
                            continue
                        }

                        int pct = sh(
                            script: """
                                python3 - <<'EOF'
import xml.etree.ElementTree as ET
import sys

tree = ET.parse('${xmlPath}')
root = tree.getroot()
for counter in root.findall('counter'):
    if counter.get('type') == 'LINE':
        missed  = int(counter.get('missed', 0))
        covered = int(counter.get('covered', 0))
        total   = missed + covered
        print(int((covered / total * 100) if total > 0 else 0))
        sys.exit(0)
print(0)
EOF
                            """,
                            returnStdout: true
                        ).trim().toInteger()

                        echo "${svc}: line coverage = ${pct}%  (minimum: ${threshold}%)"

                        if (pct < threshold) {
                            error "${svc} coverage ${pct}% is below the required ${threshold}%"
                        }
                    }
                }
            }
        }

        // ── 6. SonarQube analysis ──────────────────────────────────────────────
        stage('SonarQube Analysis') {
            when { expression { currentBuild.result != 'NOT_BUILT' } }
            steps {
                script {
                    def jobs = [:]

                    for (svc in splitEnv('CHANGED_JAVA')) {
                        def s = svc
                        jobs["Sonar · ${s}"] = {
                            withSonarQubeEnv('SonarQube') {
                                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                                    sh """
                                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                                            -pl ${s} -am \
                                            -Dsonar.token=${SONAR_TOKEN} \
                                            -B --no-transfer-progress
                                    """
                                }
                            }
                        }
                    }

                    if (jobs) {
                        parallel jobs
                    } else {
                        echo 'No Java services changed — skipping SonarQube.'
                    }
                }
            }
        }

        // ── 7. SonarQube Quality Gate ──────────────────────────────────────────
        stage('Quality Gate') {
            when {
                allOf {
                    expression { currentBuild.result != 'NOT_BUILT' }
                    expression { splitEnv('CHANGED_JAVA').size() > 0 }
                }
            }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ── 8. Snyk vulnerability scan ─────────────────────────────────────────
        stage('Snyk Security Scan') {
            when { expression { currentBuild.result != 'NOT_BUILT' } }
            steps {
                script {
                    def jobs = [:]

                    for (svc in splitEnv('CHANGED_JAVA')) {
                        def s = svc
                        jobs["Snyk · ${s}"] = {
                            snykSecurity(
                                snykInstallation : 'snyk-latest',
                                snykTokenId      : 'snyk-token',
                                targetFile       : "${s}/pom.xml",
                                failOnIssues     : false,
                                severity         : 'high',
                                additionalArguments: "--all-projects --detection-depth=2"
                            )
                        }
                    }

                    for (svc in splitEnv('CHANGED_NODE')) {
                        def s = svc
                        jobs["Snyk · ${s}"] = {
                            snykSecurity(
                                snykInstallation : 'snyk-latest',
                                snykTokenId      : 'snyk-token',
                                targetFile       : "${s}/package.json",
                                failOnIssues     : false,
                                severity         : 'high'
                            )
                        }
                    }

                    if (jobs) {
                        parallel jobs
                    } else {
                        echo 'No services changed — skipping Snyk scan.'
                    }
                }
            }
        }

        // ── 9. Build & push Docker images (main branch only) ───────────────────
        stage('Build & Push Docker Images') {
            when {
                allOf {
                    branch 'main'
                    expression { currentBuild.result != 'NOT_BUILT' }
                }
            }
            steps {
                script {
                    def jobs = [:]
                    def allChanged = splitEnv('CHANGED_JAVA') + splitEnv('CHANGED_NODE')

                    for (svc in allChanged) {
                        def s = svc
                        jobs["Docker · ${s}"] = {
                            docker.withRegistry("https://${env.DOCKER_REGISTRY}", 'docker-registry-creds') {
                                def img = docker.build(
                                    "${env.DOCKER_IMAGE_PREFIX}-${s}:${env.BUILD_NUMBER}",
                                    "--file ${s}/Dockerfile ${s}"
                                )
                                img.push()
                                img.push('latest')
                                echo "Pushed ${env.DOCKER_IMAGE_PREFIX}-${s}:${env.BUILD_NUMBER}"
                            }
                        }
                    }

                    if (jobs) {
                        parallel jobs
                    } else {
                        echo 'No services changed — skipping Docker build.'
                    }
                }
            }
        }
    }

    // ── Post actions ────────────────────────────────────────────────────────────
    post {
        always {
            archiveArtifacts(
                artifacts       : '**/*-checkstyle-result.xml, **/target/site/jacoco/jacoco.xml',
                allowEmptyArchive: true
            )
            cleanWs()
        }
        success {
            echo "Pipeline SUCCEEDED for build #${env.BUILD_NUMBER}"
        }
        failure {
            echo "Pipeline FAILED — check logs above."
        }
        unstable {
            echo "Pipeline is UNSTABLE (tests passed but some checks warn)."
        }
    }
}

// ─── Helper functions ──────────────────────────────────────────────────────────

/**
 * Returns the list of files changed since the last successful build (or HEAD~1
 * on first run / manual trigger).
 */
def getChangedFiles() {
    def base = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT ?: 'HEAD~1'
    def output = sh(
        script: "git diff --name-only ${base} ${env.GIT_COMMIT ?: 'HEAD'} 2>/dev/null || git diff --name-only HEAD~1 HEAD",
        returnStdout: true
    ).trim()
    return output ? output.split('\n').toList() : []
}

/**
 * Reads a comma-separated environment variable and returns it as a List<String>.
 * Returns an empty list when the variable is absent or blank.
 */
def splitEnv(String varName) {
    def val = env[varName]
    return (val && val.trim()) ? val.split(',').toList() : []
}
