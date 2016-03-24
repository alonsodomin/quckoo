# Quckoo

[![Build Status](https://travis-ci.org/alonsodomin/kairos.svg)](https://travis-ci.org/alonsodomin/kairos)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Quckoo is a fault-tolerant distributed task scheduler platform that runs on the JVM. The aim of the project is
the implementation of a reliable system able to run large amount of scheduled tasks without single points of failure.

To achieve that, Quckoo is composed of a cluster of scheduler nodes and ad hoc worker nodes that connect to this
cluster and request for work to be sent to them. It's basically a generalization of the [distributed worker
pattern](http://letitcrash.com/post/29044669086/balancing-workload-across-nodes-with-akka-2) (and in fact the
implementation owes a lot to the previous post).

## Documentation

 * [Wiki](https://github.com/alonsodomin/quckoo/wiki/Introduction)

## Building

To build the project input following commands in a terminal:

```
git clone https://github.com/alonsodomin/quckoo
cd quckoo
sbt package
```

### Bootstrapping DEV environment

The repository ships with a `Vagrantfile` in its root folder for ease the setup of a DEV environment. This Vagrant configuration
will create a virtual machine in the host computer and deploy into it the required components of the architecture as
Docker containers. To start this environment issue following command in a terminal window from inside your working copy:

```
vagrant up
```

Once it has finished loading, you should be able to access the Kairos UI in following URL:

http://192.168.50.25:8095

#### Build error when building the vagrant box

When trying to bootstrap the self-contained VM with Vagrant, SBT will be invoked from the inside the VM to publish
the Docker images in the host's local registry. When this is done the first time SBT may fail to communicate with
the local Docker daemon and you will see a build error. If this happens just re-provision the VM and it should work.

```
vagrant provision
```

## Contributing

Quckoo is still right now in _experimental_ phase, current codebase will be evolving until it reaches the level of
robustness necessary to be able to trust on the system to be able to handle the big load we are aiming for. Feel
free to fork this repository and contribute with your knowledge to make this project a reliable and really
fault-tolerant task scheduling platform.

## License

Copyright 2015 Antonio Alonso Dominguez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.