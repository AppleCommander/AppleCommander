# Developer Notes

AppleCommander has switched to using [Gradle](https://gradle.org/) for build and build dependencies.

There is still a bunch of ANT related build information around.  They no longer apply and "should" get cleaned up over time. 

## Tests

```
$ ./gradlew test
```

## Building

```
$ ./gradlew assemble

BUILD SUCCESSFUL in 2s
6 actionable tasks: 6 executed

$ tools/retrolambda.sh build/libs/AppleCommander-ac-<version>.jar
Converting...
Retrolambda 2.5.6
Repackaging to build/libs/AppleCommander-ac-<version>-java5.jar
```

This will:
1. Create the `build` directory and populate with uber-jars in `build/libs`.
2. Create a Java 5 variant of the `ac` tool in `AppleCommander-ac-<version>-java5.jar`. (Replace `<version>` with version.) 

To run the command-line version of AppleCommander, use the following:

* All platforms:
  `java -jar build/libs/AppleCommander-ac-VERSION.jar`
* Java 5:
  `java -jar build/libs/AppleCommander-ac-VERSION-java5.jar`

To launch the GUI version of AppleCommander, use the following:

* Linux:
  `java -jar build/libs/AppleCommander-linux64-gtk-VERSION.jar`
* Windows:
  `java -jar build/libs/AppleCommander-win64-VERSION.jar`
* Mac OS X:
  `java -XstartOnFirstThread -jar build/libs/AppleCommander-macosx-VERSION.jar`
  
