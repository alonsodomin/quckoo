# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"
Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.env.enable
  config.vm.box = "phusion/ubuntu-14.04-amd64"
  config.vm.hostname = "chronos-vagrant"

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

  config.vm.define "store" do |store|
    store.vm.network "private_network", ip: "192.168.33.25"

    # Setting up docker provisioner in a separate line to allow for the proxy configuration
    store.vm.provision :docker, version: "latest"
    store.vm.provision :docker do |d|
      d.run "cassandra", args: "-p 7000:7000 -p 9042:9042 -p 9160:9160"
    end
  end

  config.vm.define "mesos" do |mesos|
    mesos.vm.network "private_network", ip: "192.168.33.26"
    mesos.vm.provision :shell, path: "vagrant/provision_mesos.sh"
  end

end
