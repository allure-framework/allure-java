pipeline {
    agent { label 'java' }
    parameters {
        booleanParam(name: 'RELEASE', defaultValue: false, description: 'Perform release?')
        string(name: 'RELEASE_VERSION', defaultValue: '', description: 'Release version')
        string(name: 'NEXT_VERSION', defaultValue: '', description: 'Next version (without SNAPSHOT)')
    }
    stages {
        stage('Build') {
            steps {
                sh './gradlew build'
            }
        }
        stage('Reports') {
            steps {
                checkstyle pattern: '**/build/reports/checkstyle/main.xml', defaultEncoding: 'UTF8',
                        canComputeNew: false, healthy: '', unHealthy: ''
                findbugs pattern: '**/build/reports/findbugs/main.xml', defaultEncoding: 'UTF8',
                        canComputeNew: false, healthy: '', unHealthy: '', excludePattern: '', includePattern: ''
                pmd pattern: '**/build/reports/pmd/main.xml', defaultEncoding: 'UTF8',
                        canComputeNew: false, healthy: '', unHealthy: ''
            }
        }
        stage('Release') {
            when { expression { return params.RELEASE } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'qameta-ci_bintray',
                        usernameVariable: 'BINTRAY_USER', passwordVariable: 'BINTRAY_API_KEY')]) {
                    sshagent(['qameta-ci_ssh']) {
                        sh 'git checkout master && git pull origin master'
                        sh "./gradlew release -Prelease.useAutomaticVersion=true " +
                                "-Prelease.releaseVersion=${RELEASE_VERSION} " +
                                "-Prelease.newVersion=${NEXT_VERSION}-SNAPSHOT"
                    }
                }
            }
        }
    }
    post {
        always {
            deleteDir()
        }

        failure {
            allure results: [[path: '**/build/allure-results']]
            slackSend message: "${env.JOB_NAME} - #${env.BUILD_NUMBER} failed (<${env.BUILD_URL}|Open>)",
                    color: 'danger', teamDomain: 'qameta', channel: 'allure', tokenCredentialId: 'allure-channel'
        }
    }
}