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
import java.util.function.Supplier;

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

public enum FilterMethod {
    APPLESINGLE(AppleSingleFileFilter::new, "as", "applesingle"),
    APPLESOFT(ApplesoftFileFilter::new, "bas", "applesoft"),
    APPLEWORKS_DATABASE(AppleWorksDataBaseFileFilter::new, "adb"),
    APPLEWORKS_SPREADSHEET(AppleWorksSpreadSheetFileFilter::new, "asp"),
    APPLEWORKS_WORDPROCESSOR(AppleWorksWordProcessorFileFilter::new, "awp"),
    ASSEMBLY_SOURCE(AssemblySourceFileFilter::new, "asm", "assembly"),
    BINARY(BinaryFileFilter::new, "bin", "binary"),
    BUSINESS_BASIC(BusinessBASICFileFilter::new, "bbas", "business-basic"),
    DISASSEMBLY(DisassemblyFileFilter::new, "disasm", "disassembly"),
    GRAPHICS(GraphicsFileFilter::new, "gr", "graphics"),
    GUTENBERG_FILE(GutenbergFileFilter::new, "gutenberg"),
    HEX_DUMP(HexDumpFileFilter::new, "hex"),
    INTEGER_BASIC(IntegerBasicFileFilter::new, "int", "integer"),
    PASCAL_TEXT(PascalTextFileFilter::new, "ptext", "pascal-text"),
    RAW(RawFileFilter::new, "raw"),
    TEXT(TextFileFilter::new, "text");
    
    private Supplier<FileFilter> constructor;
    private List<String> codes;
    
    private FilterMethod(Supplier<FileFilter> constructor, String... codes) {
        this.constructor = constructor;
        this.codes = Arrays.asList(codes);
    }
    
    public FileFilter create() {
        return constructor.get();
    }
    public List<String> getCodes() {
        return codes;
    }
}