package io.openems.edge.io.gpio.linuxfs;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public class Gpio implements AutoCloseable {

	private final Logger log = LoggerFactory.getLogger(Gpio.class);
	private final int pinNumber;
	private final String basePath;

	public Gpio(int pinNumber, Direction dir, String basePath) {
		this.basePath = basePath;
		this.pinNumber = pinNumber;
		try {
			this.exportPin(basePath, pinNumber);
			this.setDirection(dir);

		} catch (OpenemsException e) {
			this.log.error(e.getMessage());
		}
	}

	protected void writeValue(String value) throws OpenemsException {
		this.writeFile(this.valuePath(this.pinNumber), value);
	}

	/**
	 * Gets the value of the GPIO pin.
	 * 
	 * @return a value of true/false if reading the value was successful, otherwise
	 *         empty.
	 */
	public Optional<Boolean> getValue() {
		try {
			return Optional.of(this.readFile() == '1');
		} catch (OpenemsException ex) {
			return Optional.empty();
		}
	}

	@Override
	public void close() throws Exception {
		this.writeFile(this.unexportPath(this.basePath), Integer.toString(this.pinNumber));
	}

	private String devicePath(int num) {
		return this.basePath + String.format("/gpio%d", num);
	}

	private String directionPath(int num) {
		return this.devicePath(num) + "/direction";
	}

	private String valuePath(int num) {
		return this.devicePath(num) + "/value";
	}

	private String exportPath(String basePath) {
		return basePath + "/export";
	}

	private String unexportPath(String basePath) {
		return basePath + "/unexport";
	}

	private void setDirection(Direction dir) throws OpenemsException {
		if (dir.equals(Direction.IN)) {
			this.writeFile(this.directionPath(this.pinNumber), "in");
		} else {
			this.writeFile(this.directionPath(this.pinNumber), "out");
		}
	}

	private void exportPin(String basePath, int pinNumber) throws OpenemsException {
		this.writeFile(this.exportPath(basePath), Integer.toString(pinNumber));
	}

	private synchronized void writeFile(String filename, String value) throws OpenemsException {
		try (var fos = new FileOutputStream(filename)) {
			fos.write(value.getBytes());
		} catch (IOException ex) {
			var msg = ex.getMessage();
			if (msg.contains("Permission denied")) {
				throw new OpenemsException("Not able to export GPIO pin [" + this.pinNumber + "]: permission denied.");
			} else if (msg.contains("busy")) {
				throw new OpenemsException("Skipping write to GPIO pin [" + this.pinNumber + "]: device is busy.");
			} else {
				throw new OpenemsException("Unkown error writing GPIO file: " + msg);
			}
		}
	}

	private synchronized int readFile() throws OpenemsException {
		try (var fis = new FileInputStream(this.valuePath(this.pinNumber))) {
			return fis.read();
		} catch (IOException ex) {
			var msg = ex.getMessage();
			if (msg.contains("Permission denied")) {
				throw new OpenemsException("Permission denied to GPIO file: " + msg);
			} else {
				throw new OpenemsException("Could not read from GPIO file: " + msg);
			}
		}
	}

	@Override
	public String toString() {
		return "GPIO" + this.pinNumber;
	}
}
