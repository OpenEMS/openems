package io.openems.edge.evse.chargepoint.hardybarth.common;

import io.openems.common.types.OptionsEnum;

public enum DeviceRole implements OptionsEnum {

	/**
	 * Master device role.
	 */
	MASTER(0, "2310006"),

	/**
	 * Slave device role.
	 */
	SLAVE(1, "2310007"),

	/**
	 * Unknown device role.
	 */
	UNKNOWN(2, "");

	private final int value;

	private final String product;

	DeviceRole(int value, String product) {
		this.value = value;
		this.product = product;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNKNOWN;
	}

	/**
	 * Determines the device role from model name and product number.
	 *
	 * <p>
	 * Both values must identify the same role; otherwise the result is
	 * {@link #UNKNOWN}.
	 *
	 * @param modelName salia device model name
	 * @param product   raw device product
	 * @return device role
	 */
	public static DeviceRole fromModelNameAndProduct(String modelName, String product) {
		var result = DeviceRole.UNKNOWN;

		if (modelName == null || product == null) {
			return result;
		}

		if (modelName.toUpperCase().contains(DeviceRole.MASTER.name())
				&& product.trim().equals(DeviceRole.MASTER.product)) {
			result = MASTER;
		}

		if (modelName.toUpperCase().contains(DeviceRole.SLAVE.name())
				&& product.trim().equals(DeviceRole.SLAVE.product)) {
			result = SLAVE;
		}

		return result;
	}
}
