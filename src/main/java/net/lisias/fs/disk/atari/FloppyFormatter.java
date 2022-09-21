/*
 * Copyright (C) 2009-2013 Matthias Treydte <mt@waldheinz.de>
 * 				 2018-2022 Lisias T <me@lisias.net>
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

package net.lisias.fs.disk.atari;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.fat.FatType;

import java.io.IOException;

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
public final class FloppyFormatter extends net.lisias.fs.disk.pc.FloppyFormatter {

    public FloppyFormatter(final BlockDevice device) throws IOException {
		super(device);
	}

	protected FatType fatTypeFromDevice() throws IOException {
    	final FatType result = super.fatTypeFromDevice();
        if (FatType.FAT32 == result) throw new IOException("Not supported");
        return result;
    }
    
    @Override
    public net.lisias.fs.disk.pc.FloppyFormatter setFatType(final FatType fatType)
            throws IOException, IllegalArgumentException {
        
        if (null == fatType) throw new NullPointerException();
        if (FatType.FAT32 == fatType) throw new IOException("Not supported");

        return super.setFatType(fatType);
    }
    
    @Override
    protected int defaultSectorsPerCluster(FatType fatType) throws IOException {
    	if (FatType.FAT32 == fatType) throw new IOException("Not supported");
    	return super.defaultSectorsPerCluster(fatType);
    }
}
