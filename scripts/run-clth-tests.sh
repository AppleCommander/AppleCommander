#!/bin/bash

DIR="${PWD}/.bin"
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

# Note that CLTH ends up in the cli-tests directory when run in IntelliJ. For now, we're adjusting to make it work in both cases.
cd app/cli-tests
java -jar ${CLTH} src/test/resources/{ac,acx}-config.yml
