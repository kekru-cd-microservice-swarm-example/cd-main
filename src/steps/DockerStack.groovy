package steps

import groovy.json.JsonSlurper

/**
 * Created by krummenauer on 08.01.2017.
 */
class DockerStack extends AbstractPipelineScript implements Serializable {

    def stackName
    def nameExtension

    DockerStack(steps){
        this(steps, '')
    }

    DockerStack(steps, String nameExtension){
        super(steps)
        this.nameExtension = nameExtension
    }

    @Override
    protected void initObject() {

    }


    def getPublishedPort(String serviceName, int targetPort) {

        def portMappingsJSON = shResult(getFilePath(CDMain.PRINT_PORTMAPPINGS_FILE) + ' ' + stackName)
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

    public DockerStack deployStack(){
        stackName = 'cd' + commitId + nameExtension
        steps.sh './docker stack deploy --compose-file ' + getFilePath(CDMain.DOCKER_STACK_FILE) + ' ' + stackName
        return this
    }


    def fullServiceName(serviceName) {
        if (serviceName.startsWith(stackName + '_')) {
            return serviceName
        }

        return stackName + '_' + serviceName
    }


}
