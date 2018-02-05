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

import de.waldheinz.fs.BlockDevice;

import java.io.IOException;

/**
 * The boot sector.
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 * @author Lisias T &lt;support at lisias.net&gt;
 */
public abstract class BootSector extends Sector {

	public enum OFFSET implements Sector.Offset {
		BRANCH_S(0x00),					// 3 bytes
		OEM_NAME(0x03),					// 8 chars
		BYTES_PER_SECTOR(0x0B),			// word (LSB, MSB)
		SECTORS_PER_CLUSTER(0x0D),		// byte
		NUMBER_RESERVED_SECTORS(0x0E), 	// word (LSB, MSB)
		NUMBER_OF_FATS(0x10),			// byte
		ROOT_DIR_ENTRIES(0x11),			// word (LSB, MSB)
		TOTAL_SECTORS_MEDIA(0x13),		// word (LSB, MSB)
		MEDIA_DESCRIPTOR(0x15),			// byte
		NUMBER_SECTORS_FAT(0x16),		// word (LSB, MSB)
		SECTORS_PER_TRACK(0x18),		// word (LSB, MSB)
		NUMBER_OF_HEADS(0x1A),			// word (LSB, MSB)
		NUMBER_HIDDEN_SECTORS(0x1C),	// word (LSB, MSB)
		BOOT_SECTOR_SIGNATURE(0x1FE),	// word
		;
			
		private final byte o;

		OFFSET(final int value) {
			this.o = (byte)value;
		}

		@Override
		public int offset() {
			return this.o;
		}
	}
	
	// Don't static this, so subclasses can override the value!
	public final String OEM_NAME = "FAT32lib";
       
    /**
     * The length of the file system type string.
     *
     * @see #getFileSystemType()
     */
    public static final int FILE_SYSTEM_TYPE_LENGTH = 8;

    public static final int EXTENDED_BOOT_SIGNATURE = 0x29;

    /**
     * The size of a boot sector in bytes.
     */
    public final static int SIZE = 512;
    
    protected BootSector(BlockDevice device) throws IOException {
        super(device, 0, SIZE);
        markDirty();
        this.read();
        this.checkDisk();
    }
    
    protected abstract void checkDisk() throws IOException;
    
    public abstract FatType getFatType();
    
    /**
     * Gets the number of sectors per FAT.
     * 
     * @return the sectors per FAT
     */
    public abstract long getSectorsPerFat();
    
    /**
     * Sets the number of sectors/fat
     * 
     * @param v  the new number of sectors per fat
     */
    public abstract void setSectorsPerFat(long v);

    public abstract void setSectorCount(long count);

    public abstract int getRootDirEntryCount();
    
    public abstract long getSectorCount();
    
    /**
     * Gets the offset (in bytes) of the fat with the given index
     * 
     * @param bs
     * @param fatNr (0..)
     * @return long
     * @throws IOException 
     */
    public final long getFatOffset(int fatNr) {
        long sectSize = this.getBytesPerSector();
        long sectsPerFat = this.getSectorsPerFat();
        long resSects = this.getNrReservedSectors();

        long offset = resSects * sectSize;
        long fatSize = sectsPerFat * sectSize;

        offset += fatNr * fatSize;

        return offset;
    }

    /**
     * Gets the offset (in bytes) of the root directory with the given index
     * 
     * @param bs
     * @return long
     * @throws IOException 
     */
    public final long getRootDirOffset() {
        long sectSize = this.getBytesPerSector();
        long sectsPerFat = this.getSectorsPerFat();
        int fats = this.getNrFats();

        long offset = getFatOffset(0);
        
        offset += fats * sectsPerFat * sectSize;

        return offset;
    }

    /**
     * Gets the offset of the data (file) area
     * 
     * @param bs
     * @return long
     * @throws IOException 
     */
    public final long getFilesOffset() {
        long offset = getRootDirOffset();
        
        offset += this.getRootDirEntryCount() * 32l;
        
        return offset;
    }
    
    /**
     * Returns the offset to the file system type label, as this differs
     * between FAT12/16 and FAT32.
     *
     * @return the offset to the file system type label
     */
    public abstract Offset getFileSystemTypeLabelOffset();
   
    public abstract Offset getExtendedBootSignatureOffset();
    
