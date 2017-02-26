#!/bin/bash

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

build="$sbt clean coverage validate"

eval $build