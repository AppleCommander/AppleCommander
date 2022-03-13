/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2021-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.storage.compare;

import java.util.ArrayList;
import java.util.List;

public class ComparisonResult {
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    public int getDifferenceCount() {
        return errors.size() + warnings.size();
    }
    
    public void addError(Exception ex) {
        errors.add(ex.getMessage());
    }
    public void addError(String fmt, Object... args) {
        errors.add(String.format(fmt, args));
    }
    public void addWarning(String fmt, Object... args) {
        warnings.add(String.format(fmt,  args));
    }
    
    public List<String> getErrors() {
        return errors;
    }
    public List<String> getWarnings() {
        return warnings;
    }
}
