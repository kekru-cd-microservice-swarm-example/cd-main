node {       
  
   stage('Preparation') { 
      git 'https://github.com/kekru-cd-microservice-swarm-example/jenkins-shared'  

   }
   stage('Build') {
      
      sh 'chmod 777 gradlew'
      sh './gradlew build'     
      
   }
 
}
