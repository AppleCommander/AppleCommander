#!/bin/bash

source gradle.properties
SUBPROJECT="app/gui-javafx"
VERSION="${version//-SNAPSHOT/}"
YEAR=$(date +%Y)
MAIN_JAR=$(find ${SUBPROJECT}/build/libs -name "gui-javafx-*.jar")
ARCH=$(uname -m)

echo "Building AppleCommanderFX RPM for:"
echo "  SUBPROJECT=${SUBPROJECT}"
echo "  VERSION=${VERSION} (from ${version})"
echo "  YEAR=${YEAR}"
echo "  MAIN_JAR=${MAIN_JAR}"
echo "  ARCH=${ARCH}"

jpackage \
  --type rpm \
  --java-options --enable-native-access=javafx.graphics \
  --app-version "${VERSION}" \
  --copyright "Copyright ${YEAR}" \
  --description "AppleCommanderFX is a tool that manipulates Apple ][ disk images. Files may be imported, exported, viewed, or printed with various file filters." \
  --name "AppleCommanderFX" \
  --module-path "${MAIN_JAR}:${SUBPROJECT}/build/jars" \
  --module org.applecommander.javafx/org.applecommander.javafx.AppleCommanderFX \
  --about-url "https://applecommander.org" \
  --license-file LICENSE \
  --icon lib/ac-swt-common/src/main/resources/linux/AppleCommander-128x128.png \
  --linux-deb-maintainer "robgreene@gmail.com" \
  --linux-menu-group Utility \
  --linux-rpm-license-type "GPL-2.0-or-later" \
  --linux-app-category "Utility"

# There doesn't appear to be a mechanism to set the output name without
# changing the application name as well. So we just 'mv' it.
SRC=$(find . -name "applecommanderfx*.rpm")
DST="AppleCommanderFX-${version}-linux-${ARCH}.rpm"
mv -v ${SRC} ${DST}

echo "Done!"
