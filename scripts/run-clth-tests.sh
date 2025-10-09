#!/bin/bash

DIR=".bin"
if [ ! -d ${DIR} ]
then
  mkdir ${DIR}
fi

CLTH="${DIR}/clth.jar"
if [ ! -f ${CLTH} ]
then
  eval $(cat build.gradle | grep clthVersion | tr -d ' ')
  if [ -z "${clthVersion}" ]
  then
    echo "clthVersion was not found"
    return 1
  fi

  URL="https://github.com/a2geek/command-line-test-harness/releases/download/${clthVersion}/clth-${clthVersion}.jar"
  curl -o ${CLTH} -L ${URL}
fi

java -jar ${CLTH} app/cli-tests/src/test/resources/{ac,acx}-config.yml
