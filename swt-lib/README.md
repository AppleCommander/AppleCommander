# Notes for Raspberry Pi

* Current target is Rasbian; 32-bit Rasbian and the 32-bit Rasberry Pi OS should also be ok. These are not part of the repository and are managed manually for now.
* The Eclipse builds do appear to include a 64-bit version, so expecting that Rasberry Pi OS (64-bit) will be ok.

# Updates

Process is manual.

1. Go to http://raspbian.raspberrypi.org/raspbian/pool/main/s/swt4-gtk/ and grab the latest copy of the `libswt-*` libraries.
2. Versions are super confusing. For instance, the files with `4.13.0` have a `version.txt` that suggests `4.924` as the version. None of these match what Eclipse is publishing. However, the Java manifest (`META-INF/MANIFEST.MF`) has a `Bundle-Version` entry that indicates `3.104.0`. Knowing that the Eclipse published version is `3.118.0` and that `version.txt` indicates `4.948` suggests the bundle version is the correct version entry to use.
3. See https://unix.stackexchange.com/questions/138188/easily-unpack-deb-edit-postinst-and-repack-deb regarding unpacking of the deb files.
   ```
   mkdir tmp
   dpkg-deb -R libswt-gtk-4-java_4.13.0-1+b2_armhf.deb tmp
   mkdir tmp2
   dpkg-deb -R libswt-gtk-4-jni_4.13.0-1+b2_armhf.deb tmp2
   ```
4. Look for the JAR files. We need the ones with `.class` files and some set of the `.so` files that are created. These need to be combined. Launching AppleCommander with the JAR should be sufficient to verify all code is in place. Probably.
5. Name the file according to pattern and place in `swt-lib`.


NOTE: SWT4 seems to be 64-bit only? May need to pull earlier versions.
These are at http://raspbian.raspberrypi.org/raspbian/pool/main/s/swt-gtk/
