#!/usr/bin/env bash

set -e

function install_devtools() {
    sudo apt-get -y update &> /dev/null

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