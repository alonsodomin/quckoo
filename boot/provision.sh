#!/usr/bin/env bash

set -e

function install_devtools() {
    sudo apt-get -y update &> /dev/null

    echo "Installing nodejs..."
    sudo apt-get install -y nodejs npm &> /dev/null
    sudo ln -s /usr/bin/nodejs /usr/bin/node

    echo "Installing PhantomJS..."
    sudo npm install -g phantomjs &> /dev/null

    echo "Installing SBT..."
    wget -nv https://dl.bintray.com/sbt/debian/sbt-0.13.9.deb -O ~/sbt-0.13.9.deb
    sudo dpkg -i ~/sbt-0.13.9.deb
    rm ~/sbt-0.13.9.deb
}

########## PROGRAM STARTS HERE ##############

function main() {
    install_devtools
    touch ~/.devtools_provisioned
}

test -f ~/.devtools_provisioned || main