#!/usr/bin/env sh

git clone https://github.com/ochrons/diode.git .diode
cd .diode
git checkout nextRelease
sbt publishLocal