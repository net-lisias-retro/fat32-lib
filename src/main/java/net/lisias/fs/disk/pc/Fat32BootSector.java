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
import de.waldheinz.fs.fat.BootSector.OFFSET;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Contains the FAT32 specific parts of the boot sector.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public final class Fat32BootSector extends BootSector {

	public enum OFFSET_EXT implements BootSector.Offset {
		TOTAL_SECTORS_MEDIA(0x20),		// dword, LSB...MSB
		NUMBER_SECTORS_FAT(0x24),		// dword, LSB...MSB 
		ROOT_DIR_FIRST_CLUSTER(0x2c),	// ???
		VERSION(0x2a),					// ???
		FS_INFO_SECTOR(0x30),			// word LSB,MSB
		BOOT_SECTOR_COPY(0x32),			// ???
		EXTENDED_BOOT_SIGNATURE(0x42),	// ???
		FILE_SYSTEM_TYPE(0x52),			// ???
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
  
    public static final int VERSION = 0;
    
    /*
     * TODO: make this constructor private
     */
    public Fat32BootSector(BlockDevice device) throws IOException {
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

        final long total16 = (bb.getShort(OFFSET.TOTAL_SECTORS_MEDIA.offset()) & 0xffff);
        if (0 != total16)
        	throw new IOException("This is a FAT16!");
        
        final long totalSectors = bb.getInt(OFFSET_EXT.TOTAL_SECTORS_MEDIA.offset()) & 0xffffffffl;
        if (0 == totalSectors)
        	throw new IOException("Total sectors is zero. Not a FAT32!");
        
        final int fatSz16 = bb.getShort(OFFSET.NUMBER_SECTORS_FAT.offset())  & 0xffff;
        if (0 != fatSz16)
        	throw new IOException("This is a FAT16!");
        
        final long fatSz = bb.getInt(OFFSET_EXT.NUMBER_SECTORS_FAT.offset()) & 0xffffffffl;
        if (0 == fatSz)
        	throw new IOException("FAT size is zero. Not a FAT32!");
                
        final int reservedSectors = bb.getShort(OFFSET.NUMBER_RESERVED_SECTORS.offset());
        final int fatCount = bb.get(OFFSET.NUMBER_OF_FATS.offset());
        final long dataSectors = totalSectors - (reservedSectors + (fatCount * fatSz) + rootDirSectors);
                
        final long clusterCount = dataSectors / sectorsPerCluster;
    }
    
    @Override
    public void init() throws IOException {
        super.init();

    	/* magic bytes needed by some windows versions to recognize a boot
         * sector. these are x86 jump instructions which lead into
         * nirvana when executed, but we're currently unable to produce really
         * bootable images anyway. So... */
        set8(0x00, 0xeb);
        set8(0x01, 0x3c);
        set8(0x02, 0x90);

        set16(OFFSET_EXT.VERSION, VERSION);
        setBootSectorCopySector(6); /* as suggested by M$ */
    }

    /**
     * Returns the first cluster in the FAT that contains the root directory.
     *
     * @return the root directory's first cluster
     */
    @Override
    public long getRootDirFirstCluster() {
        return get32(OFFSET_EXT.ROOT_DIR_FIRST_CLUSTER);
    }

    /**
     * Sets the first cluster of the root directory.
     *
     * @param value the root directory's first cluster
     */
    @Override
    public void setRootDirFirstCluster(final long value) {
        if (getRootDirFirstCluster() == value) return;
        
        set32(OFFSET_EXT.ROOT_DIR_FIRST_CLUSTER, value);
    }

    /**
     * Sets the sectur number that contains a copy of the boot sector.
     *
     * @param sectNr the sector that contains a boot sector copy
     */
    public void setBootSectorCopySector(int sectNr) {
        if (getBootSectorCopySector() == sectNr) return;
        if (sectNr < 0) throw new IllegalArgumentException(
                "boot sector copy sector must be >= 0");
        
        set16(OFFSET_EXT.BOOT_SECTOR_COPY, sectNr);
    }
    
    /**
     * Returns the sector that contains a copy of the boot sector, or 0 if
     * there is no copy.
     *
     * @return the sector number of the boot sector copy
     */
    public int getBootSectorCopySector() {
        return get16(OFFSET_EXT.BOOT_SECTOR_COPY);
    }

    /**
     * Sets the 11-byte volume label stored at offset 0x47.
     *
     * @param label the new volume label, may be {@code null}
     */
    @Override
    public void setVolumeLabel(String label) {
        for (int i=0; i < 11; i++) {
            final byte c =
                    (label == null) ? 0 :
                    (label.length() > i) ? (byte) label.charAt(i) : 0x20;

            set8(0x47 + i, c);
        }
    }

    @Override
    public int getFsInfoSectorNr() {
        return get16(OFFSET_EXT.FS_INFO_SECTOR);
    }

    public void setFsInfoSectorNr(int offset) {
        if (getFsInfoSectorNr() == offset) return;

        set16(OFFSET_EXT.FS_INFO_SECTOR, offset);
    }
    
    @Override
    public void setSectorsPerFat(long v) {
        if (getSectorsPerFat() == v) return;
        
        set32(OFFSET_EXT.NUMBER_SECTORS_FAT, v);
    }
    
    @Override
    public long getSectorsPerFat() {
        return get32(OFFSET_EXT.NUMBER_SECTORS_FAT);
    }

    @Override
    public FatType getFatType() {
        return FatType.FAT32;
    }

    @Override
    public void setSectorCount(long count) {
        this.setNrTotalSectors(count);
    }

    @Override
    public long getSectorCount() {
        return this.getNrTotalSectors();
    }

    /**
     * This is always 0 for FAT32.
     *
     * @return always 0
     */
    @Override
    public int getRootDirEntryCount() {
        return 0;
    }
    
    public void setFileSystemId(int id) {
        super.set32(0x43, id);
    }

    public int getFileSystemId() {
        return (int) super.get32(0x43);
    }

    /**
     * Writes a copy of this boot sector to the specified device, if a copy
     * is requested.
     *
     * @param device the device to write the boot sector copy to
     * @throws IOException on write error
     * @see #getBootSectorCopySector() 
     */
    public void writeCopy(BlockDevice device) throws IOException {
        if (getBootSectorCopySector() > 0) {
            final long offset = (long)getBootSectorCopySector() * SIZE;
            buffer.rewind();
            buffer.limit(buffer.capacity());
            device.write(offset, buffer);
        }
    }

    @Override
    public BootSector.Offset getFileSystemTypeLabelOffset() {
        return OFFSET_EXT.NUMBER_SECTORS_FAT;
    }

    @Override
    public BootSector.Offset getExtendedBootSignatureOffset() {
        return OFFSET_EXT.FILE_SYSTEM_TYPE;
    }
    
    /**
     * Gets the number of logical sectors
     * 
     * @return int
     */
    protected int getNrLogicalSectors() {
        return get16(OFFSET_EXT.NUMBER_SECTORS_FAT);
    }
    
    /**
     * Sets the number of logical sectors
     * 
     * @param v the new number of logical sectors
     */
    protected void setNrLogicalSectors(int v) {
        if (v == getNrLogicalSectors()) return;
        
        set16(OFFSET_EXT.NUMBER_SECTORS_FAT, v);
    }
    
    protected void setNrTotalSectors(long v) {
        set32(OFFSET_EXT.NUMBER_SECTORS_FAT, v);
    }
    
    protected long getNrTotalSectors() {
        return get32(OFFSET_EXT.NUMBER_SECTORS_FAT);
    }

	@Override
	public String getVolumeLabel() {
		throw new RuntimeException("Invalid Call");
	}
}
