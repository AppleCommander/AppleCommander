These are notes regarding releasing AppleCommaner into the Maven Central Repository.

For future reference, the OSSRH ticket is located at:
	https://issues.sonatype.org/browse/OSSRH-26096

Due to the fact that I no longer own the webcodepro.com domain, AppleCommander has
been released under the net.sf.applecommander package.

GAV for AppleCommander:

    <dependency>
      <groupId>net.sf.applecommander</groupId>
      <artifactId>AppleCommander</artifactId>
      <version>1.3.5.14</version>
    </dependency>

As AppleCommander itself is not a Maven based project, the release process chosen is the
manual one.  

Quick notes:
1. The person performing the release must be able to sign the JAR files.
2. It is required to include sources and javadocs in a JAR file.
3. Using the bundle.jar result is quite simple.
4. "bundle-pom.xml" is the POM used for releasing; update version information and rename
   appropriately.

Generating javadoc:

The following ANT script was used to generate javadoc on the fly:

    <project name="AppleCommander" default="doc">
      <target name="doc" description="generate documentation">
        <javadoc sourcepath="./sources/src" destdir="./javadoc"/>
      </target>
    </project>

And then jarred up and named appropriately.

A quick overview of the bundle creation is as follows:

    $ find . -name "AppleCommander*" -exec gpg -ab {} \;
    $ jar -cvf bundle.jar AppleCommander*

Contents of bundle.jar is as follows:

    $ jar -tvf bundle.jar
         0 Sun Nov 13 12:30:30 CST 2016 META-INF/
        69 Sun Nov 13 12:30:30 CST 2016 META-INF/MANIFEST.MF
    499018 Sun Nov 13 10:18:02 CST 2016 AppleCommander-1.3.5.14.jar
       473 Sun Nov 13 12:30:30 CST 2016 AppleCommander-1.3.5.14.jar.asc
    753461 Sun Nov 13 12:27:06 CST 2016 AppleCommander-1.3.5.14-javadoc.jar
       473 Sun Nov 13 12:30:30 CST 2016 AppleCommander-1.3.5.14-javadoc.jar.asc
      1204 Sun Nov 13 12:30:14 CST 2016 AppleCommander-1.3.5.14.pom
       473 Sun Nov 13 12:30:30 CST 2016 AppleCommander-1.3.5.14.pom.asc
    851262 Sun Nov 13 10:18:02 CST 2016 AppleCommander-1.3.5.14-sources.jar
       473 Sun Nov 13 12:30:30 CST 2016 AppleCommander-1.3.5.14-sources.jar.asc

Read the reference material as it was very informative.

References:
* http://central.sonatype.org/pages/manual-staging-bundle-creation-and-deployment.html
* http://central.sonatype.org/pages/releasing-the-deployment.html
* https://oss.sonatype.org/

