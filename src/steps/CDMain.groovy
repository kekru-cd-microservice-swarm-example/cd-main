package steps;

import java.io.*

class CDMain extends AbstractPipelineScript implements Serializable {

    public static final String DOCKER_STACK_FILE = 'base-setup.stack.yml'
    public static final String SETUP_DOCKERCLIENT_FILE = 'setup-dockerclient'
    public static final String SETUP_REDISCLIENT_FILE = 'setup-redisclient'
    public static final String STACK_DEPLOY_SCRIPT = 'deploy-stack'


    CDMain(steps) {
        super(steps)
    }

    @Override
    protected void initObject() {
        steps.sh 'mkdir --parents cd-main'

        //Docker Client herunterladen
        steps.sh(copyResource(SETUP_DOCKERCLIENT_FILE, true))

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

    private String extractImageTag(String imageName){
        if(!imageName.contains(':')){
            return "latest"
        }

        return imageName.substring(imageName.lastIndexOf(':'))
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



    def docker(args) {
        steps.sh "./docker ${args}"
    }

    /*public static void main(String... args) {
        def main = new CDMain(null)
        main.commitId = '24742df'
        println main.getPublishedPort('newspage', 8081)
    }*/

}


