package io.openems.edge.io.gpio.hardware;

import io.openems.edge.io.gpio.linuxfs.HardwareFactory;

public enum HardwareType {
	MODBERRY_X500_M40804_MAX, //
	MODBERRY_X500_M40804_WB, //
	MODBERRY_X500_M40804_W;

	/**
	 * Create a {@link HardwarePlatform} for this {@link HardwareType}.
	 * 
	 * @param gpioPath the GPIO path
	 * @return the {@link HardwarePlatform}
	 */
	public HardwarePlatform createInstance(String gpioPath) {
		var factory = new HardwareFactory(gpioPath);
		return switch (this) {
		case MODBERRY_X500_M40804_MAX -> new ModberryX500M40804Max(factory);
		case MODBERRY_X500_M40804_WB -> new ModberryX500M40804Wb(factory);
		case MODBERRY_X500_M40804_W -> new ModberryX500M40804W(factory);
		};
	}
}
