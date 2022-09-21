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

package net.lisias.fs.disk.atari;

import java.io.IOException;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.fat.BootSector;

public class Tos extends de.waldheinz.fs.fat.FatFileSystem {

	Tos(final BlockDevice device, final BootSector bs, final boolean readOnly) throws IOException {
		super(device, bs, readOnly);
	}

	public static FileSystem read(final BlockDevice device, final boolean readOnly) throws IOException {
    	return new Tos(device, new TosBootSector(device), readOnly);
    }

}
