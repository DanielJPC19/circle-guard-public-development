pipeline {
    environment {
        IMAGE_NAME = "circle-guard-ingesoft-v-daniel"
    }
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Building the back-end application...'
                sh './gradlew build -x test'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
            sh 'docker image prune -f' // Eliminar imagenes no utilizadas
        }
        failure {
            echo 'Pipeline failed. Please check the logs for details.'
            sh 'docker compose down || true' // Stop containers without failing
        }
        always {
            echo 'Cleaning up workspace...'
            cleanWs()
            echo 'Pipeline execution finished.'
        }
    }
}