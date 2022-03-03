package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

import io.openems.common.types.OptionsEnum;

public enum VectisStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_CONNECTED(0, "Unknown (VECTIS not connected)"), //
	ON_GRID(1, "On-Grid mode"), //
	OFF_GRID(2, "Off-Grid mode"); //

	private final int value;
	private final String name;

	private VectisStatus(int value, String name) {
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

	/**
	 * Gets the {@link VectisStatus} from an int value.
	 * 
	 * @param value the int value
	 * @return the {@link VectisStatus}
	 */
	public static VectisStatus fromInt(int value) {
		for (VectisStatus status : VectisStatus.values()) {
			if (status.getValue() == value) {
				return status;
			}
		}
		return VectisStatus.UNDEFINED;
	}
}