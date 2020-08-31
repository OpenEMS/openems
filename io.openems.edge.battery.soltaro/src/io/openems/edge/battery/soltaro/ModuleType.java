package io.openems.edge.battery.soltaro;

/**
 * This type defines the module type.
 */
public enum ModuleType {

	MODULE_3_KWH(3000), //
	MODULE_3_5_KWH(3500);

	private ModuleType(int capacity) {
		this.capacity = capacity;
	}

	private int capacity;

	public int getCapacity_Wh() {
		return this.capacity;
	}
}
