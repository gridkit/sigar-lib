package org.gridkit.lab.sigar;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hyperic.jni.ArchNotSupportedException;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarLoader;
import org.hyperic.sigar.SigarProxy;

public class SigarFactory {
    private static final String SIGAR_LIB_DIR = "sigar-1.6.4";
    
    private static final String SIGAR_X86_WINNT_DLL = "sigar-x86-winnt.dll";
    private static final String SIGAR_X86_WINNT_LIB = "sigar-x86-winnt.lib";

    public static SigarProxy newSigar() {
        return Loader.newSigar();
    }
    
    private static class Loader {
        static {
            LibraryExtractor libExtractor = new LibraryExtractor(SIGAR_LIB_DIR);
            
            try {
                for (String libFile : getSigarLibFiles()) {
                    libExtractor.extractFile(libFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            System.setProperty("org.hyperic.sigar.path", libExtractor.getLibPath());
        }
        
        public static SigarProxy newSigar() {
            return new Sigar();
        }
    }
    
    private static List<String> getSigarLibFiles() {
        try {
            String libName = new SigarLoader(SigarProxy.class).getLibraryName();
            
            if (SIGAR_X86_WINNT_DLL.equals(libName)) {
                return Arrays.asList(SIGAR_X86_WINNT_DLL, SIGAR_X86_WINNT_LIB);
            } else {
                return Collections.singletonList(libName);
            }
        } catch (ArchNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
