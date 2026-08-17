package io.openems.edge.goodwe.common.enums;

import java.util.Arrays;

public enum BatteryPort {

	PORT_1(1), //
	PORT_2(2);

	public final int index;

	private BatteryPort(int index) {
		this.index = index;
	}

	/**
	 * Gets the {@link BatteryPort} from index.
	 * 
	 * @param index the index
	 * @return the {@link BatteryPort}
	 * @throws IllegalArgumentException if no {@link BatteryPort} was found for the
	 *                                  provided index
	 */
	public static BatteryPort fromIndex(int index) {
		return Arrays.stream(BatteryPort.values()).filter(port -> port.index == index).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Invalid index: " + index));
	}
}
