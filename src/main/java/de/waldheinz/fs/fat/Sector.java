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

package de.waldheinz.fs.fat;

import de.waldheinz.fs.BlockDevice;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
class Sector {
    public interface Offset {
    	int offset();
	}

	private final BlockDevice device;
    private final long offset;

    /**
     * The buffer holding the contents of this {@code Sector}.
     */
    protected final ByteBuffer buffer;

    private boolean dirty;
    
    protected Sector(BlockDevice device, long offset, int size) {
        this.offset = offset;
        this.device = device;
        this.buffer = ByteBuffer.allocate(size);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.dirty = true;
    }
    
    /**
     * Reads the contents of this {@code Sector} from the device into the
     * internal buffer and resets the "dirty" state.
     *
     * @throws IOException on read error
     * @see #isDirty() 
     */
    protected void read() throws IOException {
        buffer.rewind();
        buffer.limit(buffer.capacity());
        device.read(offset, buffer);
        this.dirty = false;
    }
    
    public final boolean isDirty() {
        return this.dirty;
    }
    
    protected final void markDirty() {
        this.dirty = true;
    }

    /**
     * Returns the {@code BlockDevice} where this {@code Sector} is stored.
     *
     * @return this {@code Sector}'s device
     */
    public BlockDevice getDevice() {
        return this.device;
    }

    public final void write() throws IOException {
        if (!isDirty()) return;
        
        buffer.position(0);
        buffer.limit(buffer.capacity());
        device.write(offset, buffer);
        this.dirty = false;
    }
   
    protected int get8(final int offset) {
        return buffer.get(offset) & 0xff;
    }
    protected int get8(final Offset o) {
        return this.get8(o.offset());
    }
    
    protected void set8(int offset, int value) {
        if ((value & 0xff) != value) {
            throw new IllegalArgumentException(
                    value + " too big to be stored in a single octet");
        }
        
        buffer.put(offset, (byte) (value & 0xff));
        dirty = true;
    }
    protected void set8(final Offset o, final int value) {
    	this.set8(o.offset(), value);
    }
        
    protected int get16(final int offset) {
        return buffer.getShort(offset) & 0xffff;
    }
    protected int get16(final Offset o) {
        return this.get16(o.offset());
    }

    protected void set16(final int offset, final int value) {
        buffer.putShort(offset, (short) (value & 0xffff));
        dirty = true;
    }
    protected void set16(final Offset o, final int value) {
    	this.set16(o.offset(), value);
    }

    protected long get32(final int offset) {
        return buffer.getInt(offset);
    }
    protected long get32(final Offset o) {
        return this.get32(o.offset());
    }

    protected void set32(final int offset, final long value) {
        buffer.putInt(offset, (int) (value & 0xffffffff));
        dirty = true;
    }
    protected void set32(final Offset o, final long value) {
    	this.set32(o.offset(), value);
    }

   
    /**
     * Returns the device offset to this {@code Sector}.
     *
     * @return the {@code Sector}'s device offset
     */
    protected long getOffset() {
        return this.offset;
    }
    
    protected int getSectorSize() {
    	return this.buffer.capacity();
    }
}
