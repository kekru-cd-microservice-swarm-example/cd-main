package steps
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
	
	public void init(){
		//in init ausgelagert, wegen Bug https://issues.jenkins-ci.org/browse/JENKINS-26313
	
		steps.sh 'mkdir --parents cd-main'
		
		//Docker Client herunterladen	
		steps.sh(copyResource(SETUP_DOCKERCLIENT_FILE, true))
		
		//Docker-Compose File in Workspace kopieren, um daraus einen Docker Stack generieren zu können, zum Aufsetzen einer Testumgebung
		copyResource(DOCKER_STACK_FILE)
		
		copyResource(PRINT_PORTMAPPINGS_FILE, true)
		
		steps.sh 'git rev-parse --short HEAD > .git/commit-id'
		commitId = steps.readFile('.git/commit-id')
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
	
	def stackName(){
		'cd' + commitId
	}
	
	def fullServiceName(serviceName){
		if(serviceName.startsWith(stackName() + '_')){
			return serviceName
		}
		
		return stackName() + '_' + serviceName
	}
  
	def startTestenvironment(){
		steps.sh './docker stack deploy --compose-file ' + getFilePath(DOCKER_STACK_FILE) + ' ' + stackName()
	}
			
	def getPublishedPort(serviceName, targetPort){		
		
		def portMappingsJSON = steps.sh (script: getFilePath(PRINT_PORTMAPPINGS_FILE) + ' ' + stackName(), returnStdout:true).trim()
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
		
		def publishedPort = -1
		def mappingList = new JsonSlurper().parseText(portMappingsJSON)
		steps.echo 'PortMappingsJSON: ' + portMappingsJSON
		mappingList.each { mappingInfo ->
			steps.echo mappingInfo
			if(mappingInfo.name.equals(fullServiceName(serviceName))){
				steps.echo serviceName
				mappingInfo.portmappings.each { mapping ->
					steps.echo mapping
					if(mapping.TargetPort.equals(targetPort)){
						steps.echo mapping.PublishedPort
						publishedPort = mapping.PublishedPort
					}
				}
				
			}
		}
		
		return publishedPort
	}

	def docker(args) {
		steps.sh "./docker ${args}"
	}

}
