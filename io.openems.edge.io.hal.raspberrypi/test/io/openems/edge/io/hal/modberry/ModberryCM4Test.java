package io.openems.edge.io.hal.modberry;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


import io.openems.edge.io.hal.linuxfs.HardwareFactory;

public class ModberryCM4Test {
	
	private File root;
	private File exportFile;
	private File directionFile;
	private File valueFile;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    @Before
    public void setUp() {
        try {
        	root = folder.getRoot();
        	new File(root.getAbsolutePath() + "/gpio27").mkdir();
            this.exportFile = folder.newFile("export");
            this.directionFile = folder.newFile("gpio27/direction");
            this.valueFile = folder.newFile("gpio27/value");
            folder.create();
        }
        catch(IOException ioe) {
            System.err.println( 
                "error creating temporary test file in " +
                this.getClass().getSimpleName() );
        }
    }
	
	@Test
	public void testCm4Led() {
		HardwareFactory factory = new HardwareFactory(this.root.getAbsolutePath());
		ModBerryX500CM4 modberry = new ModBerryX500CM4(factory);
		var led = modberry.getLed(Cm4Hardware.Led.LED_1);
		led.on();
		assertTrue(led.isOn());
	}
}
