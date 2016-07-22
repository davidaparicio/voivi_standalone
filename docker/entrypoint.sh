#!/bin/bash

set -e

#JAVA_OPTS=${JAVA_OPTS:="-Xmx256m"}

exec java -jar $jar -conf config.json
