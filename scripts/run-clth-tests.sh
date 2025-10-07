#!/bin/bash

DIR=".bin"
if [ ! -d ${DIR} ]
then
  mkdir ${DIR}
fi

CLTH="${DIR}/clth.jar"
if [ ! -f ${CLTH} ]
then
  curl -o ${CLTH} -L https://github.com/a2geek/command-line-test-harness/releases/download/1.1/clth-1.1.jar
fi

java -jar ${CLTH} app/cli-tests/src/test/resources/{ac,acx}-config.yml
