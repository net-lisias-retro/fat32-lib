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

package net.lisias.fs.disk.pc;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.fat.BootSector;
import de.waldheinz.fs.fat.FatType;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The boot sector layout as used by the FAT12 / FAT16 variants.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class Fat16BootSector extends BootSector {

	public enum OFFSET_EXT implements BootSector.Offset {
		EXTENDED_BOOT_SIGNATURE(0x26),	// ???
		SERIAL_NUMBER(0x27),			// Random generated 32bits number
		VOLUME_LABEL(0x2b),				// (MAX_VOLUME_LABEL_LENGTH) chars
		FILE_SYSTEM_TYPE(0x36),			// ???
		;
			
		private final byte o;

		OFFSET_EXT(final int value) {
			this.o = (byte)value;
		}

		@Override
		public int offset() {
			return this.o;
		}
	}

	/**
     * The default number of entries for the root directory.
     * 
     * @see #getRootDirEntryCount()
     * @see #setRootDirEntryCount(int) 
     */
    public static final int DEFAULT_ROOT_DIR_ENTRY_COUNT = 512; // FIXME: Should be 112 or 64 for floppies!!

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
    
    /**
     * The maximum length of the volume label.
     */
    public static final int MAX_VOLUME_LABEL_LENGTH = 11;
    
    /**
     * Creates a new {@code Fat16BootSector} for the specified device.
     *
     * @param device the {@code BlockDevice} holding the boot sector
     * @throws IOException 
     */
    public Fat16BootSector(final BlockDevice device) throws IOException {
        super(device);
    }
    
    @Override
    protected void checkDisk() throws IOException {
        final ByteBuffer bb = this.buffer;
        
        if ((bb.get(510) & 0xff) != 0x55 || (bb.get(511) & 0xff) != 0xaa) 
        	throw new IOException("missing boot sector signature");
                
        final byte sectorsPerCluster = bb.get(OFFSET.SECTORS_PER_CLUSTER.offset());

        if (sectorsPerCluster <= 0) throw new IOException("suspicious sectors per cluster count " + sectorsPerCluster);
                
        final int rootDirEntries = bb.getShort(OFFSET.ROOT_DIR_ENTRIES.offset());
        final int rootDirSectors = ((rootDirEntries * 32) + (this.getSectorSize() - 1)) / this.getSectorSize();

        final long totalSectors = (bb.getShort(OFFSET.TOTAL_SECTORS_MEDIA.offset()) & 0xffff);
        if (0 == totalSectors)
        	throw new IOException("Total sectors is zero. Not a FAT16!");
        
        final int fatSz = bb.getShort(OFFSET.NUMBER_SECTORS_FAT.offset())  & 0xffff;
        if (0 == fatSz)
        	throw new IOException("FAT size is zero. Not a FAT16!");
                
        final int reservedSectors = bb.getShort(OFFSET.NUMBER_RESERVED_SECTORS.offset());
        final int fatCount = bb.get(OFFSET.NUMBER_OF_FATS.offset());
        final long dataSectors = totalSectors - (reservedSectors + (fatCount * fatSz) + rootDirSectors);
                
        final long clusterCount = dataSectors / sectorsPerCluster;
        if (clusterCount > MAX_FAT16_CLUSTERS)
        	throw new IOException("Cluster count too big. Not a FAT16!");
    }
    
    @Override
    public void init() throws IOException {
    	super.init();
    	
    	/* magic bytes needed by some windows versions to recognize a boot
         * sector. these are x86 jump instructions which lead into
         * nirvana when executed, but we're currently unable to produce really
         * bootable images anyway. So... */
        set8(OFFSET.BRANCH_S, 0xeb);
        set16(1 + OFFSET.BRANCH_S.offset(), 0x903c);

        setRootDirEntryCount(DEFAULT_ROOT_DIR_ENTRY_COUNT);
        setVolumeLabel(DEFAULT_VOLUME_LABEL);
        
        set32(OFFSET_EXT.SERIAL_NUMBER, random.nextLong() & 0xffffffff);

        // Halts Everything. I need to fix the root dir entries count for floppies!
        throw new IOException("Not fixed yet!");
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
            final char c = (char) get8(OFFSET_EXT.VOLUME_LABEL.offset() + i);

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
            set8(OFFSET_EXT.VOLUME_LABEL.offset() + i,
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
        return get16(OFFSET.NUMBER_SECTORS_FAT);
    }

    /**
     * Sets the number of sectors/fat
     *
     * @param v  the new number of sectors per fat
     */
    @Override
    public void setSectorsPerFat(long v) {
        if (v == getSectorsPerFat()) return;
        if (v > 0x7FFF)
        	throw new IllegalArgumentException("too many sectors for a FAT12/16");
        
        set16(OFFSET.NUMBER_SECTORS_FAT, (int)v);
    }

    @Override
    public FatType getFatType() {
        final long rootDirSectors = ((getRootDirEntryCount() * 32) + (getBytesPerSector() - 1)) / getBytesPerSector();
        final long dataSectors = getSectorCount() - (getNrReservedSectors() + (getNrFats() * getSectorsPerFat()) + rootDirSectors);
        final long clusterCount = dataSectors / getSectorsPerCluster();
        
        if (clusterCount > MAX_FAT16_CLUSTERS) 
        	throw new IllegalStateException("too many clusters for FAT12/16: " + clusterCount);
        
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
        return get16(OFFSET.ROOT_DIR_ENTRIES);
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
        
        set16(OFFSET.ROOT_DIR_ENTRIES, v);
    }
    
    @Override
    public BootSector.Offset getFileSystemTypeLabelOffset() {
        return OFFSET_EXT.FILE_SYSTEM_TYPE;
    }

    @Override
    public BootSector.Offset getExtendedBootSignatureOffset() {
        return OFFSET_EXT.EXTENDED_BOOT_SIGNATURE;
    }
    
    /**
     * Gets the number of logical sectors
     * 
     * @return int
     */
    protected int getNrLogicalSectors() {
        return get16(OFFSET.TOTAL_SECTORS_MEDIA);
    }
    
    /**
     * Sets the number of logical sectors
     * 
     * @param v the new number of logical sectors
     */
    protected void setNrLogicalSectors(int v) {
        if (v == getNrLogicalSectors()) return;
        
        set16(OFFSET.TOTAL_SECTORS_MEDIA, v);
    }
    
    protected void setNrTotalSectors(long v) {
        set32(OFFSET.TOTAL_SECTORS_MEDIA, v);
    }
    
    protected long getNrTotalSectors() {
        return get32(OFFSET.TOTAL_SECTORS_MEDIA);
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
