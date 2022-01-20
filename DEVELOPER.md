# Developer Notes

AppleCommander has switched to using [Gradle](https://gradle.org/) for build and build dependencies.

There is still a bunch of ANT related build information around.  They no longer apply and "should" get cleaned up over time. 

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

## Building

```
./gradlew clean build 

BUILD SUCCESSFUL in 7s
104 actionable tasks: 104 executed
```