    public void init() throws IOException {
        setBytesPerSector(getDevice().getSectorSize());
        setSectorCount(getDevice().getSize() / getDevice().getSectorSize());
        set8(getExtendedBootSignatureOffset(), EXTENDED_BOOT_SIGNATURE);
        setBytes(OFFSET.OEM_NAME, OEM_NAME.getBytes(), 8);
        
        /* the boot sector signature */
        set16(OFFSET.BOOT_SECTOR_SIGNATURE, 0xAA55);
    }
    
	/**
     * Returns the file system type label string.
     *
     * @return the file system type string
     * @see #setFileSystemTypeLabel(java.lang.String)
     * @see #getFileSystemTypeLabelOffset() 
     * @see #FILE_SYSTEM_TYPE_LENGTH
     */
    public String getFileSystemTypeLabel() {
        final StringBuilder sb = new StringBuilder(FILE_SYSTEM_TYPE_LENGTH);

        for (int i=0; i < FILE_SYSTEM_TYPE_LENGTH; i++) {
            sb.append ((char) get8(getFileSystemTypeLabelOffset().offset() + i));
        }

        return sb.toString();
    }

    /**
     * 
     *
     * @param fsType the
     * @throws IllegalArgumentException if the length of the specified string
     *      does not equal {@link #FILE_SYSTEM_TYPE_LENGTH}
     */
    public void setFileSystemTypeLabel(String fsType)
            throws IllegalArgumentException {

        if (fsType.length() != FILE_SYSTEM_TYPE_LENGTH) {
            throw new IllegalArgumentException();
        }

        for (int i=0; i < FILE_SYSTEM_TYPE_LENGTH; i++) {
            set8(getFileSystemTypeLabelOffset().offset() + i, fsType.charAt(i));
        }
    }

    /**
     * Returns the number of clusters that are really needed to cover the
     * data-caontaining portion of the file system.
     *
     * @return the number of clusters usable for user data
     * @see #getDataSize() 
     */
    public final long getDataClusterCount() {
        return getDataSize() / getBytesPerCluster();
    }

    /**
     * Returns the size of the data-containing portion of the file system.
     *
     * @return the number of bytes usable for storing user data
     */
    private long getDataSize() {
        return (getSectorCount() * getBytesPerSector()) -
                this.getFilesOffset();
    }

    /**
     * Gets the OEM name
     * 
     * @return String
     */
    public String getOemName() {
        StringBuilder b = new StringBuilder(8);
        
        for (int i = 0; i < 8; i++) {
            int v = get8(0x3 + i);
            if (v == 0) break;
            b.append((char) v);
        }
        
        return b.toString();
    }


    /**
     * Sets the OEM name, must be at most 8 characters long.
     *
     * @param name the new OEM name
     */
    public void setOemName(String name) {
        if (name.length() > 8) throw new IllegalArgumentException(
                "only 8 characters are allowed");

        for (int i = 0; i < 8; i++) {
            char ch;
            if (i < name.length()) {
                ch = name.charAt(i);
            } else {
                ch = (char) 0;
            }

            set8(0x3 + i, ch);
        }
    }
    
    /**
     * Gets the number of bytes/sector
     * 
     * @return int
     */
    public int getBytesPerSector() {
        return get16(0x0b);
    }

    /**
     * Sets the number of bytes/sector
     * 
     * @param v the new value for bytes per sector
     */
    public void setBytesPerSector(int v) {
        if (v == getBytesPerSector()) return;

        switch (v) {
            case 512: case 1024: case 2048: case 4096:
                set16(0x0b, v);
                break;
                
            default:
                throw new IllegalArgumentException();
        }
    }

    private static boolean isPowerOfTwo(int n) {
        return ((n!=0) && (n&(n-1))==0);
    }

    /**
     * Returns the number of bytes per cluster, which is calculated from the
     * {@link #getSectorsPerCluster() sectors per cluster} and the
     * {@link #getBytesPerSector() bytes per sector}.
     *
     * @return the number of bytes per cluster
     */
    public int getBytesPerCluster() {
        return this.getSectorsPerCluster() * this.getBytesPerSector();
    }

    /**
     * Gets the number of sectors/cluster
     * 
     * @return int
     */
    public int getSectorsPerCluster() {
        return get8(OFFSET.SECTORS_PER_CLUSTER);
    }

