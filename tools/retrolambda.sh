#!/bin/sh

set -x

if [ $# -ne 1 ]
then
    echo Provide on jar file to convert on the command line.
    exit 1
fi

REALPATH=$(realpath $0)
BIN_DIR=`dirname "$REALPATH"`
LIB_DIR=$BIN_DIR

JAR_TO_CONVERT="$1"
INPUT_DIR="$JAR_TO_CONVERT".inputdir
OUTPUT_DIR="$JAR_TO_CONVERT".outputdir

rm -rf "$INPUT_DIR"
rm -rf "$OUTPUT_DIR"
mkdir "$INPUT_DIR"
mkdir "$OUTPUT_DIR"
(cd "$INPUT_DIR"; jar -xvf $OLDPWD/"$JAR_TO_CONVERT")

java \
    -Dretrolambda.bytecodeVersion=49          \
    -Dretrolambda.defaultMethods=true         \
    -Dretrolambda.inputDir="$INPUT_DIR"       \
    -Dretrolambda.outputDir="$OUTPUT_DIR"     \
    -Dretrolambda.classpath="$JAR_TO_CONVERT" \
    -Dretrolambda.javacHacks=true             \
    -Dretrolambda.quiet=false                 \
    -jar "$LIB_DIR"/retrolambda-2.5.6.jar

# The order of the "m" and "f" must match the order that the
# corresponding files appear in the argument list. This is
# so stupid...
jar -cvfm "$JAR_TO_CONVERT".java5.jar \
    "$OUTPUT_DIR"/META-INF/MANIFEST.MF \
    -C "$OUTPUT_DIR" .
