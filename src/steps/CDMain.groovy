package steps

class CDMain implements Serializable {

	static final String DOCKER_STACK_FILE = 'base-setup.stack.yml'
	static final String SETUP_DOCKERCLIENT_FILE = 'setup-dockerclient'
	def steps
  
	CDMain(steps) {
		this.steps = steps	
	}
	
	public void init(){
		//in init ausgelagert, wegen Bug https://issues.jenkins-ci.org/browse/JENKINS-26313
	
		steps.sh 'mkdir --parents cd-main'
		
		//Docker Client herunterladen	
		steps.sh(copyResource(SETUP_DOCKERCLIENT_FILE, true))
		
		//Docker-Compose File in Workspace kopieren, um daraus einen Docker Stack generieren zu können, zum Aufsetzen einer Testumgebung
		copyResource(DOCKER_STACK_FILE)
	}
	
	private String copyResource(filename){
		copyResource(filename, false)
	}
	
	private String copyResource(filename, executable){
		def fileContent = steps.libraryResource filename
		steps.writeFile file: 'cd-main/'+filename, text: fileContent
		
		if(executable){
			steps.sh 'chmod +x cd-main/'+filename
		}
		
		return getFilePath(filename)
	}
	
	private String getFilePath(filename){
		return './cd-main/'+filename
	}
  
	def startTestenvironment(){
		steps.sh './docker stack deploy --compose-file '+getFilePath(DOCKER_STACK_FILE)+' test1'
	}

	def docker(args) {
		steps.sh "./docker ${args}"
	}

}