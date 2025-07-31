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

## Grande config file

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
Also note that after publishing, we have to tell Maven Central we're done. That can be done with the following curl (which takes some time).

```bash
$ curl -v -X POST https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/net.sf.applecommander --user "<user>:<password>"
> POST /manual/upload/defaultRepository/net.sf.applecommander HTTP/2
> Host: ossrh-staging-api.central.sonatype.com
> Authorization: Basic UndSNzlOOmxYSjJGWmlQMkxXNHNrS1NNeUoxeWhDaGFSV0tpdXJpNw==
> User-Agent: curl/8.9.1
> Accept: */*
> 
* Request completely sent off
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
< HTTP/2 200 
< date: Thu, 31 Jul 2025 18:27:21 GMT
< content-length: 0
<
* Connection #0 to host ossrh-staging-api.central.sonatype.com left intact
```

## Tag and publish on GitHub

Just a reminder!

# References

* https://central.sonatype.com/publishing/deployments to check on publishing status
* https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#ensuring-deployment-visibility-in-the-central-publisher-portal
* https://central.sonatype.org/publish/generate-portal-token/
* https://docs.gradle.org/8.13/userguide/signing_plugin.html#sec:signatory_credentials
* For all the other little pieces, Google is your friend. ;-)
