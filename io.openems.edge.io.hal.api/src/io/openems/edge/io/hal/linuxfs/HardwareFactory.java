package io.openems.edge.io.hal.linuxfs;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class HardwareFactory {
	
	private final Logger logger = LoggerFactory.getLogger(HardwareFactory.class);
	private final String basePath;
	
	public HardwareFactory(String gpioPath) {
		this.basePath = gpioPath;
	}
	
	/**
	 * Creates a digital input with Linux file system driver.
	 * @param gpio the number of the GPIO on the device.
	 * @return a java object that represents the digital input.
	 */
	public LinuxFsDigitalIn fabricateIn(int gpio) {
		this.logger.debug("Fabricating digital input " + gpio);
		return new LinuxFsDigitalIn(gpio, this.basePath);
	}
	
	
	/**
	 * Creates a digital output with Linux file system driver.
	 * @param gpio the number of the GPIO on the device.
	 * @return a java object that represents the digital output.
	 */
	public LinuxFsDigitalOut fabricateOut(int gpio) {
		this.logger.debug("Fabricating digital output " + gpio);
		return new LinuxFsDigitalOut(gpio, this.basePath);
	}
}
