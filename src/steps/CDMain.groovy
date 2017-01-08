package steps;

import java.io.*
import groovy.json.JsonSlurper;

class CDMain implements Serializable {

    static final String DOCKER_STACK_FILE = 'base-setup.stack.yml'
    static final String SETUP_DOCKERCLIENT_FILE = 'setup-dockerclient'
    static final String PRINT_PORTMAPPINGS_FILE = 'print-service-portmappings'

    def steps
    def commitId

    CDMain(steps) {
        this.steps = steps
    }

    public void init() {
        //in init ausgelagert, wegen Bug https://issues.jenkins-ci.org/browse/JENKINS-26313

        steps.sh 'mkdir --parents cd-main'

        //Docker Client herunterladen
        steps.sh(copyResource(SETUP_DOCKERCLIENT_FILE, true))

        //Docker-Compose File in Workspace kopieren, um daraus einen Docker Stack generieren zu können, zum Aufsetzen einer Testumgebung
        copyResource(DOCKER_STACK_FILE)

        copyResource(PRINT_PORTMAPPINGS_FILE, true)


        commitId = shResult('git rev-parse --short HEAD')
    }

    private String copyResource(filename) {
        copyResource(filename, false)
    }

    private String copyResource(filename, executable) {
        def fileContent = steps.libraryResource filename
        steps.writeFile file: 'cd-main/' + filename, text: fileContent

        if (executable) {
            steps.sh 'chmod +x cd-main/' + filename
        }

        return getFilePath(filename)
    }

    private String getFilePath(filename) {
        return './cd-main/' + filename
    }

    def stackName() {
        'cd' + commitId
    }

    def fullServiceName(serviceName) {
        if (serviceName.startsWith(stackName() + '_')) {
            return serviceName
        }

        return stackName() + '_' + serviceName
    }

    def startTestenvironment() {
        steps.sh './docker stack deploy --compose-file ' + getFilePath(DOCKER_STACK_FILE) + ' ' + stackName()
    }

    private String shResult(String shellCommand){
        return steps.sh (script: shellCommand, returnStdout:true).trim()
    }

    def getPublishedPort(String serviceName, int targetPort) {

        def portMappingsJSON = shResult(getFilePath(PRINT_PORTMAPPINGS_FILE) + ' ' + stackName())
        //steps.echo 'Portmappings: ' + portMappingsJSON
        /*
        Beispiel für portMappingsJSON:
        [{
            "name": "cd91ff259_redis",
            "portmappings": null
        }, {
            "name": "cd91ff259_newspage",
            "portmappings": [{
                    "Protocol": "tcp",
                    "TargetPort": 8081,
                    "PublishedPort": 30001,
                    "PublishMode": "ingress"
                }
            ]
        }, {
            "name": "cd91ff259_newspage-mongo",
            "portmappings": null
        }, {
            "name": "cd91ff259_webdis",
            "portmappings": [{
                    "Protocol": "tcp",
                    "TargetPort": 7379,
                    "PublishedPort": 30000,
                    "PublishMode": "ingress"
                }
            ]
        }]

        */


        def mappingList = new JsonSlurper().parseText(portMappingsJSON)
        for (def mappingInfo : mappingList) {

            if (String.valueOf(mappingInfo.name).equals(String.valueOf(fullServiceName(serviceName)))) {
                for (def mapping : mappingInfo.portmappings) {

                    if (String.valueOf(mapping.TargetPort).equals(String.valueOf(targetPort))) {
                        return mapping.PublishedPort
                    }
                }
            }
        }

        return -1
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


