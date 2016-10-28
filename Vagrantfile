# -*- mode: ruby -*-
# vi: set ft=ruby :

def require_plugin(plugin_name)
  unless Vagrant.has_plugin?(plugin_name)
    puts "ERROR: Missing plugin '#{plugin_name}'"
    puts "Please run: vagrant plugin install #{plugin_name}"
    exit(1)
  end
end

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  require_plugin("vagrant-vbguest")
  require_plugin("vagrant-docker-compose")

  config.vm.box = "bento/centos-7.2"

  if Vagrant.has_plugin?("vagrant-proxyconf")
    require_plugin("vagrant-env")
    config.env.enable

    config.proxy.http     = ENV['HTTP_PROXY']
    config.proxy.https    = ENV['HTTPS_PROXY']
    config.proxy.ftp      = ENV['FTP_PROXY']
    config.proxy.no_proxy = ENV['NO_PROXY']
  end

  config.vm.provider "virtualbox" do |vb|
    # Customize the amount of memory on the VM:
    vb.memory = "4096"
    vb.cpus = 2
  end

  config.vm.hostname = "quckoo-vagrant"
  config.vm.network "private_network", ip: "192.168.50.25"

  local_ivy_cache = File.expand_path("~/.ivy2")
  if File.exist?(local_ivy_cache)
    config.vm.synced_folder local_ivy_cache, "/home/vagrant/.ivy2",
                            id: "ivy-cache",
                            mount_options: ["dmode=777,fmode=777"]
  end

  # Setting up docker provisioner in a separate line to allow for the proxy configuration
  config.vm.provision :docker

  # Installs minimal software tools and prepares the VM for running the application
  config.vm.provision :shell, path: "sandbox/vagrant/provision.sh"

  # Sets up the third-party services needed to run the application
  config.vm.provision :docker_compose, yml: "/vagrant/sandbox/vagrant/docker-support.yml"

  # Builds the latest version of the application code
  config.vm.provision :shell, inline: "/vagrant/sandbox/vagrant/build.sh", privileged: false

  # Starts the application cluster
  config.vm.provision :docker_compose, yml: "/vagrant/sandbox/vagrant/docker-quckoo.yml"

end
