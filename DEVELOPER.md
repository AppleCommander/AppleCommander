# Developer Notes

AppleCommander has switched to using [Gradle](https://gradle.org/) for build and build dependencies.

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

To launch AppleCommander, use the following:

* Mac OS X:
  `java -XstartOnFirstThread -jar build/libs/AppleCommander.jar`

The build is currently only setup for Mac OS X. 
