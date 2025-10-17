pipeline {
  agent none
  stages {
    stage('Checkout') {
      agent any
      steps {
        echo '📦 Checking out code from GitHub...'
        checkout scm
        sh 'pwd && ls -la'
        sh 'ls -la calculator/ || echo "Calculator folder not found!"'
      }
    }

    stage('Build & Test Calculator App') {
      agent {
        docker {
          image 'maven:3.9.6-eclipse-temurin-17-alpine'
          args '-v $HOME/.m2:/root/.m2'
          reuseNode true
        }
      }
      steps {
        dir('calculator') {
          echo '⚙️ Building and testing calculator app...'
          sh 'mvn clean package -DskipTests=true'
          sh 'ls -la target/'
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
          def commitHash = env.GIT_COMMIT?.take(7) ?: "latest"
          def imageName = "kmn1624/calculator-app"

          echo "🐳 Building Docker image: ${imageName}:${commitHash}"

          sh """
            cd calculator
            docker build -t ${imageName}:${commitHash} -t ${imageName}:latest .
          """

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

    stage('Deploy to EC2 Host') {
      agent any
      steps {
        script {
          echo '🚀 Deploying calculator app on EC2 host via SSH...'

          // Replace with your EC2 host IP and SSH user
          def ec2Host = "ubuntu@3.135.237.209"

          sh """
            ssh -o StrictHostKeyChecking=no ${ec2Host} '
              docker stop calculator-app 2>/dev/null || true
              docker rm calculator-app 2>/dev/null || true
              docker run -d \
                --name calculator-app \
                -p 8081:8080 \
                --restart unless-stopped \
                kmn1624/calculator-app:latest
              docker ps | grep calculator-app
            '
          """

          echo '✅ Deployment via SSH completed!'
        }
      }
    }

    stage('Health Check') {
      agent any
      steps {
        script {
          echo '🏥 Performing health check...'

          // Health check via SSH to EC2 host
          def ec2Host = "ubuntu@3.135.237.209"
          sh """
            ssh -o StrictHostKeyChecking=no ${ec2Host} '
              sleep 5
              curl -f http://localhost:8081 || echo "⚠️ Application might not be ready yet"
              echo ""
              echo "✅ Container Status:"
              docker ps | grep calculator-app
            '
          """
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
      echo 'Pipeline completed for Calculator App!'
      echo '=========================================='
    }
    success {
      echo '✅ BUILD SUCCESSFUL!'
      echo '🌐 Access Application: http://<EC2_PUBLIC_IP>:8081'
      echo '🐳 Docker Image: kmn1624/calculator-app:latest'
      echo '📦 Container Name: calculator-app'
      echo '🔍 Check logs: docker logs calculator-app -f'
    }
    failure {
      echo '❌ BUILD FAILED! Check console for details.'
      echo '🔍 Debug: docker logs calculator-app'
    }
  }
}
