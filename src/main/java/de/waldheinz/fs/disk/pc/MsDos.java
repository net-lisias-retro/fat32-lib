package de.waldheinz.fs.disk.pc;

import java.io.IOException;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FileSystem;
import de.waldheinz.fs.fat.BootSector;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.disk.pc.Fat16BootSector;
import de.waldheinz.fs.disk.pc.Fat32BootSector;

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