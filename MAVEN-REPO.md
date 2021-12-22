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

## Gradle build and publish to Central Repository

> NOTE: The build has been updated to allow snapshots to be published.  These appear to be automatically available.

Ensure full build passes:

```bash
 ./gradlew clean build

BUILD SUCCESSFUL in 8s
91 actionable tasks: 91 executed
```

Publish:

```bash
$ ./gradlew publish

BUILD SUCCESSFUL in 8s
2 actionable tasks: 2 executed
```

The can also be combined:

```bash
$ ./gradlew clean build publish

BUILD SUCCESSFUL in 16s
93 actionable tasks: 93 executed
```

Then follow "releasing the deployment" below.

## Tag and publish on GitHub

Just a reminder!

# References

* http://central.sonatype.org/pages/gradle.html (NOTE: Documentation is out of date)
* http://central.sonatype.org/pages/releasing-the-deployment.html
* https://docs.gradle.org/current/userguide/publishing_maven.html
* For all the other little pieces, Google is your friend. ;-)
