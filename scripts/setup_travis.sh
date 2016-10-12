#!/usr/bin/env bash

echo "Installing SASS compiler..."
gem install compass

echo "Upgrading PhantomJS 2..."
mkdir /tmp/phantomjs2
wget https://s3.amazonaws.com/travis-phantomjs/phantomjs-2.0.0-ubuntu-12.04.tar.bz2
mv phantomjs-2.0.0-ubuntu-12.04.tar.bz2 /tmp/phantomjs2/
cd /tmp/phantomjs2
tar -xjf phantomjs-2.0.0-ubuntu-12.04.tar.bz2
sudo rm -rf /usr/local/phantomjs/bin/phantomjs
sudo mv phantomjs /usr/local/phantomjs/bin/phantomjs