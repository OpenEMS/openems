package io.openems.edge.io.hal.linuxfs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Gpio implements AutoCloseable {
	
	public static String GPIO_PATH = "/sys/class/gpio";
	public static String EXPORT_PATH = GPIO_PATH + "/export";
	public static String UNEXPORT_PATH = GPIO_PATH + "/unexport";
	
	protected static String devicePath(int num) {
		return GPIO_PATH + String.format("gpio%d", num);
	}
	
	protected static String directionPath(int num) {
		return devicePath(num) + "/direction";
	}
	
	protected static String valuePath(int num) {
		return devicePath(num) + "/value";
	}
	
	private final int pinNumber;
	
	public Gpio(int pinNumber, Direction dir) {
		this.pinNumber = pinNumber;
		// Export pin
		writeFile(EXPORT_PATH, Integer.toString(pinNumber));
		// Set direction
		if (dir.equals(Direction.IN)) {
			writeFile(directionPath(pinNumber), "in");	
		} else {
			writeFile(directionPath(pinNumber), "out");
		}
	}
	
	protected void writeFile(String filename, String value) {
		try {
			var fos = new FileOutputStream(filename);
			fos.write(value.getBytes());
			fos.close();
		} catch (IOException ex) {
			var msg = ex.getMessage();
			if(msg.contains("Permission denied")) {
				throw new RuntimeException("Permission denied to GPIO file: " + msg);
			} else if(msg.contains("busy")) {
				System.out.println("GPIO is already exported, continuing.");
			} else {
				throw new RuntimeException("Could not write to GPIO file: " + msg);
			}
		}
	}
	
	protected int readFile() {
		try {
			var fis = new FileInputStream(valuePath(this.pinNumber));
			return fis.read();
		} catch (IOException ex) {
			var msg = ex.getMessage();
			if(msg.contains("Permission denied")) {
				throw new RuntimeException("Permission denied to GPIO file: " + msg);
			} else {
				throw new RuntimeException("Could not read from GPIO file: " + msg);
			}
		}
	}
	
	protected void writeValue(String value) {
		writeFile(valuePath(this.pinNumber), value);
	}
	
	protected boolean getValue() {
		return this.readFile() == '1';
	}

	@Override
	public void close() throws Exception {
		writeFile(UNEXPORT_PATH, Integer.toString(this.pinNumber));
	}

}
