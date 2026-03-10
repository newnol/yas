pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        DEFAULT_BASE_BRANCH = 'main'
    }

    tools {
        jdk 'JDK21'
        maven 'Maven 3.9'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Gitleaks - Secret Scanning') {
            steps {
                sh '''
                    GITLEAKS_VERSION="8.22.1"
                    if ! command -v gitleaks &> /dev/null; then
                        curl -sSfL "https://github.com/gitleaks/gitleaks/releases/download/v${GITLEAKS_VERSION}/gitleaks_${GITLEAKS_VERSION}_linux_x64.tar.gz" \
                            | tar xz -C /tmp/
                    fi
                    GITLEAKS_CMD=$(command -v gitleaks || echo "/tmp/gitleaks")
                    ${GITLEAKS_CMD} detect \
                        --source . \
                        --report-format json \
                        --report-path gitleaks-report.json \
                        --verbose \
                        --exit-code 1
                '''
            }
            post {
                always {
                    archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
                }
            }
        }

        stage('Detect changed modules') {
            steps {
                script {
                    sh "git fetch --no-tags --prune origin +refs/heads/*:refs/remotes/origin/*"

                    def baseRef = ''
                    if (env.CHANGE_ID) {
                        baseRef = "origin/${env.CHANGE_TARGET ?: env.DEFAULT_BASE_BRANCH}"
                    } else {
                        baseRef = "origin/${env.DEFAULT_BASE_BRANCH}"
                    }

                    def diffCmd = "git diff --name-only ${baseRef}...HEAD"
                    def changedFilesRaw = sh(script: diffCmd, returnStdout: true).trim()
                    def changedFiles = changedFilesRaw ? changedFilesRaw.split('\n') : []

                    echo "BRANCH_NAME: ${env.BRANCH_NAME}"
                    echo "Base for diff: ${baseRef}"
                    echo "origin/${env.DEFAULT_BASE_BRANCH}: " + sh(script: "git rev-parse ${baseRef}", returnStdout: true).trim()
                    echo "HEAD: " + sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    echo "Changed files:\n- " + (changedFiles ? changedFiles.join("\n- ") : "(none)")

                    def modules = [
                        'customer',
                        'cart',
                        'order',
                        'product',
                        'tax',
                        'media',
                        'search',
                        'webhook',
                        'common-library'
                    ]

                    def impacted = modules.findAll { m ->
                        changedFiles.any { f -> f.startsWith("${m}/") }
                    }

                    if (impacted.isEmpty()) {
                        echo "No impacted modules detected (only service-folder changes are considered). Marking build as NOT_BUILT."
                        currentBuild.result = 'NOT_BUILT'
                        env.IMPACTED_MODULES = ''
                    } else {
                        env.IMPACTED_MODULES = impacted.join(',')
                        echo "Impacted modules: ${env.IMPACTED_MODULES}"
                    }
                }
            }
        }

        stage('Build impacted modules') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                cache(maxCacheSize: 500, defaultBranch: 'main', caches: [
                    arbitraryFileCache(
                        path: '.m2/repository',
                        includes: '**/*',
                        cacheValidityDecidingFile: 'pom.xml'
                    )
                ]) {
                    script {
                        def mods = env.IMPACTED_MODULES.split(',') as List
                        def pl = mods.join(',')
                        sh "mvn -B clean install -pl ${pl} -am -DskipTests"
                    }
                }
            }
        }

        stage('Test & Coverage Check (>70%)') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                cache(maxCacheSize: 500, defaultBranch: 'main', caches: [
                    arbitraryFileCache(
                        path: '.m2/repository',
                        includes: '**/*',
                        cacheValidityDecidingFile: 'pom.xml'
                    )
                ]) {
                    script {
                        def mods = env.IMPACTED_MODULES.split(',') as List
                        def pl = mods.join(',')
                        sh "mvn -B verify -DskipITs -pl ${pl} -am"
                    }
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true, skipMarkingBuildUnstable: true

                    script {
                        def mods = (env.IMPACTED_MODULES?.trim() ? env.IMPACTED_MODULES.split(',') : []) as List
                        mods.each { m ->
                            jacoco(
                                execPattern: "${m}/target/jacoco.exec",
                                classPattern: "${m}/target/classes",
                                sourcePattern: "${m}/src/main/java",
                                exclusionPattern: '**/*Test*.class',
                                minimumLineCoverage: '70',
                                minimumBranchCoverage: '70'
                            )

                            publishHTML([
                                allowMissing: true,
                                alwaysLinkToLastBuild: true,
                                keepAll: true,
                                reportDir: "${m}/target/site/jacoco",
                                reportFiles: 'index.html',
                                reportName: "${m} Coverage Report",
                                reportTitles: "Code Coverage Report (${m})"
                            ])
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                withSonarQubeEnv('SonarQube') {
                    script {
                        def mods = env.IMPACTED_MODULES.split(',') as List
                        def pl = mods.join(',')
                        sh """
                            mvn -B sonar:sonar \
                                -pl ${pl} -am \
                                -Dsonar.projectKey=nashtech-garage_yas-yas-parent \
                                -Dsonar.organization=nashtech-garage
                        """
                    }
                }
            }
        }

        stage('SonarQube Quality Gate') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Snyk Security Scan') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                    script {
                        sh 'snyk auth ${SNYK_TOKEN}'
                        def mods = env.IMPACTED_MODULES.split(',') as List
                        mods.each { m ->
                            sh """
                                snyk test --file=${m}/pom.xml \
                                    --severity-threshold=high \
                                    --json > snyk-${m}-report.json || true
                            """
                        }
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: 'snyk-*-report.json', allowEmptyArchive: true
                }
            }
        }
    }

    post {
        success { echo 'Monorepo CI Pipeline completed successfully!' }
        failure { echo 'Monorepo CI Pipeline failed!' }
    }
}
