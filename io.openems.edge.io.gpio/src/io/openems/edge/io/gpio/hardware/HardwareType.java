package io.openems.edge.io.gpio.hardware;

import io.openems.edge.io.gpio.linuxfs.HardwareFactory;

public enum HardwareType {
	MODBERRY_X500_M40804_MAX, //
	MODBERRY_X500_M40804_WB, //
	MODBERRY_X500_M4S, //
	MODBERRY_X500_M4S_F, //
	MODBERRY_X500_M3;

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
		case MODBERRY_X500_M4S, MODBERRY_X500_M4S_F, MODBERRY_X500_M3 -> new ModberryX500M4S(factory);
		};
	}
}
