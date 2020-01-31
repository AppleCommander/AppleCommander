#!/bin/bash

TMP=$(mktemp -d)
if [ -z ${TMP} ]
then
    echo "Problem creating TEMP directory; aborting!"
    exit 1
fi

pushd build
  for JAR in $(find . -name "*.jar" -a -not -name "*-sources.jar" -a -not -name "*-javadoc.jar")
  do
    echo "Checking ${JAR}..."
    unzip -qq -d ${TMP} ${JAR}
    find ${TMP} -name "*.class" | xargs file -b | sort -u
    rm -rf ${TMP}/*
    echo
  done
popd

rm -rf ${TMP}
