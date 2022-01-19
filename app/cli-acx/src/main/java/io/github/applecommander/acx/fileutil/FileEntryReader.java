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
import java.util.Optional;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.os.dos33.DosFileEntry;
import com.webcodepro.applecommander.storage.os.nakedos.NakedosFileEntry;
import com.webcodepro.applecommander.storage.os.pascal.PascalFileEntry;
import com.webcodepro.applecommander.storage.os.prodos.ProdosFileEntry;
import com.webcodepro.applecommander.storage.os.rdos.RdosFileEntry;

public interface FileEntryReader {
    //  FileEntry common
    public default Optional<String> getFilename()                   { return Optional.empty(); }
    public default Optional<String> getProdosFiletype()             { return Optional.empty(); }
    public default Optional<Boolean> isLocked()                     { return Optional.empty(); }
    public default Optional<byte[]> getFileData()                   { return Optional.empty(); }
    public default Optional<byte[]> getResourceData()               { return Optional.empty(); }
    /** 
     * The address embedded in binary objects. 
     * This varies by DOS's so is split apart. 
     */
    public default Optional<Integer> getBinaryAddress()             { return Optional.empty(); }
    /** 
     * The length embedded in binary, Applesoft, Integer BASIC objects. 
     * This varies by DOS's so is split apart. 
     */
    public default Optional<Integer> getBinaryLength()              { return Optional.empty(); }
    // ProdosFileEntry specific
    public default Optional<Integer> getAuxiliaryType()             { return Optional.empty(); }
    public default Optional<Date> getCreationDate()                 { return Optional.empty(); }
    // ProdosFileEntry / PascalFileEntry specific
    public default Optional<Date> getLastModificationDate()         { return Optional.empty(); }
    
    public static FileEntryReader get(FileEntry fileEntry) {
        if (fileEntry instanceof DosFileEntry) {
            return new DosFileEntryReaderWriter((DosFileEntry)fileEntry);
        }
        else if (fileEntry instanceof NakedosFileEntry) {
            return new NakedosFileEntryReader((NakedosFileEntry)fileEntry);
        }
        else if (fileEntry instanceof PascalFileEntry) {
            return new PascalFileEntryReaderWriter((PascalFileEntry)fileEntry);
        }
        else if (fileEntry instanceof ProdosFileEntry) {
            return new ProdosFileEntryReaderWriter((ProdosFileEntry)fileEntry);
        }
        else if (fileEntry instanceof RdosFileEntry) {
            return new RdosFileEntryReader((RdosFileEntry)fileEntry);
        }
        throw new RuntimeException(String.format("No reader for %s", fileEntry.getClass().getName()));
    }
}
