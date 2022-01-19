/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2019-2022 by Robert Greene and others
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
package io.github.applecommander.acx.fileutil;

import java.util.Date;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.os.dos33.DosFileEntry;
import com.webcodepro.applecommander.storage.os.pascal.PascalFileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;

public interface FileEntryWriter {
    //  FileEntry common
    public default void setFilename(String filename)                { }
    public default void setProdosFiletype(String filetype)          { }
    public default void setLocked(boolean flag)                     { }
    public default void setFileData(byte[] data)                    { }
    // Special case for GS/OS files (uglifies API; sets 0x05)
    public default void setFileData(byte[] data, byte[] resource)   { }
    /** 
     * The address embedded in binary objects. 
     * This varies by DOS's so is split apart. 
     */
    public default void setBinaryAddress(int address)               { }
    /** 
     * The length embedded in binary, Applesoft, Integer BASIC objects. 
     * This varies by DOS's so is split apart. 
     */
    public default void setBinaryLength(int length)                 { }
    // ProdosFileEntry specific
    public default void setAuxiliaryType(int auxType)               { }
    public default void setCreationDate(Date date)                  { }
    // ProdosFileEntry / PascalFileEntry specific
    public default void setLastModificationDate(Date date)          { }
    
    public static FileEntryWriter get(FileEntry fileEntry) {
        if (fileEntry instanceof DosFileEntry) {
            return new DosFileEntryReaderWriter((DosFileEntry)fileEntry);
        } 
        else if (fileEntry instanceof ProdosFileEntry) {
            return new ProdosFileEntryReaderWriter((ProdosFileEntry)fileEntry);
        }
        else if (fileEntry instanceof PascalFileEntry) {
            return new PascalFileEntryReaderWriter((PascalFileEntry)fileEntry);
        }
        throw new RuntimeException(String.format("No writer for %s", fileEntry.getClass().getName()));
    }
}
