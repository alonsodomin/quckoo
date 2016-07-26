#!/usr/bin/env bash

echo "Publishing into sandboxed Artifactory..."
cd /vagrant

sbt core/publish exampleJobs/publish