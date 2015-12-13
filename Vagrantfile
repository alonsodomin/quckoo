# -*- mode: ruby -*-
# vi: set ft=ruby :

def require_plugin(plugin_name)
  if !Vagrant.has_plugin?(plugin_name)
    puts "ERROR: Missing plugin '#{plugin_name}'"
    puts "Please run: vagrant plugin install #{plugin_name}"
    exit(1)
  end
end

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "alonsodomin/ubuntu-trusty64-java8"

  if Vagrant.has_plugin?("vagrant-hostmanager")
    config.hostmanager.enabled = true
    config.hostmanager.manage_host = true
    config.hostmanager.ignore_private_ip = false
  end

  if Vagrant.has_plugin?("vagrant-proxyconf")
    require_plugin("vagrant-env")
    config.env.enable

    config.proxy.http     = ENV['HTTP_PROXY']
    config.proxy.https    = ENV['HTTPS_PROXY']
    config.proxy.no_proxy = ENV['NO_PROXY']
  end

  config.vm.provider "virtualbox" do |vb|
    # Customize the amount of memory on the VM:
    vb.memory = "2048"
    vb.cpus = 2
  end

  # fix "stdin: is not a tty" error
  config.ssh.shell = "bash -c 'BASH_ENV=/etc/profile exec bash'"

  config.vm.hostname = "kairos-vagrant"
  config.vm.network "private_network", ip: "192.168.50.25"
  config.vm.synced_folder File.expand_path("~/.ivy2"), "/home/vagrant/.ivy2"

  # Setting up docker provisioner in a separate line to allow for the proxy configuration
  config.vm.provision :docker, version: "latest"

  config.vm.provision :shell, path: "boot/provision.sh"
  config.vm.provision :shell, path: "boot/build.sh", privileged: false

  config.vm.provision :docker do |d|
    d.run "cassandra",
      image: "cassandra:3.0",
      args: "-p 7000:7000 -p 9042:9042 -p 9160:9160 -v /var/lib/cassandra:/var/lib/cassandra"

    d.run "kernel",
      image: "kairos/kernel:0.1.0-SNAPSHOT",
      args: "-p 8095:8095",
      cmd: "--cs 192.168.50.25:9042"
  end

end
