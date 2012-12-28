package org.gridkit.lab.sigar;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

class LibraryExtractor {
    private static final int BUFFER_LENGTH = 1024 * 4; // 4KB
    
    private final File libDir;
    private final String libDirName;
    
    public LibraryExtractor(String libDirName) {
        this.libDir = new File(System.getProperty("java.io.tmpdir") + File.separator + libDirName);
        this.libDirName = libDirName;
    }
    
    public void extractFile(String libFileName) throws IOException {
        libDir.mkdirs();
        
        File libFile = new File(libDir, libFileName);
        libFile.createNewFile();

        URL libUrl = LibraryExtractor.class.getClassLoader().getResource(libDirName + "/" + libFileName);
        
        byte[] libHash = hash(libUrl);
        
        RandomAccessFile raf = null;
        
        try {
            raf = new RandomAccessFile(libFile, "r");
            
            byte[] libFileHash = hash(raf);
            
            if (!Arrays.equals(libHash, libFileHash)) {
                raf.close();
                raf = new RandomAccessFile(libFile, "rw");
                
                FileLock lock = raf.getChannel().lock();
                try {
                    libFileHash = hash(raf);
                    if (!Arrays.equals(libHash, libFileHash)) {
                        copy(libUrl, raf);
                    }
                } finally {
                    lock.release();
                }
            }
        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }
    
    public String getLibPath() {
        return libDir.getAbsolutePath();
    }
    
    public String getFilePath(String libFileName) {
        return new File(libDir, libFileName).getAbsolutePath();
    }
    
    private static byte[] hash(URL url) throws IOException {
        byte[] buffer = new byte[BUFFER_LENGTH];
        MessageDigest digest = newMessageDigest();
        
        InputStream input = null;
        
        int read = 0;
        try {
            input = url.openStream();
            while ((read = input.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        } finally {
            safeClose(input);
        }

        return digest.digest();
    }
    
    private static byte[] hash(RandomAccessFile file) throws IOException {
        byte[] buffer = null;
        MessageDigest digest = newMessageDigest();
        
        long pointer = 0;
        file.seek(pointer);
        long length = file.length();

        while ((pointer = file.getFilePointer()) < length) {
            int bufferLength = (BUFFER_LENGTH > (length - pointer)) ? (int) (length - pointer) : BUFFER_LENGTH;
            
            if (buffer == null || buffer.length != bufferLength) {
                buffer = new byte[bufferLength];
            }

            file.read(buffer);
            digest.update(buffer);
        }

        return digest.digest();
    }

    private static void copy(URL url, RandomAccessFile file) throws IOException {
        byte[] buffer = new byte[BUFFER_LENGTH];
        InputStream input = null;

        file.seek(0);
        file.setLength(0);
        
        try {
            input = url.openStream();
            
            int length;
            while ((length = input.read(buffer)) > 0){
                file.write(buffer, 0, length);
            }
        } finally {
            safeClose(input);
        }
    }
    
    private static MessageDigest newMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void safeClose(Closeable file) {
        try {
            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            // ignored
        }
    }
}

