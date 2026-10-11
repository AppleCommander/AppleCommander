#!/bin/bash

find . -name "*.java" -not -name "Sleak.java" -not -name module-info.java -not -name package-info.java | xargs grep -L "GNU General Public License"
