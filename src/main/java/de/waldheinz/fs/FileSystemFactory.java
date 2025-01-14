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
 
package de.waldheinz.fs;

import net.lisias.fs.disk.atari.Tos;
import net.lisias.fs.disk.msx.MsxDos;
import net.lisias.fs.disk.pc.MsDos;

import java.io.IOException;

/**
 * Factory for {@link FileSystem} instances.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FileSystemFactory {
    
    private FileSystemFactory() { }
    
    /**
     * <p>
     * Creates a new {@link FileSystem} for the specified {@code device}. When
     * using this method, care must be taken that there is only one
     * {@code FileSystems} accessing the specified {@link BlockDevice}.
     * Otherwise severe file system corruption may occur.
     * </p>
     *
     * @param device the device to create the file system for
     * @param readOnly if the file system should be openend read-only
     * @return a new {@code FileSystem} instance for the specified device
     * @throws IOException on read error, or if the file system type could
     *      not be determined
     */
    public static FileSystem create(final BlockDevice device, final boolean readOnly) throws IOException {
        try {    
        	return MsDos.read(device, readOnly);
        } catch (IOException e0) { try {
        	return MsxDos.read(device, readOnly);
        } catch (IOException e1) { try {
        	return Tos.read(device, readOnly);
        } catch (IOException e2) { 
        	throw new IOException("Disk format not recognized!");
        } } }
    }
    
}
