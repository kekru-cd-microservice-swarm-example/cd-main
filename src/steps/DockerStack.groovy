package steps

import groovy.json.JsonSlurperClassic

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
        return getPublishedPortOfService(fullServiceName(serviceName), targetPort)
    }

    public DockerStack deployStack(){
        stackName = 'cd' + commitId + nameExtension
        def deployScript =  getFilePath(CDMain.STACK_DEPLOY_SCRIPT)
        def stackFile = getFilePath(CDMain.DOCKER_STACK_FILE)
        steps.sh deployScript + ' ' + stackFile + ' ' + stackName
        return this
    }

    def getBorderproxyPort() {
        return getPublishedPort('traefik', 80)
    }

    def fullServiceName(serviceName) {
        if (serviceName.startsWith(stackName + '_')) {
            return serviceName
        }

        return stackName + '_' + serviceName
    }

    def getNetworkName(){
        return stackName + '_default'
    }

    def removeStack(){
        steps.sh './docker stack rm ' + stackName
    }

}
