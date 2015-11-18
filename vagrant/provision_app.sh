#!/usr/bin/env bash

set -e

function install_devtools() {
    echo "Installing Java 8..."
    sudo apt-get install -y software-properties-common &> /dev/null
    sudo add-apt-repository -y ppa:webupd8team/java &> /dev/null

    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
    sudo apt-get update &> /dev/null
    sudo apt-get install -y oracle-java8-installer &> /dev/null
    sudo apt-get install -y oracle-java8-set-default &> /dev/null

    echo "Installing nodejs..."
    sudo apt-get install -y nodejs npm &> /dev/null
    sudo ln -s /usr/bin/nodejs /usr/bin/node

    echo "Installing PhantomJS..."
    sudo npm install -g phantomjs &> /dev/null
}

########## PROGRAM STARTS HERE ##############

function main() {
    install_devtools
    touch ~/.devtools_provisioned
}

test -f ~/.devtools_provisioned || main