#!/bin/bash

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

report="$sbt_cmd coverageReport"
aggregate="$sbt_cmd coverageAggregate"

coverage="$report && $aggregate"

eval $coverage