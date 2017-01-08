package steps;

import java.io.*

class CDMain extends AbstractPipelineScript implements Serializable {

    public static final String DOCKER_STACK_FILE = 'base-setup.stack.yml'
    public static final String SETUP_DOCKERCLIENT_FILE = 'setup-dockerclient'
    public static final String PRINT_PORTMAPPINGS_FILE = 'print-service-portmappings'


    CDMain(steps) {
        super(steps)
    }

    @Override
    protected void initObject() {
        //Docker Client herunterladen
        steps.sh(copyResource(SETUP_DOCKERCLIENT_FILE, true))

        //Docker-Compose File in Workspace kopieren, um daraus einen Docker Stack generieren zu können, zum Aufsetzen einer Testumgebung
        copyResource(DOCKER_STACK_FILE)

        copyResource(PRINT_PORTMAPPINGS_FILE, true)
    }


    public DockerStack startTestenvironment() {
        return startTestenvironment('')
    }

    public DockerStack startTestenvironment(String nameExtension) {
        return new DockerStack(steps, nameExtension).init().deployStack()
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


