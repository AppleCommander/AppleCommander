package io.github.applecommander.acx;

import com.webcodepro.applecommander.ui.AppleCommander;

import io.github.applecommander.applesingle.AppleSingle;
import picocli.CommandLine.IVersionProvider;

/** Display version information.  Note that this is dependent on the Spring Boot Gradle plugin configuration. */
public class VersionProvider implements IVersionProvider {
    public String[] getVersion() {
    	return new String[] { 
            String.format("acx: %s", Main.class.getPackage().getImplementationVersion()),
            String.format("AppleCommander API: %s", AppleCommander.VERSION),
            String.format("AppleSingle API: %s", AppleSingle.VERSION)
		};
    }
}