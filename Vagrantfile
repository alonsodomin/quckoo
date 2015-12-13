# -*- mode: ruby -*-
# vi: set ft=ruby :

def require_plugin(plugin_name)
  if !Vagrant.has_plugin?(plugin_name)
    puts "ERROR: Missing plugin '#{plugin_name}'"
    puts "Please run: vagrant plugin install #{plugin_name}"
    exit(1)
  end
end

require_plugin("vagrant-env")
require_plugin("vagrant-hostmanager")

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.env.enable

  if Vagrant.has_plugin?("vagrant-hostmanager")
    config.hostmanager.enabled = true
    config.hostmanager.manage_host = true
    config.hostmanager.ignore_private_ip = false
  end

  if Vagrant.has_plugin?("vagrant-proxyconf")
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

  config.vm.define "support" do |support|
    support.vm.box = "phusion/ubuntu-14.04-amd64"
    support.vm.hostname = "kairos-support"
    support.vm.network "private_network", ip: "192.168.50.25"

    # Setting up docker provisioner in a separate line to allow for the proxy configuration
    support.vm.provision :docker, version: "latest"
    support.vm.provision :docker do |d|
      d.run "cassandra",
        image: "cassandra:3.0",
        args: "-p 7000:7000 -p 9042:9042 -p 9160:9160 -v /var/lib/cassandra:/var/lib/cassandra"
    end
  end

  config.vm.define "app" do |app|
    app.vm.box = "alonsodomin/ubuntu-trusty64-java8"
    app.vm.hostname = "kairos-app"
    app.vm.network "private_network", ip: "192.168.50.26"

    app.vm.synced_folder File.expand_path("~/.ivy2"), "/home/vagrant/.ivy2"

    app.vm.provision :shell, path: "vagrant/provision_app.sh"
    app.vm.provision :docker, version: "latest"

    app.vm.provision :shell, path: "vagrant/build_app.sh"

    app.vm.provision :docker do |d|
      d.run "kernel",
        image: "kairos/kernel:0.1.0-SNAPSHOT",
        args: "-p 8095:8095",
        cmd: "--cs 192.168.50.25:9042"
    end
  end

end
