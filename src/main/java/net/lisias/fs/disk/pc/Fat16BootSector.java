/*
 * Copyright (C) 2009-2013 Matthias Treydte <mt@waldheinz.de>
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

package net.lisias.fs.disk.pc;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.fat.BootSector;
import de.waldheinz.fs.fat.FatType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The boot sector layout as used by the FAT12 / FAT16 variants.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
final class Fat16BootSector extends BootSector {

    /**
     * The default number of entries for the root directory.
     * 
     * @see #getRootDirEntryCount()
     * @see #setRootDirEntryCount(int) 
     */
    public static final int DEFAULT_ROOT_DIR_ENTRY_COUNT = 512;

    /**
     * The default volume label.
     */
    public static final String DEFAULT_VOLUME_LABEL = "NO NAME"; //NOI18N
    
    /**
     * The maximum number of clusters for a FAT12 file system. This is actually
     * the number of clusters where mkdosfs stop complaining about a FAT16
     * partition having not enough sectors, so it would be misinterpreted
     * as FAT12 without special handling.
     *
     * @see #getNrLogicalSectors()
     */
    public static final int MAX_FAT12_CLUSTERS = 4084;

    public static final int MAX_FAT16_CLUSTERS = 65524;

    public static final int TOTAL_SECTORS_16_OFFSET = 19;

    /**
     * The offset to the sectors per FAT value.
     */
    public static final int SECTORS_PER_FAT_OFFSET = 0x16;

    /**
     * The offset to the root directory entry count value.
     *
     * @see #getRootDirEntryCount()
     * @see #setRootDirEntryCount(int) 
     */
    public static final int ROOT_DIR_ENTRIES_OFFSET = 0x11;

    /**
     * The offset to the first byte of the volume label.
     */
    public static final int VOLUME_LABEL_OFFSET = 0x2b;
    
    /**
     * Offset to the FAT file system type string.
     *
     * @see #getFileSystemType() 
     */
    public static final int FILE_SYSTEM_TYPE_OFFSET = 0x36;
    
    /**
     * The maximum length of the volume label.
     */
    public static final int MAX_VOLUME_LABEL_LENGTH = 11;
    
    public static final int EXTENDED_BOOT_SIGNATURE_OFFSET = 0x26;

    /**
     * Creates a new {@code Fat16BootSector} for the specified device.
     *
     * @param device the {@code BlockDevice} holding the boot sector
     * @throws IOException 
     */
    public Fat16BootSector(BlockDevice device) throws IOException {
        super(device);
        
        final ByteBuffer bb = ByteBuffer.allocate(SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        device.read(0, bb);
        
        if ((bb.get(510) & 0xff) != 0x55 || (bb.get(511) & 0xff) != 0xaa) 
        	throw new IOException("missing boot sector signature");
                
        final byte sectorsPerCluster = bb.get(SECTORS_PER_CLUSTER_OFFSET);

        if (sectorsPerCluster <= 0) throw new IOException("suspicious sectors per cluster count " + sectorsPerCluster);
                
        final int rootDirEntries = bb.getShort(Fat16BootSector.ROOT_DIR_ENTRIES_OFFSET);
        final int rootDirSectors = ((rootDirEntries * 32) + (device.getSectorSize() - 1)) / device.getSectorSize();

        final long totalSectors = (bb.getShort(TOTAL_SECTORS_16_OFFSET) & 0xffff);
        if (0 == totalSectors)
        	throw new IOException("Total sectors is zero. Not a FAT16!");
        
        final int fatSz = bb.getShort(SECTORS_PER_FAT_OFFSET)  & 0xffff;
        if (0 == fatSz)
        	throw new IOException("FAT size is zero. Not a FAT16!");
                
        final int reservedSectors = bb.getShort(RESERVED_SECTORS_OFFSET);
        final int fatCount = bb.get(FAT_COUNT_OFFSET);
        final long dataSectors = totalSectors - (reservedSectors + (fatCount * fatSz) + rootDirSectors);
                
        final long clusterCount = dataSectors / sectorsPerCluster;
        if (clusterCount > MAX_FAT16_CLUSTERS)
        	throw new IOException("Cluster count too big. Not a FAT16!");
    }
    
    /**
     * Returns the volume label that is stored in this boot sector.
     *
     * @return the volume label
     */
    @Override
    public String getVolumeLabel() {
        final StringBuilder sb = new StringBuilder();

        for (int i=0; i < MAX_VOLUME_LABEL_LENGTH; i++) {
            final char c = (char) get8(VOLUME_LABEL_OFFSET + i);

            if (c != 0) {
                sb.append(c);
            } else {
                break;
            }
        }
        
        return sb.toString();
    }

    /**
     * Sets the volume label that is stored in this boot sector.
     *
     * @param label the new volume label
     * @throws IllegalArgumentException if the specified label is longer
     *      than {@link #MAX_VOLUME_LABEL_LENGTH}
     */
    @Override
    public void setVolumeLabel(String label) throws IllegalArgumentException {
        if (label.length() > MAX_VOLUME_LABEL_LENGTH)
            throw new IllegalArgumentException("volume label too long");

        for (int i = 0; i < MAX_VOLUME_LABEL_LENGTH; i++) {
            set8(VOLUME_LABEL_OFFSET + i,
                    i < label.length() ? label.charAt(i) : 0);
        }
    }
    
    /**
     * Gets the number of sectors/fat for FAT 12/16.
     *
     * @return int
     */
    @Override
    public long getSectorsPerFat() {
        return get16(SECTORS_PER_FAT_OFFSET);
    }

    /**
     * Sets the number of sectors/fat
     *
     * @param v  the new number of sectors per fat
     */
    @Override
    public void setSectorsPerFat(long v) {
        if (v == getSectorsPerFat()) return;
        if (v > 0x7FFF) throw new IllegalArgumentException(
                "too many sectors for a FAT12/16");
        
        set16(SECTORS_PER_FAT_OFFSET, (int)v);
    }

    @Override
    public FatType getFatType() {
        final long rootDirSectors = ((getRootDirEntryCount() * 32) +
                (getBytesPerSector() - 1)) / getBytesPerSector();
        final long dataSectors = getSectorCount() -
                (getNrReservedSectors() + (getNrFats() * getSectorsPerFat()) +
                rootDirSectors);
        final long clusterCount = dataSectors / getSectorsPerCluster();
        
        if (clusterCount > MAX_FAT16_CLUSTERS) throw new IllegalStateException(
                "too many clusters for FAT12/16: " + clusterCount);
        
        return clusterCount > MAX_FAT12_CLUSTERS ?
            FatType.FAT16 : FatType.FAT12;
    }
    
    @Override
    public void setSectorCount(long count) {
        if (count > 65535) {
            setNrLogicalSectors(0);
            setNrTotalSectors(count);
        } else {
            setNrLogicalSectors((int) count);
            setNrTotalSectors(count);
        }
    }
    
    @Override
    public long getSectorCount() {
        if (getNrLogicalSectors() == 0) return getNrTotalSectors();
        else return getNrLogicalSectors();
    }
    
    /**
     * Gets the number of entries in the root directory.
     *
     * @return int the root directory entry count
     */
    @Override
    public int getRootDirEntryCount() {
        return get16(ROOT_DIR_ENTRIES_OFFSET);
    }
    
    /**
     * Sets the number of entries in the root directory
     *
     * @param v the new number of entries in the root directory
     * @throws IllegalArgumentException for negative values
     */
    public void setRootDirEntryCount(int v) throws IllegalArgumentException {
        if (v < 0) throw new IllegalArgumentException();
        if (v == getRootDirEntryCount()) return;
        
        set16(ROOT_DIR_ENTRIES_OFFSET, v);
    }
    
    @Override
    public void init() throws IOException {
        super.init();
        
        setRootDirEntryCount(DEFAULT_ROOT_DIR_ENTRY_COUNT);
        setVolumeLabel(DEFAULT_VOLUME_LABEL);
    }

    @Override
    public int getFileSystemTypeLabelOffset() {
        return FILE_SYSTEM_TYPE_OFFSET;
    }

    @Override
    public int getExtendedBootSignatureOffset() {
        return EXTENDED_BOOT_SIGNATURE_OFFSET;
    }
    
    /**
     * Gets the number of logical sectors
     * 
     * @return int
     */
    protected int getNrLogicalSectors() {
        return get16(TOTAL_SECTORS_16_OFFSET);
    }
    
    /**
     * Sets the number of logical sectors
     * 
     * @param v the new number of logical sectors
     */
    protected void setNrLogicalSectors(int v) {
        if (v == getNrLogicalSectors()) return;
        
        set16(TOTAL_SECTORS_16_OFFSET, v);
    }
    
    protected void setNrTotalSectors(long v) {
        set32(TOTAL_SECTORS_16_OFFSET, v);
    }
    
    protected long getNrTotalSectors() {
        return get32(TOTAL_SECTORS_16_OFFSET);
    }

	@Override
	public void setRootDirFirstCluster(final long startCluster) {
		throw new RuntimeException("Invalid Call");
	}

	@Override
	public int getFsInfoSectorNr() {
		throw new RuntimeException("Invalid Call");
	}

	@Override
	public long getRootDirFirstCluster() {
		throw new RuntimeException("Invalid Call");
	}
}
