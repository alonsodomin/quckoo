#!/usr/bin/env bash

echo "Publishing into sandboxed Artifactory..."
cd /vagrant

sbt coreJVM/publish exampleJobs/publish