#!/bin/bash

set -e

# Shadow jar isn't really needed, but it's a cheap way to get jars into one place
./gradlew shadowJar

MAIN=leapfin.Main
JAR=leapfin-example-all.jar

java -cp ./build/libs/${JAR} $MAIN "$@"


