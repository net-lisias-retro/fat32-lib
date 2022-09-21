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

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.fat.BootSector;
import net.lisias.fs.disk.pc.Fat16BootSector;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The boot sector layout as used by the FAT12 / FAT16 variants.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 * @author Lisias
 */
final class TosBootSector extends Fat16BootSector {

	public enum OFFSET_EXT implements BootSector.Offset {
		SERIAL(0X08),						// 3 bytes (24 bits)
		EXECFLG(0x1e),						// copied to _cmdload
        LDMODE(0x20),						// load mode
        SSECT(0x22),						// sector start
        SCETCNT(0x24),						// # of sectors to load
        LDADDR(0x26),						// load address
        FATBUF(0x2a),						// FAT address
        FNAME(0x2e),						// file name to load if LDMODE is 0
        RESERVED(0x39),						// reserved
        BOOTIT(0x3a),						// boot code
		CHECKSUM_MAGIC(0x1FF),				// word LSB,MSB
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

	private boolean bootable = false;
	
	/**
     * Creates a new {@code Fat16BootSector} for the specified device.
     *
     * @param device the {@code BlockDevice} holding the boot sector
     * @throws IOException 
     */
    public TosBootSector(final BlockDevice device) throws IOException {
        super(device);
    }
    
    @Override
    protected void checkDisk() throws IOException {
        final ByteBuffer bb = this.buffer;
        
        if ((bb.get(0) & 0xff) != 0x60) 
        	throw new IOException("Missing boot sector signature");
                
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
        
        this.bootable = 0x1234 == this.chksum(bb.array());
        
//        final int media_id = (bb.get(0) & 0xff);			// Fails with 1.2 and 1.4MB disks!
//        if (!(0xf8 <= media_id && media_id <= 0xff))
//        	throw new IOException("Media ID not recognized. Not a MSX floppy disk!");
    }    

    private short chksum(final byte[] buf) {
    	int sum = 0;
    	for(int b:buf){
    	    sum ^= b;
    	}
    	return (short)(sum & 0xFFFF);
    }
    @Override
    public void init() throws IOException {
    	super.init();
    	
        setRootDirEntryCount(DEFAULT_ROOT_DIR_ENTRY_COUNT);
        setVolumeLabel(DEFAULT_VOLUME_LABEL);
        
        {	// Gambiarra to set a 24 bits value using the 32bits function! :-)
        	final int b = get8(OFFSET_EXT.SERIAL.offset()-1);
   	        set32(OFFSET_EXT.SERIAL.offset()-1, random.nextLong() & 0xffffffff);
   	        set8(OFFSET_EXT.SERIAL.offset()-1, b);
        }
        
        //TODO calcular MAGIC de forma que o checksum do setor dê 0x1234
        final int magic = 0x12;
        this.set8(OFFSET_EXT.CHECKSUM_MAGIC, magic);
        
        // Halts Everything. I didn't implemented this yet!
        throw new IOException("Not Implemented yet!");
    }
    
    public boolean isBootable() {
    	return this.bootable;
    }
}
