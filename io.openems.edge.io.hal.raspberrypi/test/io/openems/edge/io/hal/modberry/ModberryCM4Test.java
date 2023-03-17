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

	private ModBerryX500CM4 modberry;
	private HardwareFactory factory;
	
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    @Before
    public void setUp() {
        try {
        	this.root = this.folder.getRoot();
        	new File(this.root.getAbsolutePath() + "/gpio27").mkdir();
            this.exportFile = this.folder.newFile("export");
            this.directionFile = this.folder.newFile("gpio27/direction");
            this.valueFile = this.folder.newFile("gpio27/value");
            this.folder.create();
        } catch (IOException ioe) {
            System.err.println("error creating temporary test file in " + this.getClass().getSimpleName());
        }
        
        this.factory = new HardwareFactory(this.root.getAbsolutePath());
        this.modberry = new ModBerryX500CM4(this.factory);
		
    }
	
	@Test
	public void testCm4Led() {
		var led = this.modberry.getLed(Cm4Hardware.Led.LED_1);
		led.on();
		assertTrue(led.isOn());
	}
	
	@Test
	public void testCm4LedToggle() {
		var led = this.modberry.getLed(Cm4Hardware.Led.LED_1);
		led.toggle();
		assertTrue(led.isOn());
	}
}