    /**
     * Sets the number of sectors/cluster
     *
     * @param v the new number of sectors per cluster
     */
    public void setSectorsPerCluster(int v) {
        if (v == getSectorsPerCluster()) return;
        if (!isPowerOfTwo(v)) throw new IllegalArgumentException("value must be a power of two");
        
        set8(OFFSET.SECTORS_PER_CLUSTER, v);
    }
    
    /**
     * Gets the number of reserved (for bootrecord) sectors
     * 
     * @return int
     */
    public int getNrReservedSectors() {
        return get16(OFFSET.NUMBER_RESERVED_SECTORS);
    }

    /**
     * Sets the number of reserved (for bootrecord) sectors
     * 
     * @param v the new number of reserved sectors
     */
    public void setNrReservedSectors(int v) {
        if (v == getNrReservedSectors()) return;
        if (v < 1) throw new IllegalArgumentException("there must be >= 1 reserved sectors");
        
        set16(OFFSET.NUMBER_RESERVED_SECTORS, v);
    }

    /**
     * Gets the number of fats
     * 
     * @return int
     */
    public final int getNrFats() {
        return get8(OFFSET.NUMBER_OF_FATS);
    }

    /**
     * Sets the number of fats
     *
     * @param v the new number of fats
     */
    public final void setNrFats(int v) {
        if (v == getNrFats()) return;
        set8(OFFSET.NUMBER_OF_FATS, v);
    }
    
    /**
     * Gets the medium descriptor byte
     * 
     * @return int
     */
    public int getMediumDescriptor() {
        return get8(0x15);
    }

    /**
     * Sets the medium descriptor byte
     * 
     * @param v the new medium descriptor
     */
    public void setMediumDescriptor(int v) {
        set8(0x15, v);
    }
    
    /**
     * Gets the number of sectors/track
     * 
     * @return int
     */
    public int getSectorsPerTrack() {
        return get16(0x18);
    }

    /**
     * Sets the number of sectors/track
     *
     * @param v the new number of sectors per track
     */
    public void setSectorsPerTrack(int v) {
        if (v == getSectorsPerTrack()) return;
        
        set16(0x18, v);
    }

    /**
     * Gets the number of heads
     * 
     * @return int
     */
    public int getNrHeads() {
        return get16(0x1a);
    }

    /**
     * Sets the number of heads
     * 
     * @param v the new number of heads
     */
    public void setNrHeads(int v) {
        if (v == getNrHeads()) return;
        
        set16(0x1a, v);
    }

    /**
     * Gets the number of hidden sectors
     * 
     * @return int
     */
    public long getNrHiddenSectors() {
        return get32(0x1c);
    }

    /**
     * Sets the number of hidden sectors
     *
     * @param v the new number of hidden sectors
     */
    public void setNrHiddenSectors(long v) {
        if (v == getNrHiddenSectors()) return;
        
        set32(0x1c, v);
    }
    
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder(1024);
        res.append("Bootsector :\n");
        res.append("oemName=");
        res.append(getOemName());
        res.append('\n');
        res.append("medium descriptor = ");
        res.append(getMediumDescriptor());
        res.append('\n');
        res.append("Nr heads = ");
        res.append(getNrHeads());
        res.append('\n');
        res.append("Sectors per track = ");
        res.append(getSectorsPerTrack());
        res.append('\n');
        res.append("Sector per cluster = ");
        res.append(getSectorsPerCluster());
        res.append('\n');
        res.append("byte per sector = ");
        res.append(getBytesPerSector());
        res.append('\n');
        res.append("Nr fats = ");
        res.append(getNrFats());
        res.append('\n');
        res.append("Nr hidden sectors = ");
        res.append(getNrHiddenSectors());
        res.append('\n');
        res.append("Nr logical sectors = ");
        res.append(getSectorCount());
        res.append('\n');
        res.append("Nr reserved sector = ");
        res.append(getNrReservedSectors());
        res.append('\n');
        
        return res.toString();
    }

	public abstract void setRootDirFirstCluster(final long startCluster);
	public abstract int getFsInfoSectorNr();
	public abstract long getRootDirFirstCluster();
	public abstract String getVolumeLabel();
	public abstract void setVolumeLabel(String label);
}
