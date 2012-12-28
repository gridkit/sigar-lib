package org.gridkit.lab.sigar;

import org.junit.Test;

public class SigarFactoryTest {
    @Test
    public void get_pid_test() {
        System.err.println(SigarFactory.newSigar().getPid());
    }
}
