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

        stage('Detect changed modules') {
            steps {
                script {
                    // Always fetch all remote branches so origin/main exists locally
                    sh "git fetch --no-tags --prune origin +refs/heads/*:refs/remotes/origin/*"

                    // 1) Choose base for diff
                    // - PR build: compare with PR target branch (usually main)
                    // - Branch build: compare with origin/main
                    def baseRef = ''
                    if (env.CHANGE_ID) {
                        baseRef = "origin/${env.CHANGE_TARGET ?: env.DEFAULT_BASE_BRANCH}"
                    } else {
                        baseRef = "origin/${env.DEFAULT_BASE_BRANCH}"
                    }

                    // 2) Diff and collect changed files
                    def diffCmd = "git diff --name-only ${baseRef}...HEAD"
                    def changedFilesRaw = sh(script: diffCmd, returnStdout: true).trim()
                    def changedFiles = changedFilesRaw ? changedFilesRaw.split('\n') : []

                    echo "BRANCH_NAME: ${env.BRANCH_NAME}"
                    echo "Base for diff: ${baseRef}"
                    echo "origin/${env.DEFAULT_BASE_BRANCH}: " + sh(script: "git rev-parse ${baseRef}", returnStdout: true).trim()
                    echo "HEAD: " + sh(script: "git rev-parse HEAD", returnStdout: true).trim()
                    echo "Changed files:\n- " + (changedFiles ? changedFiles.join("\n- ") : "(none)")

                    // 3) Declare Maven modules (folder names)
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

                    // 4) Decide impacted modules (Option A: ONLY folder-based changes)
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
                script {
                    def mods = env.IMPACTED_MODULES.split(',') as List
                    def pl = mods.join(',')
                    sh "mvn -B clean install -pl ${pl} -am -DskipTests"
                }
            }
        }

        stage('Test impacted modules') {
            when { expression { return env.IMPACTED_MODULES?.trim() } }
            steps {
                script {
                    def mods = env.IMPACTED_MODULES.split(',') as List
                    def pl = mods.join(',')
                    sh "mvn -B test jacoco:report -pl ${pl} -am"
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
                                exclusionPattern: '**/*Test*.class'
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
    }

    post {
        success { echo 'Monorepo CI Pipeline completed successfully!' }
        failure { echo 'Monorepo CI Pipeline failed!' }
    }
}