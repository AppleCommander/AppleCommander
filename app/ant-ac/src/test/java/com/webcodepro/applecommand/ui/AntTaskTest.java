package com.webcodepro.applecommand.ui;

import com.webcodepro.applecommander.ui.AntTask;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This unit test is used to "mock" Ant itself. Current IDE does not allow debugging, and
 * because "ant" is alphabetized first, it runs first and fails first. Without debugging,
 * it gets annoying. This allows a JUnit based approach to decipher what is going on. Ugh.
 */
public class AntTaskTest {
    /**
     * <property name="tmpdir" value="build/tmp" />
     * <property name="dos140image" value="${tmpdir}/test-ant-dos140.do"/>
     * <property name="appantdir" value="app/ant-ac/src/test/resources" />
     * <appleCommander command="dos140" imagename="${dos140image}" />
     * <appleCommander command="p" input="${appantdir}/manifest.mf"
     * 	imagename="${dos140image}" filename="MANIFEST" type="T" />
     */
    @Test
    public void testPutFileOnDOS140kImage() {
        final String dos140image = "build/tmp/test-ant-dos140.do";
        final String appantdir = "src/test/resources";
        assertDoesNotThrow(() -> {
            AntTask t = new AntTask();
            t.setCommand("dos140");
            t.setImageName(dos140image);
            t.execute();
        }, "Creating DOS 140K image");

        assertDoesNotThrow(() -> {
            AntTask t = new AntTask();
            t.setCommand("p");
            t.setInput(appantdir + "/manifest.mf");
            t.setImageName(dos140image);
            t.setFileName("MANIFEST");
            t.setType("T");
            t.execute();
        }, "'ac' put file onto DOS 140K image");
    }
}
