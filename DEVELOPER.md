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

## Testing for Ant Task

The Apache Ant Task has been added as an independent "application" to the Gradle source structure. It resides in `app/ant-ac`. To test the Ant Task, there is an actual Ant build script available. Run this script from the AppleCommander project directory -- it should figure out paths from that starting position.

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
