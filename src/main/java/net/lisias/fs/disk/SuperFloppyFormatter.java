/*
 * Copyright (C) 2018-2022 Lisias T <me@lisias.net>
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.lisias.fs.disk;

import java.io.IOException;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.fat.FatType;
import net.lisias.fs.disk.pc.FloppyFormatter;
import de.waldheinz.fs.fat.BootSector;
import de.waldheinz.fs.fat.FatFileSystem;

/**
 * Allows to create FAT file systems on {@link BlockDevice}s which follow the
 * "super floppy" standard. This means that the device will be formatted so
 * that it does not contain a partition table. Instead, the entire device holds
 * a single FAT file system.
 * 
 * This class follows the "builder" pattern, which means it's methods always
 * returns the {@code SuperFloppyFormatter} instance they're called on. This
 * allows to chain the method calls like this:
 * <pre>
 *  BlockDevice dev = new RamDisk(16700000);
 *  FatFileSystem fs = SuperFloppyFormatter.get(dev).
 *          setFatType(FatType.FAT12).format();
 * </pre>
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public abstract class SuperFloppyFormatter {
	public enum eSystem {
		atari,
		msx,
		pc;
	}
	
    /**
     * Retruns a {@code SuperFloppyFormatter} instance suitable for formatting
     * the specified device.
     *
     * @param dev the device that should be formatted
     * @return the formatter for the device
     * @throws IOException on error creating the formatter
     */
    public static SuperFloppyFormatter get(final eSystem system, final BlockDevice dev) throws IOException {
        switch(system) {
        	case atari: throw new IOException("Not implemented yet!");
        	case msx: return new net.lisias.fs.disk.msx.FloppyFormatter(dev);
        	case pc: return new net.lisias.fs.disk.pc.FloppyFormatter(dev);
        }
    	throw new IOException("Invalid system!");
    }

	public abstract SuperFloppyFormatter setFatType(final FatType fatType) throws IOException, IllegalArgumentException;
	public abstract SuperFloppyFormatter setVolumeLabel(final String label);
	public abstract FatFileSystem format() throws IOException;

	protected abstract void initBootSector(final BootSector bs) throws IOException;
}