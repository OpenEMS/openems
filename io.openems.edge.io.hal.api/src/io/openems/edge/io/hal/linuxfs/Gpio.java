package io.openems.edge.io.hal.linuxfs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gpio implements AutoCloseable {

	private final Logger logger = LoggerFactory.getLogger(Gpio.class);
	private final int pinNumber;
	private final String basePath;
	
	public Gpio(int pinNumber, Direction dir, String basePath) {
		this.basePath = basePath;
		this.pinNumber = pinNumber;

		exportPin(basePath, pinNumber);
		setDirection(dir);
	}
	
	protected void writeValue(String value) {
		writeFile(valuePath(this.pinNumber), value);
	}
	
	protected boolean getValue() {
		return this.readFile() == '1';
	}

	@Override
	public void close() throws Exception {
		writeFile(unexportPath(this.basePath), Integer.toString(this.pinNumber));
	}
	
	private String devicePath(int num) {
		return basePath + String.format("/gpio%d", num);
	}
	
	private String directionPath(int num) {
		return devicePath(num) + "/direction";
	}
	
	private String valuePath(int num) {
		return devicePath(num) + "/value";
	}
	
	private String exportPath(String basePath) {
		return basePath + "/export";
	}
	
	private String unexportPath(String basePath) {
		return basePath + "/unexport";
	}
	private void setDirection(Direction dir) {
		if (dir.equals(Direction.IN)) {
			writeFile(directionPath(pinNumber), "in");	
		} else {
			writeFile(directionPath(pinNumber), "out");
		}
	}
	
	private void exportPin(String basePath, int pinNumber) {
		writeFile(exportPath(basePath), Integer.toString(pinNumber));
	}
	
	private void writeFile(String filename, String value) {
		try {
			var fos = new FileOutputStream(filename);
			fos.write(value.getBytes());
			fos.close();
		} catch (IOException ex) {
			var msg = ex.getMessage();
			if(msg.contains("Permission denied")) {
				throw new RuntimeException("Permission denied to GPIO file: " + msg);
			} else if(msg.contains("busy")) {
				logger.info("GPIO is already exported, ignoring request.");
			} else {
				throw new RuntimeException("Could not write to GPIO file: " + msg);
			}
		}
	}
	
	private int readFile() {
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


}
