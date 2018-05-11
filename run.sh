#!/bin/bash

set -e

MAIN=leapfin.Main
JAR=leapfin-example-all.jar

java -cp ./build/libs/${JAR} $MAIN "$@"


