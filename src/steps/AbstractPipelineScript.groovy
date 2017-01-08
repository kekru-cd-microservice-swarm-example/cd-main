package steps

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

        steps.sh 'mkdir --parents cd-main'

        commitId = shResult('git rev-parse --short HEAD')

        initObject()

        return this
    }
}
