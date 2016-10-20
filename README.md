# Quckoo

[![Build Status](https://travis-ci.org/alonsodomin/quckoo.svg?branch=master)](https://travis-ci.org/alonsodomin/quckoo)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/e9f5ba8e800e494f995643d50927783f)](https://www.codacy.com/app/alonso-domin/quckoo?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=alonsodomin/quckoo&amp;utm_campaign=Badge_Grade)
[![codecov](https://codecov.io/gh/alonsodomin/quckoo/branch/master/graph/badge.svg)](https://codecov.io/gh/alonsodomin/quckoo)
[![License](http://img.shields.io/:license-Apache%202-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.quckoo/quckoo-core_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.quckoo/quckoo-core_2.11)

Quckoo is a fault-tolerant distributed task scheduler platform that runs on the JVM. The aim of the project is
the implementation of a reliable system able to run large amount of scheduled tasks without single points of failure.

To achieve that, Quckoo is composed of a cluster of scheduler nodes and ad hoc worker nodes that connect to this
cluster and request for work to be sent to them. It's basically a generalization of the [distributed worker
pattern](http://letitcrash.com/post/29044669086/balancing-workload-across-nodes-with-akka-2) (and in fact the
implementation owes a lot to the previous post).

## Documentation

 * [Wiki](https://github.com/alonsodomin/quckoo/wiki/Introduction)

## Building

First of all, ensure you have the following tools in your local machine:

 * [Java JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 1.8+ for running and compiling the source code
 * [SBT](http://www.scala-sbt.org) 0.13.5+ for building the whole project
 * [NodeJS](https://nodejs.org/en/) 4.5.x+ for running and optimizing the ScalaJS code
 * [PhantomJS](http://phantomjs.org) 2.x+ for running the ScalaJS test suites
 * [SASS](http://sass-lang.com) 3.4.0+ for compiling UI's stylesheets.

Optionally, if you will want to be able to run Quckoo in your local machine
using the Docker containers you will need one of the following (or both):

 * [Docker](http://www.docker.com) 1.10+ for running the docker containers "natively" in your local machine
 * [Vagrant](http://www.vagrantup.com) 1.8.x+ plus a Virtual Machine provider (i.e.: [VirtualBox](http://www.virtualbox.org) 5.x+) for bootstrapping the sandbox.

To build the project input following commands in a terminal:

```
git clone https://github.com/alonsodomin/quckoo
cd quckoo
sbt package
```

### Sandbox environment

The repository ships with a `Vagrantfile` in its root folder for ease the setup of a Sandboxed environment. This Vagrant configuration
will create a virtual machine in the host computer and deploy into it the required components of the architecture as
Docker containers. To start this environment issue following command in a terminal window from inside your working copy:

```
vagrant up
```

Once it has finished loading, you should be able to access the Quckoo UI in following URL:

http://192.168.50.25:8095

_Use `admin` and `password` as credentials when prompted._

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