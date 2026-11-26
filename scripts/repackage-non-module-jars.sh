#!/bin/bash

set -e

#
# See: https://stackoverflow.com/questions/77634820/how-to-create-a-module-info-class-file-and-add-it-to-the-jar
# And: https://github.com/FXMisc/RichTextFX/issues/902
#
function patch() {
  package=$1
  file=$2

  echo "######################## $package - $file #######################"

  alternatePackage="../src/main/module-infos/$package.java"
  echo "Copying $alternatePackage into jars folder"
  cp $alternatePackage jars/module-info.java

  echo "module-info.java for $file is:"
  echo "*** START ***"
  cat jars/module-info.java
  echo "*** -END- ***"

  echo "Building module-info.java"
  javac --module-path jars --patch-module $package=$file jars/module-info.java

  echo "Patching module-info.class into $file"
  jar uf $file -C jars module-info.class

  echo "Cleaning up"
  rm jars/module-info.*
}


pushd app/gui-javafx/build

# Done in dependency order
patch reactfx jars/reactfx-*.jar
patch org.fxmisc.flowless jars/flowless-*.jar
patch org.fxmisc.undo jars/undofx-*.jar
patch wellbehavedfx jars/wellbehavedfx-*.jar
patch org.fxmisc.richtext jars/richtextfx-*.jar

popd
