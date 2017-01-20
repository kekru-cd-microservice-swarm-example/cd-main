package steps;

import java.io.*

class CDMain extends AbstractPipelineScript implements Serializable {

    public static final String DOCKER_STACK_FILE = 'base-setup.stack.yml'
    public static final String SETUP_DOCKERCLIENT_FILE = 'setup-dockerclient'
    public static final String SETUP_DOCKERCLIENT_PROD_FILE = 'setup-dockerclient-prod'
    public static final String SETUP_REDISCLIENT_FILE = 'setup-redisclient'
    public static final String STACK_DEPLOY_SCRIPT = 'deploy-stack'


    CDMain(steps) {
        super(steps)
    }

    @Override
    protected void initObject() {
        steps.sh 'mkdir --parents cd-main'

        //Docker Client herunterladen und für Testumgebung einrichten
        steps.sh(copyResource(SETUP_DOCKERCLIENT_FILE, true))

        //Docker Client für Produktiv-Umgebung einrichten
        steps.sh(copyResource(SETUP_DOCKERCLIENT_PROD_FILE, true))

        //Redis Client herunterladen: Bashclient "redi.sh"
        steps.sh(copyResource(SETUP_REDISCLIENT_FILE, true))

        //Docker-Compose File in Workspace kopieren, um daraus einen Docker Stack generieren zu können, zum Aufsetzen einer Testumgebung
        copyResource(DOCKER_STACK_FILE)

        //Bash Script, dass Service-Versionen im Stack-File ersetzt und damit den Stack startet
        copyResource(STACK_DEPLOY_SCRIPT, true)
    }


    public DockerStack startTestenvironment() {
        return startTestenvironment('')
    }

    public DockerStack startTestenvironment(String nameExtension) {
        return new DockerStack(steps, nameExtension).init().deployStack()
    }


    public String buildAndPush(String serviceName, String pathDockerfile = '.') {
        def fullImageName = 'manager1:5000/cd/'+serviceName+':'+commitId

        steps.echo 'Build and Push ' + fullImageName
        steps.sh './docker build -t ' + fullImageName + ' ' + pathDockerfile
        steps.sh './docker push ' + fullImageName

        return fullImageName
    }

    def extractImageTag(String imageName){
        if(imageName.contains('/')){
            imageName = imageName.substring(imageName.lastIndexOf('/'))
        }

        if(!imageName.contains(':')){
            return "latest"
        }

        return imageName.substring(imageName.lastIndexOf(':') +1)
    }

    def waitForTCP(int port){
        steps.sh './docker run --rm wait 10.1.6.210 '+port
    }

    def deployInProduction(String fullServiceName, String fullImageName) {
        //TODO Deploy in Production

        def simpleServiceName = fullServiceName
        if(simpleServiceName.contains("_")){
            simpleServiceName = simpleServiceName.substring(0, simpleServiceName.indexOf("_"))
        }

        def imageTag = extractImageTag(fullImageName)
        //Versionsbezeichner (= ImageTag) in Redis speichern
        steps.sh 'echo "' + imageTag + '" | ./redi.sh -s ' + simpleServiceName + '-version'
    }


    def dockerProd(String args) {
        steps.withCredentials(
                [steps.file(credentialsId: 'prod-client-key', variable: 'CLIENTKEY'),
                 steps.file(credentialsId: 'client-cert', variable: 'CLIENTCERT'),
                 steps.file(credentialsId: 'ca-cert', variable: 'CACERT')]) {

            steps.sh './cd-main/docker-client/docker/docker' +
                    ' --tlsverify' +
                    ' -H=prodmanager1:2376' +
                    ' --tlscacert=$CACERT' +
                    ' --tlscert=$CLIENTCERT' +
                    ' --tlskey=$CLIENTKEY' +
                    args

        }
    }


    def docker(args) {
        steps.sh "./docker ${args}"
    }

    /*public static void main(String... args) {
        def main = new CDMain(null)
        main.commitId = '24742df'
        println main.getPublishedPort('newspage', 8081)
    }*/

}


