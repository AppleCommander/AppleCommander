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
package io.github.applecommander.acx.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.github.applecommander.acx.FilterMethod;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

public class FilterMethodConverter implements ITypeConverter<FilterMethod> {
    public static final Map<String,FilterMethod> FILTERS = new HashMap<>();
    static {
        for (FilterMethod x : FilterMethod.values()) {
            for (String code : x.getCodes()) {
                FILTERS.put(code, x);
            }
        }
    }
    
    @Override
    public FilterMethod convert(String value) throws Exception {
        if (FILTERS.containsKey(value)) {
            return FILTERS.get(value);
        }
        throw new TypeConversionException(String.format("Export method not found: %s", value));
    }
    
    public static class FilterMethodCandidates extends ArrayList<String> {
        private static final long serialVersionUID = -744232190636905235L;

        FilterMethodCandidates() {
            super(FILTERS.keySet());
            Collections.sort(this);
        }
    }
}
