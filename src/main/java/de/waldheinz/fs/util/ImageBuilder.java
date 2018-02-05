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


package de.waldheinz.fs.util;

import de.waldheinz.fs.fat.FatFile;
import de.waldheinz.fs.fat.FatFileSystem;
import de.waldheinz.fs.fat.FatLfnDirectory;
import de.waldheinz.fs.fat.FatLfnDirectoryEntry;
import de.waldheinz.fs.fat.FatType;
import net.lisias.fs.disk.SuperFloppyFormatter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author Matthias Treydte &lt;mt at waldheinz.de&gt;
 */
public final class ImageBuilder {
    
    public static ImageBuilder of(File rootDir) throws IOException {
        if (!rootDir.isDirectory()) {
            throw new IOException("root must be a directory");
        }
        
        return new ImageBuilder(rootDir);
    }

    private void copyContents(File f, FatFile file)
            throws IOException {
        
        final RandomAccessFile raf = new RandomAccessFile(f, "r");
        
        try {
            final FileChannel fc = raf.getChannel();
            long dstOffset = 0;

            while (true) {
                final int read = fc.read(this.buffer);

                if (read >= 0) {
                    this.buffer.flip();
                    file.write(dstOffset, this.buffer);
                    this.buffer.clear();
                    dstOffset += read;
                } else {
                    break;
                }
            }
        } finally {
            this.buffer.clear();
            raf.close();
        }
    }
    
    private final File imageRoot;
    private final ByteBuffer buffer;
    
    private ImageBuilder(File imageRoot) {
        this.imageRoot = imageRoot;
        this.buffer = ByteBuffer.allocate(1024 * 1024);
    }
    
    public void createDiskImage(final File outFile, final String label) throws IOException {
        final FileDisk fd = FileDisk.create(outFile, 8l * 1024 * 1024 * 1024);
        final FatFileSystem fs = SuperFloppyFormatter.get(SuperFloppyFormatter.eSystem.pc, fd).setFatType(FatType.FAT32).setVolumeLabel(label).format();
        
        try {
            this.copyRec(this.imageRoot, fs.getRoot());
        } finally {
            fs.close();
            fd.close();
        }
    }
    
    private void copyRec(File src,  FatLfnDirectory dst) throws IOException {
        for (File f : src.listFiles()) {
            System.out.println("-> " + f);
            
            if (f.isDirectory()) {
                final FatLfnDirectoryEntry de = dst.addDirectory(f.getName());
                copyRec(f, de.getDirectory());
            } else if (f.isFile()) {
                final FatLfnDirectoryEntry de = dst.addFile(f.getName());
                final FatFile file = de.getFile();
                copyContents(f, file);
            }
            
        }
    }
    
    public static void main(String[] args) throws IOException {
        ImageBuilder
                .of(new File("/home/trem/Downloads/"))
                .createDiskImage(new File("/mnt/archiv/trem/dl.img"), "huhu");
    }
    
}
