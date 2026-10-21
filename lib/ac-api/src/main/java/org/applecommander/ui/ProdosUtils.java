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

import org.applecommander.util.FileMagic;

public abstract class ProdosUtils {
    public static String getFileTypeText(int fileType, int auxType) {
        return FileMagic.getProdosFileTypeText(fileType, auxType);
    }

    public static String getStorageTypeText(int storageType) {
        return switch (storageType) {
            case 0x1 -> "Seedling";
            case 0x2 -> "Sapling";
            case 0x3 -> "Tree";
            case 0x4 -> "Pascal";
            case 0x5 -> "Extended";
            case 0xd -> "Directory";
            case 0xe -> "Subdirectory";
            case 0xf -> "Volume Directory";
            default -> String.format("Unknown (%x)", storageType);
        };
    }

    public static String getAccessText(int access) {
        StringBuilder sb = new StringBuilder();
        String flags = "RWI--BRD";  // string index is bit value
        for (int i=0; i<flags.length(); i++) {
            int bit = i^2;
            if ((access & bit) != 0) {
                sb.append(flags, i, i+1);
            }
            else {
                sb.append("-");
            }
        }
        return sb.toString();
    }
}
