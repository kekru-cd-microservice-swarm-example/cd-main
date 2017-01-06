package steps

class AlphaNachrichten implements Serializable {
  def steps
  AlphaNachrichten(steps) {this.steps = steps}
  def docker(args) {
    steps.sh "./docker ${args}"
  }
  
  def printTree(){   

    def fileContent = steps.libraryResource 'github.com/kekru-cd-microservice-swarm-example/cd-main@master/setup-dockerclient'
    steps.writeFile file: 'setup-dockerclient2', text: fileContent
	steps.sh "tree ."
  }
}