#!/usr/bin/env bash

echo "Building project..."
cd /vagrant

sbt docker:publishLocal coreJVM/publish exampleJobs/publish

