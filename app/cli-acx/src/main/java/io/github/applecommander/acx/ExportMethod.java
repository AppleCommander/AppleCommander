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
package io.github.applecommander.acx;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.webcodepro.applecommander.storage.FileEntry;
import com.webcodepro.applecommander.storage.FileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksDataBaseFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksSpreadSheetFileFilter;
import com.webcodepro.applecommander.storage.filters.AppleWorksWordProcessorFileFilter;
import com.webcodepro.applecommander.storage.filters.ApplesoftFileFilter;
import com.webcodepro.applecommander.storage.filters.AssemblySourceFileFilter;
import com.webcodepro.applecommander.storage.filters.BinaryFileFilter;
import com.webcodepro.applecommander.storage.filters.BusinessBASICFileFilter;
import com.webcodepro.applecommander.storage.filters.DisassemblyFileFilter;
import com.webcodepro.applecommander.storage.filters.GraphicsFileFilter;
import com.webcodepro.applecommander.storage.filters.GutenbergFileFilter;
import com.webcodepro.applecommander.storage.filters.HexDumpFileFilter;
import com.webcodepro.applecommander.storage.filters.IntegerBasicFileFilter;
import com.webcodepro.applecommander.storage.filters.PascalTextFileFilter;
import com.webcodepro.applecommander.storage.filters.TextFileFilter;

import io.github.applecommander.filters.AppleSingleFileFilter;
import io.github.applecommander.filters.RawFileFilter;

public enum ExportMethod {
    APPLESINGLE(ignored -> new AppleSingleFileFilter(), "as", "applesingle"),
    APPLESOFT(ignored -> new ApplesoftFileFilter(), "bas", "applesoft"),
    APPLEWORKS_DATABASE(ignored -> new AppleWorksDataBaseFileFilter(), "adb"),
    APPLEWORKS_SPREADSHEET(ignored -> new AppleWorksSpreadSheetFileFilter(), "asp"),
    APPLEWORKS_WORDPROCESSOR(ignored -> new AppleWorksWordProcessorFileFilter(), "awp"),
    ASSEMBLY_SOURCE(ignored -> new AssemblySourceFileFilter(), "asm", "assembly"),
    BINARY(ignored -> new BinaryFileFilter(), "bin", "binary"),
    BUSINESS_BASIC(ignored -> new BusinessBASICFileFilter(), "bbas", "business-basic"),
    DISASSEMBLY(fileEntry -> new DisassemblyFileFilter(fileEntry), "disasm", "disassembly"),
    GRAPHICS(ignored -> new GraphicsFileFilter(), "gr", "graphics"),
    GUTENBERG_FILE(ignored -> new GutenbergFileFilter(), "gutenberg"),
    HEX_DUMP(ignored -> new HexDumpFileFilter(), "hex"),
    INTEGER_BASIC(ignored -> new IntegerBasicFileFilter(), "int", "integer"),
    PASCAL_TEXT(ignored -> new PascalTextFileFilter(), "ptext", "pascal-text"),
    RAW(ignored -> new RawFileFilter(), "raw"),
    TEXT(ignored -> new TextFileFilter(), "text");
    
    private final Function<FileEntry,FileFilter> fileFilterFn;
    private final List<String> codes;
    
    ExportMethod(Function<FileEntry, FileFilter> fileFilterFn, String... codes) {
        this.fileFilterFn = fileFilterFn;
        this.codes = Arrays.asList(codes);
    }
    
    public FileFilter create(FileEntry fileEntry) {
        return fileFilterFn.apply(fileEntry);
    }
    public List<String> getCodes() {
        return codes;
    }
}