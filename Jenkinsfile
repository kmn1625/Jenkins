pipeline {
  agent none

  stages {

    stage('Checkout') {
      agent any
      steps {
        echo '📦 Checking out code from GitHub...'
        checkout scm
        sh 'pwd && ls -la'
      }
    }

    stage('Build & Test Voting App') {
      agent {
        docker {
          image 'maven:3.9.6-eclipse-temurin-17-alpine'
          args '-v $HOME/.m2:/root/.m2'
          reuseNode true
        }
      }
      steps {
        dir('voting') {
          echo '⚙️ Building and testing app...'
          sh 'mvn clean package -DskipTests=false'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
      }
    }

    stage('Build & Push Docker Image') {
      agent any
      steps {
        script {
          def commitHash = env.GIT_COMMIT?.take(7) ?: "local"
          def imageName = "kmn1624/craftista-voting"

          echo "🐳 Building image: ${imageName}:${commitHash}"

          // Build image
          sh """
            docker build -t ${imageName}:${commitHash} -t ${imageName}:latest -f voting/Dockerfile voting
          """

          // Push image to Docker Hub
          withDockerRegistry([credentialsId: 'dockerlogin', url: 'https://index.docker.io/v1/']) {
            sh """
              docker push ${imageName}:${commitHash}
              docker push ${imageName}:latest
            """
          }

          echo "✅ Docker image pushed: ${imageName}:${commitHash}"
        }
      }
    }

    stage('Deploy to Local Container') {
      agent any
      steps {
        script {
          echo '🚀 Deploying the app container on this instance...'

          sh '''
            docker stop craftista-voting 2>/dev/null || true
            docker rm craftista-voting 2>/dev/null || true

            docker run -d \
              --name craftista-voting \
              -p 8081:8080 \
              --restart unless-stopped \
              kmn1624/craftista-voting:latest
          '''

          echo '🕒 Waiting for container to start...'
          sleep 15
          sh 'docker ps | grep craftista-voting || (echo "❌ Container not running!" && exit 1)'
          echo '✅ Deployment successful! Access the app at: http://<your-public-ip>:8081'
        }
      }
    }
  }

  tools {
    maven 'Maven 3.9.6'
  }

  post {
    always {
      echo '=========================================='
      echo 'Pipeline completed for Craftista Voting!'
      echo '=========================================='
    }
    success {
      echo '✅ BUILD SUCCESSFUL!'
      echo '🌐 Application: http://<your-public-ip>:8081'
      echo '🐳 Image: kmn1624/craftista-voting:latest'
    }
    failure {
      echo '❌ BUILD FAILED! Check console for details.'
    }
  }
}
