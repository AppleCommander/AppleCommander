#!/bin/bash +x

JAR="$(find app/cli-acx/build/libs/ -type f -not -name '*-plain.jar')"

function acx() {
  java -agentlib:native-image-agent=config-merge-dir=app/cli-acx/src/main/resources/META-INF/native-image \
       -jar "${JAR}" \
       "$@"
}

printf "\n\n*** RUNNING BT THROUGH A NUMBER OF COMMANDS TO CAPTURE DETAILS FOR NATIVE IMAGE ***\n\n"

acx
acx help
