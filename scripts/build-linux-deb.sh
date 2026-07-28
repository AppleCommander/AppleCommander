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
MAIN_JAR=$(find ${SUBPROJECT}/build/jars -name "gui-swt-linux-*-plain.jar" -exec basename {} \;)
ARCH=$(uname -m)

echo "Building AppleCommander DEB for:"
echo "  SUBPROJECT=${SUBPROJECT}"
echo "  VERSION=${VERSION} (from ${version})"
echo "  YEAR=${YEAR}"
echo "  MAIN_JAR=${MAIN_JAR}"
echo "  ARCH=${ARCH}"

jpackage \
  --input "${SUBPROJECT}/build/jars" \
  --type deb \
  --java-options --enable-native-access=ALL-UNNAMED \
  --app-version "${VERSION}" \
  --copyright "Copyright ${YEAR}" \
  --description "AppleCommander is a tool that manipulates Apple ][ disk images. Files may be imported, exported, viewed, or printed with various file filters." \
  --name "AppleCommander" \
  --main-jar "${MAIN_JAR}" \
  --about-url "https://applecommander.org" \
  --license-file LICENSE \
  --main-class com.webcodepro.applecommander.ui.swt.SwtAppleCommander \
  --icon lib/ac-swt-common/src/main/resources/linux/AppleCommander-128x128.png \
  --linux-deb-maintainer "Rob Greene <robgreene@gmail.com>" \
  --linux-menu-group Utility \
  --linux-rpm-license-type "GPL-2.0-or-later" \
  --linux-app-category "Utility" \
  --linux-app-release "${VERSION}"

# There doesn't appear to be a mechanism to set the output name without
# changing the application name as well. So we just 'mv' it.
SRC=$(find . -name "applecommander*.deb")
DST="AppleCommander-${VERSION}-${ARCH}.deb"
mv -v ${SRC} ${DST}

echo "Done!"
