# Developer Notes

AppleCommander has switched to using [Gradle](https://gradle.org/) for build and build dependencies.

There is still a bunch of ANT related build information around.  They no longer apply and "should" get cleaned up over time. 

## Tests

```
$ ./gradlew test
```

## Building

```
$ ./gradlew clean assemble

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
  
## Raspberry Pi Notes

Available versions found are: `3.8` and `4.6`. `4.6` failed with a JNI issue.

Retrieve libraries:

```
$ sudo apt install libswt-gtk-3-java libswt-gtk-3-jni
```

Combine into one JAR (`-j` drops the directories from `*.so` files in the zip file):

```
$ cp /usr/lib/java/swt-gtk-3.8.2.jar org.eclipse.swt.gtk.linux.arm-3.8.2.jar
$ zip -j org.eclipse.swt.gtk.linux.arm-3.8.2.jar /usr/lib/jni/libswt-*3836*.so 
```

Preserve JAR file in `rpi-lib`!  If the file name is incorrect, the build process will show the expected filename in the error message...

Error for 4.6:

```
** (SWT:12220): CRITICAL **: 15:12:55.506: JNI method ID pointer is NULL for method atkObject_get_role

java.lang.reflect.InvocationTargetException
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke(Method.java:498)
    at com.webcodepro.applecommander.ui.AppleCommander.launchSwtAppleCommander(AppleCommander.java:103)
    at com.webcodepro.applecommander.ui.AppleCommander.main(AppleCommander.java:57)
Caused by: java.lang.NoSuchMethodError: atkObject_get_role
    at org.eclipse.swt.internal.gtk.OS._g_main_context_iteration(Native Method)
    at org.eclipse.swt.internal.gtk.OS.g_main_context_iteration(OS.java:1581)
    at org.eclipse.swt.widgets.Display.readAndDispatch(Display.java:4470)
    at com.webcodepro.applecommander.ui.swt.SwtAppleCommander.launch(SwtAppleCommander.java:93)
    at com.webcodepro.applecommander.ui.swt.SwtAppleCommander.launch(SwtAppleCommander.java:79)
    ... 6 more
```