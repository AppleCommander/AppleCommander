/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2025 by Robert Greene and others
 * robgreene at users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package io.github.applecommander.acx;

import io.github.a2geek.clth.Config;
import io.github.a2geek.clth.JUnitHelper;
import io.github.a2geek.clth.TestHarness;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import io.github.a2geek.clth.TestSuite;

public class AcxCommandLineTest {
    @ParameterizedTest(name = "{1}: {2}")
    @MethodSource("testCases")
    public void test(TestSuite testSuite, String name, String parameters) {
        TestHarness.Settings settings = TestHarness.settings()
                .deleteFiles()
                .baseDirectory(Path.of("cli-tests/src/test/resources"))
                .enableAlwaysShowOutput()
                .get();
        TestHarness.run(testSuite, JUnitHelper::execute, settings);
    }

    public static Stream<Arguments> testCases() {
        try (InputStream inputStream = AcxCommandLineTest.class.getResourceAsStream("/acx-config.yml")) {
            assert inputStream != null;
            String document = new String(inputStream.readAllBytes());
            Config config = Config.load(document);

            return TestSuite.build(config)
                    .map(t -> Arguments.of(t, t.testName(), String.join(" ", t.variables().values())));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
