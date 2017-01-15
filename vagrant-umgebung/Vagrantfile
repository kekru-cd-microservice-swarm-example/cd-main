Vagrant.configure(2) do |config|
  
  config.vm.box = "ubuntu/trusty64"
  
  config.vm.hostname = "manager1"
  
  #Wait max 10 minutes (600 seconds) to start VM without provisioning
  config.vm.boot_timeout = 600
  
  #config.ssh.password = "vagrant"
  
  #Docker installieren, wenn noch nicht installiert
  config.vm.provision "shell", inline: "command -v docker >/dev/null 2>&1 || curl -fsSL https://test.docker.com/ | sh"
  config.vm.provision "shell", inline: "sudo usermod -aG docker vagrant"
  
  #Registry auf manager1:5000 zulassen
  config.vm.provision "shell", inline: "sudo chmod 666 /etc/default/docker"
  config.vm.provision "shell", inline: "sudo echo 'DOCKER_OPTS=\"--insecure-registry manager1:5000\"' >> /etc/default/docker"
  config.vm.provision "shell", inline: "sudo service docker restart"
  
  #Alte Docker Container stoppen und loeschen
  config.vm.provision "shell", inline: "sh -c 'docker stop $(docker ps -aq) && docker rm $(docker ps -aq); true'"
  #Zeitzone auf Europe/Berlin setzen
  config.vm.provision "shell", inline: "echo 'Europe/Berlin' > /etc/timezone && dpkg-reconfigure -f noninteractive tzdata"
  #Shared-Folder erzeugen, auf den alle VMs Zugriff haben. Hier werden die Docker Swarm Join-Tokens hinterlegt
  config.vm.provision "shell", inline: "mkdir --parents /vagrant/swarmtokens"
  
  #Die VMs erhalten jeweils 2GB RAM
  config.vm.provider :virtualbox do |p|
    p.customize ["modifyvm", :id, "--memory", 2048]
  end
  
  #-------------------------------------------------------------------------------------------
  #-------------------------------------------------------------------------------------------  
  
  #Testumgebung generieren
  config.vm.define "manager1" do |machine|
    #Der Manager bekommt 3GB RAM
	machine.vm.provider :virtualbox do |p|
		p.customize ["modifyvm", :id, "--memory", 3072]
	end
  
	#IP und DNS Name setzen
    machine.vm.hostname = "manager1"
	machine.vm.network "private_network", ip: "10.1.6.210"
	machine.vm.provision :hosts, :sync_hosts => true #DNS für die drei Maschinen. Hierfuer muss vagrant-hosts installiert werden: "vagrant plugin install vagrant-hosts"
	
	#Swarm fuer Testumgebung erstellen
	machine.vm.provision "shell", inline: "sh -c 'docker swarm init --advertise-addr 10.1.6.210; true'"
	#Join-Tokens abspeichern
	machine.vm.provision "shell", inline: "docker swarm join-token -q manager > /vagrant/swarmtokens/test-manager-token"
	machine.vm.provision "shell", inline: "docker swarm join-token -q worker > /vagrant/swarmtokens/test-worker-token"
	
	#Registry starten
	machine.vm.provision "shell", inline: "sudo mkdir --parents /data/registry"
	machine.vm.provision "shell", inline: "docker run --name registry --restart unless-stopped -d -p 5000:5000 -v /data/registry:/var/lib/registry registry"
	
	#Jenkins starten
	machine.vm.provision "shell", inline: "sudo mkdir --parents /data/jenkins"
	machine.vm.provision "shell", inline: "sudo chmod 777 /data/jenkins"
	machine.vm.provision "shell", inline: "docker build -t myjenkins /vagrant/jenkins" 
	machine.vm.provision "shell", inline: "docker run --name jenkins --restart unless-stopped -d -p 8080:8080 -p 8079:50000 -v /data/jenkins:/var/jenkins_home myjenkins"
	
	#Swarm Visualizer starten
	machine.vm.provision "shell", inline: "sudo chmod 666 /var/run/docker.sock"
	machine.vm.provision "shell", inline: "docker run --name swarmvisualizer --restart unless-stopped -d -p 8081:8080 -v /var/run/docker.sock:/var/run/docker.sock:ro manomarks/visualizer"
	
	#Docker Remote API nach aussen verfuegbar machen (ungesichert)
	machine.vm.provision "shell", inline: "docker run --name remoteapi --restart unless-stopped -d -p 2375:2375 -v /var/run/docker.sock:/var/run/docker.sock:ro jarkt/docker-remote-api"
	
	#wait-Image erstellen, wird in den Jenkinsfiles genutzt
	machine.vm.provision "shell", inline: "docker build -t wait /vagrant/waitnetwork" 
  end

  config.vm.define "worker1" do |machine|
    #IP und DNS Name setzen
    machine.vm.hostname = "worker1"
	machine.vm.network "private_network", ip: "10.1.6.211"
	machine.vm.provision :hosts, :sync_hosts => true
	
	#Testumgebungs-Swarm als Worker beitreten. Der Swarm Manager hat den Join Token in einer Datei hinterlegt
	machine.vm.provision "shell", inline: "sh -c 'docker swarm join --token $(cat /vagrant/swarmtokens/test-worker-token) 10.1.6.210:2377; true'"
  end
  
  config.vm.define "worker2" do |machine|
    #IP und DNS Name setzen
    machine.vm.hostname = "worker2"
	machine.vm.network "private_network", ip: "10.1.6.212"
	machine.vm.provision :hosts, :sync_hosts => true
	
	#Testumgebungs-Swarm als Worker beitreten. Der Swarm Manager hat den Join Token in einer Datei hinterlegt
	machine.vm.provision "shell", inline: "sh -c 'docker swarm join --token $(cat /vagrant/swarmtokens/test-worker-token) 10.1.6.210:2377; true'"
  end
  
  
  #-------------------------------------------------------------------------------------------
  #-------------------------------------------------------------------------------------------
  
  #Erstelle Produktivumgebung
  config.vm.define "prodmanager1" do |machine|
    #Der Prod-Manager erhaelt 1GB RAM
    machine.vm.provider :virtualbox do |p|
		p.customize ["modifyvm", :id, "--memory", 1024]
	end
	
	#IP und DNS Name setzen
    machine.vm.hostname = "prodmanager1"
	machine.vm.network "private_network", ip: "10.1.6.213"
	machine.vm.provision :hosts, :sync_hosts => true
	
	#Produktiv Swarm erstellen
	machine.vm.provision "shell", inline: "sh -c 'docker swarm init --advertise-addr 10.1.6.213; true'"
	#Join-Tokens abspeichern
	machine.vm.provision "shell", inline: "docker swarm join-token -q manager > /vagrant/swarmtokens/prod-manager-token"
	machine.vm.provision "shell", inline: "docker swarm join-token -q worker > /vagrant/swarmtokens/prod-worker-token"
	
	#Docker Remote API nach aussen verfuegbar machen (ungesichert)
	machine.vm.provision "shell", inline: "docker run --name remoteapi --restart unless-stopped -d -p 2375:2375 -v /var/run/docker.sock:/var/run/docker.sock:ro jarkt/docker-remote-api"
  end
  
  config.vm.define "prodworker1" do |machine|
    #Der Prod-Worker erhaelt 1GB RAM
	machine.vm.provider :virtualbox do |p|
		p.customize ["modifyvm", :id, "--memory", 1024]
	end
	
	#IP und DNS Name setzen
    machine.vm.hostname = "prodworker1"
	machine.vm.network "private_network", ip: "10.1.6.214"
	machine.vm.provision :hosts, :sync_hosts => true
	
	#Dem Produktiv Swarm als Worker beitreten. Der Swarm Manager hat den Join Token in einer Datei hinterlegt
	machine.vm.provision "shell", inline: "sh -c 'docker swarm join --token $(cat /vagrant/swarmtokens/prod-worker-token) 10.1.6.213:2377; true'"
  end

end