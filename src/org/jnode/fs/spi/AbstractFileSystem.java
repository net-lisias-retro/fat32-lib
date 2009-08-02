/*
 * $Id: AbstractFileSystem.java 4975 2009-02-02 08:30:52Z lsantha $
 *
 * Copyright (C) 2003-2009 JNode.org
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
 
package org.jnode.fs.spi;

import java.io.IOException;
import java.util.HashMap;

import java.util.logging.Logger;
import org.jnode.driver.Device;
import org.jnode.driver.block.BlockDeviceAPI;
import org.jnode.fs.FSDirectory;
import org.jnode.fs.FSEntry;
import org.jnode.fs.FSFile;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;

/**
 * Abstract class with common things in different FileSystem implementations
 * 
 * @author Fabien DUMINY
 */
public abstract class AbstractFileSystem<T extends FSEntry> implements FileSystem<T> {

    private static final Logger log = Logger.getLogger(AbstractFileSystem.class.getName());
    private boolean readOnly;
    private final BlockDeviceAPI api;
    private boolean closed;
    private T rootEntry;

    // cache of FSFile (key: FSEntry)
    private HashMap<FSEntry, FSFile> files = new HashMap<FSEntry, FSFile>();

    // cache of FSDirectory (key: FSEntry)
    private HashMap<FSEntry, FSDirectory> directories = new HashMap<FSEntry, FSDirectory>();

    /**
     * Construct an AbstractFileSystem in specified readOnly mode
     * 
     * @param device
     * @param readOnly
     * @throws FileSystemException
     */
    public AbstractFileSystem(BlockDeviceAPI api, boolean readOnly) throws FileSystemException {
        this.api = api;
        this.closed = false;
        this.readOnly = readOnly;
    }

    /**
     * @see org.jnode.fs.FileSystem#getRootEntry()
     */
    public T getRootEntry() throws IOException {
        if (isClosed())
            throw new IOException("FileSystem is closed");

        if (rootEntry == null) {
            rootEntry = createRootEntry();
        }
        return rootEntry;
    }

    /**
     * @see org.jnode.fs.FileSystem#close()
     */
    public void close() throws IOException {
        if (!isClosed()) {
            // if readOnly, nothing to do
            if (!isReadOnly()) {
                flush();
            }

            api.flush();
            files.clear();
            directories.clear();

            // these fields are final, can't nullify them
            // device = null;
            // api = null;

            rootEntry = null;
            files = null;
            directories = null;
            closed = true;
        }
    }

    /**
     * Save the content that have been altered but not saved in the Device
     * 
     * @throws IOException
     */
    public void flush() throws IOException {
        flushFiles();
        flushDirectories();
    }

    /**
     * @return Returns the api.
     */
    public final BlockDeviceAPI getApi() {
        return api;
    }

    /**
     * @return Returns the FSApi.
     * @throws ApiNotFoundException
     */
    public final BlockDeviceAPI getFSApi() {
        return api;
    }

    /**
     * @return if filesystem is closed.
     */
    public final boolean isClosed() {
        return closed;
    }

    /**
     * @return if filesystem is readOnly.
     */
    public final boolean isReadOnly() {
        return readOnly;
    }

    protected final void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Gets the file for the given entry.
     * 
     * @param entry
     * @return the FSFile object associated with entry
     * @throws IOException
     */
    public final synchronized FSFile getFile(FSEntry entry) throws IOException {
        if (isClosed())
            throw new IOException("FileSystem is closed");

        FSFile file = files.get(entry);
        if (file == null) {
            file = createFile(entry);
            files.put(entry, file);
        }
        return file;
    }

    /**
     * Abstract method to create a new FSFile from the entry
     * 
     * @param entry
     * @return a new created FSFile
     * @throws IOException
     */
    protected abstract FSFile createFile(FSEntry entry) throws IOException;

    /**
     * Flush all unsaved FSFile in our cache
     * 
     * @throws IOException
     */
    private final void flushFiles() throws IOException {
        log.info("flushing files ...");
        for (FSFile f : files.values()) {
            log.finer("flush: flushing file " + f);

            f.flush();
        }
    }

    /**
     * Gets the file for the given entry.
     * 
     * @param entry
     * @return the FSDirectory object associated with this entry
     * @throws IOException
     */
    public final synchronized FSDirectory getDirectory(FSEntry entry) throws IOException {
        if (isClosed())
            throw new IOException("FileSystem is closed");

        FSDirectory dir = directories.get(entry);
        if (dir == null) {
            dir = createDirectory(entry);
            directories.put(entry, dir);
        }
        return dir;
    }

    /**
     * Abstract method to create a new directory from the given entry
     * 
     * @param entry
     * @return the new created FSDirectory
     * @throws IOException
     */
    protected abstract FSDirectory createDirectory(FSEntry entry) throws IOException;

    /**
     * Flush all unsaved FSDirectory in our cache
     * 
     * @throws IOException
     */
    private final void flushDirectories() {
        log.info("flushing directories ...");
        for (FSDirectory d : directories.values()) {
            log.finer("flush: flushing directory " + d);

            //TODO: uncomment this line
//            d.flush();
        }
    }

    /**
     * Abstract method to create a new root entry
     * @return the new created root entry
     * @throws IOException
     */
    protected abstract T createRootEntry() throws IOException;
}