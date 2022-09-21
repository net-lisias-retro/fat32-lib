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

package net.lisias.fs.disk.pc;

import java.io.IOException;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.fat.BootSector;
import de.waldheinz.fs.fat.FatFileSystem;
import net.lisias.fs.disk.pc.Fat16BootSector;
import net.lisias.fs.disk.pc.Fat32BootSector;

public final class MsDos extends de.waldheinz.fs.fat.FatFileSystem {

    MsDos(final BlockDevice device, final BootSector bs, final boolean readOnly) throws IOException {
		super(device, bs, readOnly);
	}

    /**
     * Reads the file system structure from the specified {@code BlockDevice}
     * and returns a fresh {@code FatFileSystem} instance to read or modify
     * it.
     *
     * @param device the {@code BlockDevice} holding the file system
     * @param readOnly if the {@code FatFileSystem} should be in read-only mode
     * @return the {@code FatFileSystem} instance for the device
     * @throws IOException on read error or if the file system structure could
     *      not be parsed
     */
    public static FatFileSystem read(final BlockDevice device, final boolean readOnly) throws IOException {
        try {
        	return new MsDos(device, new Fat16BootSector(device), readOnly);
        } catch (final IOException e) { 
        	return new MsDos(device, new Fat32BootSector(device), readOnly);
        }
    }
}