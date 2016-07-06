#!/usr/bin/env bash

echo "Building project..."
cd /vagrant
sbt clean docker:publishLocal

