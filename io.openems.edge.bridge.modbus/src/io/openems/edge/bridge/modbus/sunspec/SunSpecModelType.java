package io.openems.edge.bridge.modbus.sunspec;

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

	protected final int startId;
	protected final int endId;

	private SunSpecModelType(int startId, int endId) {
		this.startId = startId;
		this.endId = endId;
	}

	protected static SunSpecModelType getModelType(int id) {
		for (SunSpecModelType type : SunSpecModelType.values()) {
			if (type.startId <= id && type.endId >= id) {
				return type;
			}
		}
		throw new IllegalArgumentException("There is no SunSpec Model-Type for ID " + id);
	}
}