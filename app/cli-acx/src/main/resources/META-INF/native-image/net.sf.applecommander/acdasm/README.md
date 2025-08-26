# README

These files were generated for the AppleCommander disassembler and it's usage within 'acx'. Hypothetically, once
the disassembler project gets updated for GraalVM native image capabilities, these files can be removed.

Generation command:

```shell
$ java -agentlib:native-image-agent=config-merge-dir=app/cli-acx/src/main/resources/META-INF/native-image \
       -jar app/cli-acx/build/libs/AppleCommander-acx-12.0-SNAPSHOT.jar \
       dump -d deleteme.po -b 0 --disassembly
```
