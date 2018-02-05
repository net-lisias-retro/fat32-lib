/*
 * Copyright (C) 2003-2009 JNode.org
 *               2009-2013 Matthias Treydte <mt@waldheinz.de>
 * 				 2018 Lisias T <support@lisias.net>
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
 
package de.waldheinz.fs.fat;

import de.waldheinz.fs.AbstractFileSystem;
import de.waldheinz.fs.BlockDevice;
import java.io.IOException;
import de.waldheinz.fs.ReadOnlyException;
import net.lisias.fs.disk.SuperFloppyFormatter;

/**
 * <p>
 * Implements the {@code FileSystem} interface for the FAT family of file
 * systems. This class always uses the "long file name" specification when
 * writing directory entries.
 * </p><p>
 * For creating (aka "formatting") FAT file systems please refer to the
 * {@link SuperFloppyFormatter} class.
 * </p>
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public abstract class FatFileSystem extends AbstractFileSystem {
    
    protected final BootSector bs;
    private final Fat fat;
    private final FsInfoSector fsiSector;
    private final FatLfnDirectory rootDir;
    private final AbstractDirectory rootDirStore;
    private final FatType fatType;
    private final long filesOffset;

    protected FatFileSystem(final BlockDevice device, final BootSector bs, final boolean readOnly) throws IOException {
        this(device, bs, readOnly, false);
    }
    
    /**
     * Constructor for FatFileSystem in specified readOnly mode
     * 
     * @param device the {@code BlockDevice} holding the file system
     * @param readOnly if this FS should be read-lonly
     * @param ignoreFatDifferences
     * @throws IOException on read error
     */
    private FatFileSystem(final BlockDevice device, final BootSector bs, final boolean readOnly, final boolean ignoreFatDifferences)
            throws IOException {
        
        super(readOnly);
        
        this.bs = bs;
        this.bs.read();
        
        if (bs.getNrFats() <= 0) throw new IOException(
                "boot sector says there are no FATs");
        
        this.filesOffset = bs.getFilesOffset();
        this.fatType = bs.getFatType();
        this.fat = Fat.read(bs, 0);

        if (!ignoreFatDifferences) {
            for (int i=1; i < bs.getNrFats(); i++) {
                final Fat tmpFat = Fat.read(bs, i);
                if (!fat.equals(tmpFat)) {
                    throw new IOException("FAT " + i + " differs from FAT 0");
                }
            }
        }
        
        if (fatType == FatType.FAT32) {
            final ClusterChain rootChain = new ClusterChain(fat, bs.getRootDirFirstCluster(), isReadOnly());
            this.rootDirStore = ClusterChainDirectory.readRoot(rootChain);
            this.fsiSector = FsInfoSector.read(bs);
            
            if (fsiSector.getFreeClusterCount() < fat.getFreeClusterCount()) {
                throw new IOException("free cluster count mismatch - fat: " +
                        fat.getFreeClusterCount() + " - fsinfo: " +
                        fsiSector.getFreeClusterCount());
            }
        } else {
            this.rootDirStore = Fat16RootDirectory.read(bs,readOnly);
            this.fsiSector = null;
        }

        this.rootDir = new FatLfnDirectory(rootDirStore, fat, isReadOnly());
            
    }

    long getFilesOffset() {
        checkClosed();
        
        return filesOffset;
    }

    /**
     * Returns the size of the FAT entries of this {@code FatFileSystem}.
     *
     * @return the exact type of the FAT used by this file system
     */
    public FatType getFatType() {
        checkClosed();

        return this.fatType;
    }

    /**
     * Returns the volume label of this file system.
     *
     * @return the volume label
     */
    public String getVolumeLabel() {
        checkClosed();
        
        final String fromDir = rootDirStore.getLabel();
        
        if (fromDir == null && fatType != FatType.FAT32) {
            return bs.getVolumeLabel();
        } else {
            return fromDir;
        }
    }
    
    /**
     * Sets the volume label for this file system.
     *
     * @param label the new volume label, may be {@code null}
     * @throws ReadOnlyException if the file system is read-only
     * @throws IOException on write error
     */
    public void setVolumeLabel(String label)
            throws ReadOnlyException, IOException {
        
        checkClosed();
        checkReadOnly();

        rootDirStore.setLabel(label);
        
        if (fatType != FatType.FAT32) {
            bs.setVolumeLabel(label);
        }
    }

    AbstractDirectory getRootDirStore() {
        checkClosed();
        
        return rootDirStore;
    }
    
    /**
     * Flush all changed structures to the device.
     * 
     * @throws IOException on write error
     */
    @Override
    public void flush() throws IOException {
        checkClosed();
        
        if (bs.isDirty()) {
            bs.write();
        }
        
        for (int i = 0; i < bs.getNrFats(); i++) {
            fat.writeCopy(bs.getFatOffset(i));
        }
        
        rootDir.flush();
        
        if (fsiSector != null) {
            fsiSector.setFreeClusterCount(fat.getFreeClusterCount());
            fsiSector.setLastAllocatedCluster(fat.getLastAllocatedCluster());
            fsiSector.write();
        }
    }
    
    @Override
    public FatLfnDirectory getRoot() {
        checkClosed();
        
        return rootDir;
    }
    
    /**
     * Returns the fat.
     * 
     * @return Fat
     */
    Fat getFat() {
        return fat;
    }

    /**
     * Returns the bootsector.
     * 
     * @return BootSector
     */
    BootSector getBootSector() {
        checkClosed();
        
        return bs;
    }

    /**
     * The free space of this file system.
     *
     * @return if -1 this feature is unsupported
     */
    @Override
    public long getFreeSpace() {
        checkClosed();

        return fat.getFreeClusterCount() * bs.getBytesPerCluster();
    }

    /**
     * The total size of this file system.
     *
     * @return if -1 this feature is unsupported
     */
    @Override
    public long getTotalSpace() {
        checkClosed();

        if (fatType == FatType.FAT32) {
            return bs.getSectorCount() * bs.getBytesPerSector();
        }

        return -1;
    }

    /**
     * The usable space of this file system.
     *
     * @return if -1 this feature is unsupported
     */
    @Override
    public long getUsableSpace() {
        checkClosed();

        return bs.getDataClusterCount() * bs.getBytesPerCluster();
    }
}
