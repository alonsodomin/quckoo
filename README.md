# Chronos

[![Build Status](https://travis-ci.org/alonsodomin/chronos.svg)](https://travis-ci.org/alonsodomin/chronos)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)

Chronos is a fault-tolerant distributed task scheduler platform that runs on the JVM. The aim of the project is
the implementation of a reliable system able to run large amount of scheduled tasks without single points of failure.

To achieve that, Chronos is composed of a cluster of scheduler nodes and ad hoc worker nodes that connect to this
cluster and request for work to be sent to them. It's basically a generalization of the [distributed worker
pattern](http://letitcrash.com/post/29044669086/balancing-workload-across-nodes-with-akka-2) (and in fact the
implementation owes a lot to the previous post).

## Documentation

 * [Wiki](https://github.com/alonsodomin/chronos/wiki/Introduction)

## Contributing

Chronos is still right now in _experimental_ phase, current codebase will be evolving until it reaches the level of
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