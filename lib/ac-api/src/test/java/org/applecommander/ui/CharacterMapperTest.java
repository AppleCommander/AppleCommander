/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2026 by Robert Greene and others
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
package org.applecommander.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CharacterMapperTest {
    @Test
    public void testForcedUppercase() {
        CharacterMapper hex = CharacterMapper.builder()
                .mapRange('a', 'f', true)
                .mapRange('A', 'F', false)
                .mapRange('0', '9', false)
                .map('$', '$')
                .build();

        assertEquals("$12AF", hex.map(">>> $12af <<<"));
    }

    @Test
    public void testMixedCase() {
        CharacterMapper hex = CharacterMapper.builder()
                .mapRange('a', 'f', false)
                .mapRange('A', 'F', false)
                .mapRange('0', '9', false)
                .map('$', '$')
                .build();

        assertEquals("$12af", hex.map(">>> $12af <<<"));
    }
}
