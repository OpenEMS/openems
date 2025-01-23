package io.openems.edge.evcs.hypercharger;

import io.openems.common.types.OptionsEnum;

/**
 * Shows the status of the Hypercharger.
 */
public enum AvailableState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AVAILABLE(0, "Available"), //
	PREPARING_TAG_ID_READY(1, "Ready to prepare the tag ID"), //
	PREPARING_EV_READY(2, "Charging"), //
	CHARGING(3, "Charging"), //
	SUSPENDED_EV(4, "Suspended electric vehicle"), //
	SUSPENDED_EV_SE(5, "Suspended electric vehicle se"), //
	FINISHING(6, "Finishing"), //
	RESERVED(7, "Reserved"), //
	UNAVAILABLE(8, "Unavailable"), //
	UNAVAILABLE_FW_UPDATE(9, "Unavailable firmware update"), //
	FAULTED(10, "Faulted"), //
	UNAVAILABLE_CONNECTION_OBJECT(11, "Unavailable connection");

	private final int value;
	private final String name;

	private AvailableState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
