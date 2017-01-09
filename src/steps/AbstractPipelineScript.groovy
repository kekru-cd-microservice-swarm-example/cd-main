package steps

import groovy.json.JsonSlurperClassic

/**
 * Created by krummenauer on 08.01.2017.
 */
abstract class AbstractPipelineScript {

    def steps
    def commitId

    AbstractPipelineScript(steps){
        this.steps = steps
    }

    protected String copyResource(filename) {
        copyResource(filename, false)
    }

    protected String copyResource(filename, executable) {
        def fileContent = steps.libraryResource filename
        steps.writeFile file: 'cd-main/' + filename, text: fileContent

        if (executable) {
            steps.sh 'chmod +x cd-main/' + filename
        }

        return getFilePath(filename)
    }

    protected String getFilePath(filename) {
        return './cd-main/' + filename
    }

    protected String shResult(String shellCommand){
        return steps.sh (script: shellCommand, returnStdout:true).trim()
    }

    protected abstract void initObject()

    public final AbstractPipelineScript init() {
        //in init ausgelagert, wegen Bug https://issues.jenkins-ci.org/browse/JENKINS-26313

        commitId = shResult('git rev-parse --short HEAD')

        initObject()

        return this
    }

    public final String getPublishedPortOfService(String fullServiceName, int targetPort){
        return getPublishedPortOfService(fullServiceName, fullServiceName, targetPort);
    }

    public final String getPublishedPortOfService(String bashCommandCreatingServiceNames, String fullServiceName, int targetPort){
        String portMappingJSON = shResult('./docker service inspect --format=\'{"name": {{json .Spec.Name}}, "portmappings": {{json .Endpoint.Ports}}},\' '+bashCommandCreatingServiceNames)
        if(portMappingJSON.endsWith(",")){
            portMappingJSON = portMappingJSON.substring(0, portMappingJSON.length() -1)
        }
        portMappingJSON = "[" + portMappingJSON + "]";
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

        def mappingList = new JsonSlurperClassic().parseText(portMappingJSON)
        for (def mappingInfo : mappingList) {

            if (String.valueOf(mappingInfo.name).equals(String.valueOf(fullServiceName))) {
                for (def mapping : mappingInfo.portmappings) {

                    if (String.valueOf(mapping.TargetPort).equals(String.valueOf(targetPort))) {
                        return mapping.PublishedPort
                    }
                }
            }
        }

        return -1
    }

}
