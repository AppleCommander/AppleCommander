# Releasing to the Maven Central Repository

## GPG Keys

> Note historically, it was `gpg2` but appears to now be `gpg` (which is v2 by default).

Summary of commands for GPG 2.x keys preparation...

Generate key:

```bash
$ gpg --gen-key
```

Publish key on public key server:

```bash
$ gpg --list-keys
$ gpg --list-secret-keys
$ gpg --keyserver hkp://pool.sks-keyservers.net --send-keys <key-id>
```

Extract secret key for the Gradle signing plugin:

```bash
$ gpg --export-secret-keys > secring.gpg
```

## Gradle config file

```bash
$ cat ~/.gradle/gradle.properties 
ossrhUsername=<generated user>
ossrhPassword=<generated password>

signing.keyId=<exported key id - last 8 characters>
signing.password=<passphrase>
signing.secretKeyRingFile=/home/rob/.gnupg/secring.gpg
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

As of 2025, the publishing in Maven Central has moved/changed. The plugin does work, but a special id needs to be generated.
Also note that after publishing, we have to tell Maven Central we're done.

The secrets are stored in the `.netrc` file:

```
machine ossrh-staging-api.central.sonatype.com login <generated user> password <generated password>
```

And this script will do the appropriate HTTP POST:

```bash
$ scripts/finish-publishing.sh
```

## Tag and publish on GitHub

Just a reminder!

# References

* https://central.sonatype.com/publishing/deployments to check on publishing status
* https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#ensuring-deployment-visibility-in-the-central-publisher-portal
* https://central.sonatype.org/publish/generate-portal-token/
* https://docs.gradle.org/8.13/userguide/signing_plugin.html#sec:signatory_credentials
* For all the other little pieces, Google is your friend. ;-)
