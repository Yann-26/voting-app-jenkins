pipeline {
    agent any

    environment {
        // === Maven settings ===
        MAVEN_HOME = "/usr/share/maven"
        PATH = "$MAVEN_HOME/bin:$PATH"

        // === Slack settings ===
        SLACK_CHANNEL = '#slackup_simplilearn_notifications'
        SLACK_CREDENTIALS_ID = 'demo-channel'

        // === Tomcat Manager Text URL ===
        TOMCAT_DEPLOY_URL = 'http://localhost:9090/manager/text'
    }

    options {
        timestamps()
    }

    stages {

        stage('Notify Start') {
            steps {
                echo '📢 Build started...'
                slackSend(
                    channel: "${SLACK_CHANNEL}",
                    color: '#439FE0',
                    message: "🚀 *Build Started* for `${env.JOB_NAME}` (#${env.BUILD_NUMBER})\n${env.BUILD_URL}"
                )
            }
        }

        stage('Checkout Source') {
            steps {
                echo '📦 Checking out source code...'
                checkout scm
            }
        }

        stage('Build with Maven') {
            steps {
                echo '⚙️ Running Maven build...'
                sh '''
                    set -e

                    if [ -f "./mvnw" ]; then
                        echo "Using Maven Wrapper..."
                        chmod +x mvnw
                        ./mvnw clean package -DskipTests
                    else
                        echo "Using System Maven..."
                        mvn clean package -DskipTests
                    fi

                    echo "Listing target/ directory:"
                    ls -al target
                '''
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                echo '🚀 Deploying application to Tomcat...'

                withCredentials([
                    usernamePassword(credentialsId: '39eead8c-6c36-422b-810d-5758be33fce2',
                                     usernameVariable: 'TOMCAT_USER',
                                     passwordVariable: 'TOMCAT_PASS')
                ]) {

                    sh '''
                        set -eu

                        echo "🔍 Searching artifact..."
                        ARTIFACT=$(find target -maxdepth 1 -type f -name "*.war" | head -n 1 || true)

                        if [ -z "$ARTIFACT" ]; then
                          echo "❌ ERROR: No WAR file found in target/"
                          exit 1
                        fi

                        echo "✅ Found artifact: $ARTIFACT"

                        echo "♻️ Removing old deployment..."
                        curl --fail -u "$TOMCAT_USER:$TOMCAT_PASS" \
                             "${TOMCAT_DEPLOY_URL}/undeploy?path=/myapp" || true

                        echo "🚀 Deploying new version..."
                        curl --fail -u "$TOMCAT_USER:$TOMCAT_PASS" \
                             -T "$ARTIFACT" \
                             "${TOMCAT_DEPLOY_URL}/deploy?path=/myapp&update=true"

                        echo "✅ Deployment successful!"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ Build and deployment successful!'
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'good',
                message: "✅ *SUCCESS* — `${env.JOB_NAME}` #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }

        failure {
            echo '❌ Build or deployment failed!'
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'danger',
                message: "❌ *FAILURE* — `${env.JOB_NAME}` #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }

        aborted {
            echo '🛑 Build aborted!'
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: '#AAAAAA',
                message: "🛑 *ABORTED* — `${env.JOB_NAME}` #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }

        unstable {
            echo '⚠️ Build is unstable!'
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: 'warning',
                message: "⚠️ *UNSTABLE BUILD* — `${env.JOB_NAME}` #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
            )
        }

        always {
            echo '📣 Build completed.'
            slackSend(
                channel: "${SLACK_CHANNEL}",
                color: '#AAAAAA',
                message: "📣 *Build Completed* — `${env.JOB_NAME}` #${env.BUILD_NUMBER}\nStatus: ${currentBuild.currentResult}\n${env.BUILD_URL}"
            )
        }
    }
}
