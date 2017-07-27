#!/bin/bash

echo "Installing SASS compiler..."
gem install compass

echo "Installing Node..."
nvm install node

echo "Installing PhantomJS 2..."
npm install -g phantomjs-prebuilt