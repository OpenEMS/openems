package io.openems.edge.io.gpio.linuxfs;

public class HardwareFactory {

	private final String basePath;

	public HardwareFactory(String gpioPath) {
		this.basePath = gpioPath + "/gpio";
	}

	/**
	 * Creates a digital input with Linux file system driver.
	 * 
	 * @param gpio the number of the GPIO on the device.
	 * @return a java object that represents the digital input.
	 */
	public LinuxFsDigitalIn fabricateIn(int gpio) {
		return new LinuxFsDigitalIn(gpio, this.basePath);
	}

	/**
	 * Creates a digital output with Linux file system driver.
	 * 
	 * @param gpio the number of the GPIO on the device.
	 * @return a java object that represents the digital output.
	 */
	public LinuxFsDigitalOut fabricateOut(int gpio) {
		return new LinuxFsDigitalOut(gpio, this.basePath);
	}
}
