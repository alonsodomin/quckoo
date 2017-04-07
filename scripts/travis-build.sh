#!/bin/bash

sbt_cmd="sbt ++$TRAVIS_SCALA_VERSION"

test_cmd="$sbt_cmd clean coverage test"
validate_cmd="$sbt_cmd coverage master/multi-jvm:test"
coverage_cmd="$sbt_cmd coverageReport && $sbt_cmd coverageAggregate"

build_cmd="$test_cmd && $coverage_cmd"

eval $build_cmd