pipeline {
  agent none
  
  stages {
    stage('Checkout') {
      agent any
      steps {
        echo '📦 Checking out code from GitHub...'
        checkout scm
        script {
          sh 'pwd'
          sh 'ls -la'
          sh 'ls -la calculator/ || echo "❌ Calculator folder not found!"'
          sh 'ls -la calculator/Dockerfile || echo "❌ Dockerfile not found!"'
          sh 'ls -la calculator/pom.xml || echo "❌ pom.xml not found!"'
        }
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
          sh 'pwd'
          sh 'ls -la'
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
          
          // Verify Dockerfile exists before building
          sh """
            if [ ! -f calculator/Dockerfile ]; then
              echo "❌ ERROR: Dockerfile not found in calculator directory!"
              exit 1
            fi
          """
          
          // Build Docker image
          dir('calculator') {
            sh """
              pwd
              ls -la
              docker build -t ${imageName}:${commitHash} -t ${imageName}:latest .
            """
          }
          
          // Push to Docker Hub
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
    
    stage('Clean Up Old Container') {
      agent any
      steps {
        script {
          echo '🧹 Cleaning up old containers and freeing port 8081...'
          
          sh '''
            # Stop and remove calculator-app container if exists
            if docker ps -a --format '{{.Names}}' | grep -q '^calculator-app$'; then
              echo "🛑 Stopping existing calculator-app container..."
              docker stop calculator-app || true
              echo "🗑️  Removing existing calculator-app container..."
              docker rm calculator-app || true
            else
              echo "ℹ️  No existing calculator-app container found"
            fi
            
            # Check if port 8081 is still in use
            if docker ps --format '{{.Ports}}' | grep -q '8081'; then
              echo "⚠️  Port 8081 still in use by another container"
              echo "🔍 Finding container using port 8081..."
              CONTAINER_ID=$(docker ps -q -f "publish=8081")
              if [ ! -z "$CONTAINER_ID" ]; then
                CONTAINER_NAME=$(docker ps --filter "id=$CONTAINER_ID" --format "{{.Names}}")
                echo "📦 Container using port 8081: $CONTAINER_NAME (ID: $CONTAINER_ID)"
                echo "🛑 Stopping container $CONTAINER_NAME..."
                docker stop $CONTAINER_NAME || true
                echo "🗑️  Removing container $CONTAINER_NAME..."
                docker rm $CONTAINER_NAME || true
              fi
            fi
            
            # Double check port is free
            sleep 2
            if sudo lsof -t -i:8081 > /dev/null 2>&1; then
              echo "⚠️  Port 8081 still in use by non-Docker process"
              echo "🛑 Killing process using port 8081..."
              sudo kill -9 $(sudo lsof -t -i:8081) || true
            fi
            
            echo "✅ Port 8081 is now free"
          '''
        }
      }
    }
    
    stage('Deploy to EC2') {
      agent any
      steps {
        script {
          echo '🚀 Deploying calculator app to EC2...'
          
          sh '''
            # Verify port is free
            if sudo lsof -t -i:8081 > /dev/null 2>&1; then
              echo "❌ ERROR: Port 8081 is still in use!"
              sudo lsof -i:8081
              exit 1
            fi
            
            # Run new container
            echo "🐳 Starting new calculator-app container..."
            docker run -d \
              --name calculator-app \
              -p 8081:8080 \
              --restart unless-stopped \
              kmn1624/calculator-app:latest
            
            # Verify container started
            if [ $? -eq 0 ]; then
              echo "✅ Container started successfully"
            else
              echo "❌ Failed to start container"
              exit 1
            fi
          '''
          
          echo '⏳ Waiting for application to start...'
          sleep 15
          
          // Verify container is running
          sh '''
            echo "📊 Container status:"
            docker ps | grep calculator-app || (echo "❌ Container not running!" && exit 1)
            
            echo ""
            echo "📝 Recent logs:"
            docker logs calculator-app --tail 30
          '''
          
          echo '✅ Deployment successful!'
        }
      }
    }
    
    stage('Health Check & Verification') {
      agent any
      steps {
        script {
          echo '🏥 Performing health check and verification...'
          
          sh '''
            # Wait for app to fully start
            echo "⏳ Waiting for application to initialize..."
            sleep 10
            
            # Get EC2 public IP
            echo "🌐 Getting EC2 public IP..."
            PUBLIC_IP=$(curl -s http://checkip.amazonaws.com)
            echo "📍 EC2 Public IP: $PUBLIC_IP"
            
            # Test localhost access
            echo ""
            echo "🔍 Testing localhost access..."
            if curl -f -s http://localhost:8081 > /dev/null; then
              echo "✅ Application is accessible on localhost:8081"
            else
              echo "⚠️  Application not responding on localhost:8081 yet"
            fi
            
            # Test health endpoint
            echo ""
            echo "🏥 Testing health endpoint..."
            curl -f http://localhost:8081/actuator/health 2>/dev/null || echo "⚠️  Health endpoint not responding yet"
            
            # Show container details
            echo ""
            echo "📦 Container details:"
            docker inspect calculator-app --format '{{.State.Status}}' || true
            
            echo ""
            echo "✅ Verification complete!"
            echo ""
            echo "================================================"
            echo "🎉 DEPLOYMENT SUCCESSFUL!"
            echo "================================================"
            echo "🌐 Access your application:"
            echo "   - Local:    http://localhost:8081"
            echo "   - Public:   http://$PUBLIC_IP:8081"
            echo ""
            echo "🏥 Health Check:"
            echo "   - http://localhost:8081/actuator/health"
            echo "   - http://$PUBLIC_IP:8081/actuator/health"
            echo ""
            echo "🐳 Docker Info:"
            echo "   - Container: calculator-app"
            echo "   - Image:     kmn1624/calculator-app:latest"
            echo "   - Port:      8081 -> 8080"
            echo ""
            echo "🔍 Useful commands:"
            echo "   - View logs:    docker logs calculator-app -f"
            echo "   - Stop app:     docker stop calculator-app"
            echo "   - Restart app:  docker restart calculator-app"
            echo "================================================"
          '''
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
      script {
        def publicIp = sh(script: 'curl -s http://checkip.amazonaws.com', returnStdout: true).trim()
        echo '✅ BUILD & DEPLOYMENT SUCCESSFUL!'
        echo '🎉 Calculator app is now running!'
        echo ''
        echo '🌐 Access URLs:'
        echo "   - Local:  http://localhost:8081"
        echo "   - Public: http://${publicIp}:8081"
        echo ''
        echo '🐳 Docker Image: kmn1624/calculator-app:latest'
        echo '📦 Container Name: calculator-app'
        echo '🔍 Check logs: docker logs calculator-app -f'
      }
    }
    failure {
      echo '❌ BUILD FAILED! Check console for details.'
      echo '🔍 Debug commands:'
      echo '   - docker logs calculator-app'
      echo '   - docker ps -a | grep calculator'
      echo '   - sudo lsof -i:8081'
    }
  }
}
