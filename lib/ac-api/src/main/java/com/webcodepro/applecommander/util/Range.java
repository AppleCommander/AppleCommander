/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene and others
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
package com.webcodepro.applecommander.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  Represents a range of numbers with helper methods to put them together. 
 */
public class Range {
    private int first;
    private int last;
    
    public Range(int first, int last) {
        if (first < last) {
            this.first = first;
            this.last = last;
        }
        else {
            this.first = last;
            this.last = first;
        }
    }
    
    public int getFirst() {
        return first;
    }
    public int getLast() {
        return last;
    }
    public int size() {
        return last - first + 1;
    }
    
    @Override
    public String toString() {
        if (first == last) {
            return String.format("%d", first);
        }
        else {
            return String.format("%d-%d", first, last);
        }
    }

    public static List<Range> from(List<Integer> numbers) {
        List<Range> ranges = new ArrayList<>();
        Collections.sort(numbers);
        
        int first = -1;
        int last = -1;
        for (int number : numbers) {
            if (first == -1) {
                first = last = number;
            }
            else if (number == last+1) {
                last = number;
            }
            else {
                ranges.add(new Range(first, last));
                first = last = number;
            }
        }

        if (first != -1) {
            ranges.add(new Range(first, last));
        }
        
        return ranges;
    }
}
