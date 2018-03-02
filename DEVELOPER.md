# Developer Notes

AppleCommander has switched to using [Gradle](https://gradle.org/) for build and build dependencies.

There is still a bunch of ANT related build information around.  They no longer apply and "should" get cleaned up over time. 

## Tests

> Note that test currently fail due to dependencies on disk images not present in the repository.

```
$ ./gradlew test
```

## Building

```
$ ./gradlew assemble

BUILD SUCCESSFUL in 2s
6 actionable tasks: 6 executed
```

This will create the `build` directory and populate with an uber-jar.

To run the command-line version of AppleCommander, use the following:

* All platforms:
  `java -jar AppleCommander-ac-VERSION.jar`

To launch the GUI version of AppleCommander, use the following:

* Linux:
  `java -jar AppleCommander-linux64-gtk-VERSION.jar`
* Windows:
  `java -jar AppleCommander-win64-VERSION.jar`
* Mac OS X:
  `java -XstartOnFirstThread -jar build/libs/AppleCommander-macosx-VERSION.jar`
  
