package org.gridkit.lab.sigar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarLoader;
import org.hyperic.sigar.SigarLog;
import org.slf4j.LoggerFactory;

// TODO handle concurrent extraction
public class SigarFactory {
    private static final String SIGAR_LIB_ZIP = "sigar-lib.zip";
        
    private static final String JAVA_IO_TMPDIR = System.getProperty("java.io.tmpdir");
        
    private static final File SIGAR_LIB_TMP_DIR = new File(JAVA_IO_TMPDIR + File.separator + "sigar-lib");

    private static final int COPY_BUFFER_SIZE = 1024 * 4;
    
    private static final String SIGAR_LOG_NAME = "Sigar";
    
    static {
        try {
            Object level = disableSigarLogger();
            try {
                newSigar().getPid();
            } finally {
                enableSigarLogger(level);
            }
        } catch (UnsatisfiedLinkError ule) {
            try {
                Class.forName(SigarInitializer.class.getName());
            } catch (ClassNotFoundException cnfe) {
                throw new UnsatisfiedLinkError(cnfe.getMessage());
            }
        }
    }
    
    public static Sigar newSigar() {
        return new Sigar();
    }

    private static Object disableSigarLogger() {
        try {
            return disableSigarLoggerLog4j();
        } catch (Error e1) {
            try {
                return disableSigarLoggerLogback();
            } catch (Error e2) {
                return null;
            }
        }
    }
    
    private static void enableSigarLogger(Object level) {
        if (level != null) {
            try {
                enableSigarLoggerLog4j(level);
            } catch (Error e1) {
                try {
                    enableSigarLoggerLogback(level);
                } catch (Error e2) {
                    
                }
            }
        }
    }
    
    private static Object disableSigarLoggerLog4j() {
        org.apache.log4j.Logger sigarLogger = SigarLog.getLogger(SIGAR_LOG_NAME);
        
        org.apache.log4j.Level level = sigarLogger.getLevel();
        
        sigarLogger.setLevel(org.apache.log4j.Level.OFF);
        
        return level;
    }
    
    private static Object disableSigarLoggerLogback() {
        ch.qos.logback.classic.Logger sigarLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(SIGAR_LOG_NAME);
        
        ch.qos.logback.classic.Level level = sigarLogger.getLevel();
        
        sigarLogger.setLevel(ch.qos.logback.classic.Level.OFF);
        
        return level;
    }
    
    private static void enableSigarLoggerLog4j(Object rawLevel) {
        if (rawLevel instanceof org.apache.log4j.Level) {
            org.apache.log4j.Logger sigarLogger = SigarLog.getLogger(SIGAR_LOG_NAME);
            
            sigarLogger.setLevel((org.apache.log4j.Level)rawLevel);
        }
    }
    
    private static void enableSigarLoggerLogback(Object rawLevel) {
        if (rawLevel instanceof ch.qos.logback.classic.Level) {
            ch.qos.logback.classic.Logger sigarLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(SIGAR_LOG_NAME);
                    
            sigarLogger.setLevel((ch.qos.logback.classic.Level)rawLevel);
        }
    }
    
    private static class SigarInitializer {
        static {
            try {
                File sigarLib = new File(SIGAR_LIB_TMP_DIR, SigarLoader.getNativeLibraryName());

                if (!sigarLib.exists()) {
                    SIGAR_LIB_TMP_DIR.mkdirs();
                    
                    File sigarLibZip = copySigarLib();
                    
                    unzip(sigarLibZip, SIGAR_LIB_TMP_DIR);
                    
                    sigarLibZip.delete();
                }
                
                System.load(sigarLib.getAbsolutePath());
                
                newSigar().getPid();
            } catch (IOException e) {
                throw new UnsatisfiedLinkError(e.getMessage());
            }
        }
        
        private static File copySigarLib() throws IOException {
            File sigarLibZip = new File(SIGAR_LIB_TMP_DIR + File.separator + SIGAR_LIB_ZIP);
                        
            OutputStream os = new BufferedOutputStream(new FileOutputStream(sigarLibZip, false));
            URL url = SigarFactory.class.getClassLoader().getResource(SIGAR_LIB_ZIP);
            
            copy(url, os);
            
            os.close();
            
            return sigarLibZip;
        }
        
        public static void unzip(File zipFile, File folder) throws IOException {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];

            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            
            ZipEntry ze = zis.getNextEntry();
        
            while(ze!=null){
                File newFile = new File(folder, ze.getName());
                
                new File(newFile.getParent()).mkdirs();
        
                FileOutputStream fos = new FileOutputStream(newFile);
        
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
        
                fos.close();
                
                zis.closeEntry();
                ze = zis.getNextEntry();
            }

            zis.close();
        }
        
        
        public static void copy(URL url, OutputStream os) throws IOException {
            InputStream is = new BufferedInputStream(url.openStream());

            byte[] buffer = new byte[COPY_BUFFER_SIZE];
     
            int length;
            while ((length = is.read(buffer)) > 0){
                os.write(buffer, 0, length);
            }
            
            is.close();
        }
    }
}
