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
    sudo dpkg -i ~/sbt-0.13.9.deb &> /dev/null
    rm ~/sbt-0.13.9.deb
}

########## PROGRAM STARTS HERE ##############

function main() {
    install_devtools
    touch ~/.devtools_provisioned

    # Prepare the local SBT repository folder
    mkdir -p /home/vagrant/.ivy2/local
    chown -R vagrant:vagrant /home/vagrant/.ivy2/local
    chmod -R 777 /home/vagrant/.ivy2/local

    # Forcing a re-start of the docker daemon
    sudo service docker restart
}

# Only run main function on first build
test -f ~/.devtools_provisioned || main