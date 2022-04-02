# Developer Notes

AppleCommander has switched to using [Gradle](https://gradle.org/) for build and build dependencies.

There is still a bunch of ANT related build information around in `ant-build`.  This should get cleaned up over time. 

## Structure

The project is structured as a Gradle multi-project.  Independent components have been broken out and each of the SWT targets are independent.

| Path | Note |
| ---- | ---- |
| `lib/ac-api` | The AppleCommander APIs. These are released via Maven and resused in several projects. |
| `lib/ac-swt-common` | The SWT GUI application. Since SWT targets specific environments with native libraries, the actual applications are in the `app` directories. |
| `app/cli-ac` | The `ac` CLI utility. |
| `app/cli-acx` | The `acx` CLI utility. |
| `app/gui-swt-<os>-<arch>` | The indepent SWT GUI applications; one project per combination. |

## Requirements

With the introduction of the Apple Silicon, AppleCommander switched over to the (relatively new) SWT libraries. With that switch, the SWT libraries now require Java 11.

## Tests

```
./gradlew test

BUILD SUCCESSFUL in 554ms
19 actionable tasks: 19 up-to-date
```

## Testing of the Ant Task

The Ant Task testing has been enbedded within the Gradle build for Ant itself and does not need to be run separately. The `app/ant-ac/build.gradle` script reads from the `app/ant-ac/src/test/resources/build-testacant.xml` and executes the tests as part of the testing task. 

However, if the Ant script itself is being worked upon, it may be useful to run it outside of the build process. Run the following from the main AppleCommander project directory; note that this requires Ant to be installed and active. As usual, note your Java versions and ensure that Java 11 is active.

```
$ ant -f app/ant-ac/src/test/resources/build-testacant.xml
Buildfile: /.../AppleCommander/app/ant-ac/src/test/resources/build-testacant.xml

<snip>

BUILD SUCCESSFUL
Total time: 0 seconds
```

## Building

```
./gradlew clean build 

BUILD SUCCESSFUL in 7s
104 actionable tasks: 104 executed
```
