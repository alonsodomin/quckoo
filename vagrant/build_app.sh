#!/usr/bin/env bash

echo "Building project..."
cd /vagrant
export JAVA_OPTS="-Dfile.encoding=UTF8 -Xms1g -Xmx2g -Xss2m -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC"
./activator docker:publishLocal &> /dev/null

