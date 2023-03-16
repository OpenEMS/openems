package io.openems.edge.io.hal.linuxfs;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class HardwareFactory {
	
	private final Logger logger = LoggerFactory.getLogger(HardwareFactory.class);
	private final String basePath;
	
	public HardwareFactory (String gpioPath) {
		this.basePath = gpioPath;
	}
	
	public LinuxFsDigitalIn fabricateIn(int gpio) {
		logger.debug("Fabricating digital input " + gpio);
		return new LinuxFsDigitalIn(gpio, basePath);
	}
	
	public LinuxFsDigitalOut fabricateOut(int gpio) {
		logger.debug("Fabricating digital output " + gpio);
		return new LinuxFsDigitalOut(gpio, basePath);
	}
}
