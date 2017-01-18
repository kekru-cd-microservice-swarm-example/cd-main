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
        return getPublishedPortOfService('$(./docker stack services -q '+stackName+')', fullServiceName(serviceName), targetPort)
    }

    public DockerStack deployStack(){
        stackName = 'cd' + commitId + nameExtension
        def stackFile = getFilePath(CDMain.DOCKER_STACK_FILE)
        steps.sh 'sed -i "s|test_default|'+stackName+'_default|g" ' + stackFile
        steps.echo 'Deploy Stack'
        steps.sh 'cat ' + stackFile
        steps.sh './docker stack deploy --compose-file ' + stackFile + ' ' + stackName
        return this
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

}
