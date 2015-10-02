#!/usr/bin/env bash

set -e

function install_mesos() {
    sudo apt-get install -y software-properties-common &> /dev/null

    echo "Configuring package sources..."
    APT_KEY_ARGS="--keyserver hkp://keyserver.ubuntu.com:80 --recv E56151BF"
    if [ ! -z "$HTTP_PROXY" ]; then
        APT_KEY_ARGS="--keyserver-options http-proxy=$HTTP_PROXY $APT_KEY_ARGS"
    fi

    sudo -E apt-key adv ${APT_KEY_ARGS} &> /dev/null
    DISTRO=$(lsb_release -is | tr '[:upper:]' '[:lower:]')
    CODENAME=$(lsb_release -cs)

    echo "deb http://repos.mesosphere.com/${DISTRO} ${CODENAME} main" | sudo tee /etc/apt/sources.list.d/mesosphere.list
    sudo -E add-apt-repository -y ppa:webupd8team/java &> /dev/null
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
    sudo apt-get -y update &> /dev/null

    echo "Installing Java 8..."
    sudo apt-get install -y oracle-java8-installer &> /dev/null
    sudo apt-get install -y oracle-java8-set-default &> /dev/null

    echo "Installing autotools..."
    sudo apt-get install -y autoconf libtool &> /dev/null

    echo "Installing build dependencies..."
    sudo apt-get install -y build-essential python-dev python-boto libcurl4-nss-dev libsasl2-dev maven libapr1-dev libsvn-dev &> /dev/null

    echo "Installing Mesos and Marathon..."
    sudo apt-get install -y mesos marathon &> /dev/null
}

########## PROGRAM STARTS HERE ##############

function main() {
    install_mesos
    touch ~/.mesos_provisioned
}

test -f ~/.mesos_provisioned || main