package io.openems.edge.bridge.modbus.sunspec;

import java.util.Arrays;

/**
 * This is taken from the first sheet inside the SunSpec excel file.
 */
public enum SunSpecModelType {
	COMMON(1, 1), //
	AGGREGATOR(2, 2), //
	NETWORK_CONFIGURATION(10, 19), //
	INVERTER(100, 199), //
	METER(200, 299), //
	ENVIRONMENTAL(300, 399), //
	STRING_COMBINER(400, 499), //
	PANEL(500, 599), //
	TRACKER(600, 699), //
	RESERVED_1(700, 799), //
	STORAGE(800, 899), //
	RESERVED_2(900, 63000), //
	VENDOR_SPECIFIC(64000, 65535);

	private final int startId;
	private final int endId;

	private SunSpecModelType(int startId, int endId) {
		this.startId = startId;
		this.endId = endId;
	}

	/**
	 * Get a {@link SunSpecModelType} by its id.
	 * 
	 * @param id the id
	 * @return The {@link SunSpecModelType}
	 */
	public static SunSpecModelType getModelType(int id) {
		return Arrays.stream(SunSpecModelType.values()) //
				.filter(t -> t.startId <= id && t.endId >= id) //
				.findFirst() //
				.orElseThrow(() -> new IllegalArgumentException("There is no SunSpec Model-Type for ID " + id));
	}
}