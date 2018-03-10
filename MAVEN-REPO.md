# Releasing to the Maven Central Repository

## GPG Keys

Summary of commands for GPG2 keys preparation...

Generate key:

```bash
$ gpg2 --gen-key
```

Publish key on public key server:

```bash
$ gpg2 --list-keys
$ gpg2 --list-secret-keys
$ gpg2 --keyserver hkp://pool.sks-keyservers.net --send-keys <key-id>
```

Extract secret key for the Gradle signing plugin:

```bash
$ gpg2 --export-secret-keys > secring.gpg
```

## Build and publish to Central Repository

Ensure full build passes:

```bash
$ ./gradlew clean test javadoc assemble
<...lots of stuff, primarily Javadoc issues...>
BUILD SUCCESSFUL in 3s
13 actionable tasks: 13 executed
```

Upload:

```bash
$ ./gradlew uploadArchives

BUILD SUCCESSFUL in 10s
10 actionable tasks: 1 executed, 9 up-to-date
```

Then follow "releasing the deployment" below.

## Tag and publish on GitHub

Just a reminder!

# References

* http://central.sonatype.org/pages/gradle.html
* http://central.sonatype.org/pages/releasing-the-deployment.html
* For all the other little pieces, Google is your friend. ;-)
