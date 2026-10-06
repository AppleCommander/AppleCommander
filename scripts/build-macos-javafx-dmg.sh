#!/bin/bash

source gradle.properties
SUBPROJECT="app/gui-javafx"
VERSION="${version//-SNAPSHOT/}"
YEAR=$(date +%Y)
MAIN_JAR=$(find ${SUBPROJECT}/build/libs -name "gui-javafx-*.jar")
ARCH=$(uname -m)

echo "Building AppleCommanderFX DMG for:"
echo "  SUBPROJECT=${SUBPROJECT}"
echo "  VERSION=${VERSION} (from ${version})"
echo "  YEAR=${YEAR}"
echo "  MAIN_JAR=${MAIN_JAR}"
echo "  ARCH=${ARCH}"

jpackage \
  --type dmg \
  --java-options --enable-native-access=javafx.graphics \
  --app-version "${VERSION}" \
  --copyright "Copyright ${YEAR}" \
  --description "AppleCommanderFX is a tool that manipulates Apple ][ disk images. Files may be imported, exported, viewed, or printed with various file filters." \
  --name "AppleCommanderFX" \
  --module-path "${MAIN_JAR}:${SUBPROJECT}/build/jars" \
  --module org.applecommander.javafx/org.applecommander.javafx.AppleCommanderFX \
  --about-url "https://applecommander.org" \
  --license-file LICENSE \
  --mac-package-identifier AppleCommanderFX \
  --mac-package-name AppleCommanderFX \
  --icon app/gui-javafx/src/main/resources/images/AppleCommanderIcon.icns

# There doesn't appear to be a mechanism to set the output name without
# changing the application name as well. So we just 'mv' it.
SRC="AppleCommanderFX-${VERSION}.dmg"
DST="AppleCommanderFX-${version}-mac-${ARCH}.dmg"
mv -v ${SRC} ${DST}

echo "Done!"
