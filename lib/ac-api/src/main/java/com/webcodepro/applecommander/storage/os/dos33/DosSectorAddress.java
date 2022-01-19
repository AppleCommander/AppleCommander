/*
 * AppleCommander - An Apple ][ image utility.
 * Copyright (C) 2002-2022 by Robert Greene
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
package com.webcodepro.applecommander.storage.os.dos33;

/**
 * A Container for DOS 3.3 Sector Addresses.
 * <br>
 * Created on Dec 13, 2017.
 * @author Lisias Toledo
 */
public class DosSectorAddress {

	public final Integer track;
	public final Integer sector;

	public DosSectorAddress(int track, int sector) {
		this.track = track;
		this.sector = sector;
	}

	public String toString() {
		return "Track:" + this.track + ", Sector:" + this.sector;
	}

	public boolean equals(final Object other){
		if(other == null)							return false;
		if(!(other instanceof DosSectorAddress))	return false;

		final DosSectorAddress o = (DosSectorAddress) other;
		return this.track == o.track && this.sector == o.sector;
	}

    public int hashCode() {
        int result = 17;
        result = 31 * result + this.track.hashCode();
        result = 31 * result + this.sector.hashCode();
        return result;
    }
}
