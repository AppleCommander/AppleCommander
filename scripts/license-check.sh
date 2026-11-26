#!/bin/bash

find . -name "*.java" \
    -not -name "Sleak.java" \
    -not -name module-info.java \
    -not -name package-info.java \
    -not -path "*/module-infos/*" |
  xargs grep -L "GNU General Public License"
