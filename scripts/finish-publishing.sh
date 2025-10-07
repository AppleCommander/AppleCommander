#!/bin/bash

# Note: Credentials are in the netrc file
curl --netrc -v -X POST https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/org.applecommander
