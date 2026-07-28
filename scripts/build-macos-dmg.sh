#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: $0 <subproject-path>"
  exit 1
fi

source gradle.properties
SUBPROJECT="$1"
# shellcheck disable=SC2154
VERSION="${version//-SNAPSHOT/}"
YEAR=$(date +%Y)
# shellcheck disable=SC2086
MAIN_JAR=$(find ${SUBPROJECT}/build/jars -name "gui-swt-macosx-*-plain.jar" -exec basename {} \;)
ARCH=$(uname -m)

echo "Building AppleCommander DMG for:"
echo "  SUBPROJECT=${SUBPROJECT}"
echo "  VERSION=${VERSION} (from ${version})"
echo "  YEAR=${YEAR}"
echo "  MAIN_JAR=${MAIN_JAR}"
echo "  ARCH=${ARCH}"

jpackage \
  --input "${SUBPROJECT}/build/jars" \
  --type dmg \
  --java-options -XstartOnFirstThread \
  --java-options --enable-native-access=ALL-UNNAMED \
  --app-version "${VERSION}" \
  --copyright "Copyright ${YEAR}" \
  --description "AppleCommander is a tool that manipulates Apple ][ disk images. Files may be imported, exported, viewed, or printed with various file filters." \
  --name "AppleCommander" \
  --main-jar "${MAIN_JAR}" \
  --about-url "https://applecommander.org" \
  --license-file LICENSE \
  --main-class com.webcodepro.applecommander.ui.swt.SwtAppleCommander \
  --mac-package-identifier AppleCommander \
  --mac-package-name AppleCommander \
  --icon lib/ac-swt-common/src/main/resources/mac/AppleCommander.icns

# There doesn't appear to be a mechanism to set the output name without
# changing the application name as well. So we just 'mv' it.
SRC="AppleCommander-${VERSION}.dmg"
DST="AppleCommander-${VERSION}-${ARCH}.dmg"
mv -v ${SRC} ${DST}

echo "Done!"
