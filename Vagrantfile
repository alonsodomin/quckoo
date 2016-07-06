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

  config.vm.provider "virtualbox" do |vb|
    # Customize the amount of memory on the VM:
    vb.memory = "4096"
    vb.cpus = 2
  end

  # fix "stdin: is not a tty" error
  config.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"

  config.vm.hostname = "quckoo-vagrant"
  config.vm.network "private_network", ip: "192.168.50.25"
  config.vm.synced_folder File.expand_path("~/.ivy2"), "/home/vagrant/.ivy2",
    id: "ivy-cache",
    mount_options: ["dmode=777,fmode=777"]

  # Setting up docker provisioner in a separate line to allow for the proxy configuration
  config.vm.provision :docker

  config.vm.provision :shell, path: "boot/provision.sh"

  config.vm.provision :shell, inline: "/vagrant/boot/build.sh", privileged: false
  config.vm.provision :docker_compose, yml: "/vagrant/boot/docker-compose.yml"

end
