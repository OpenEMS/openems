package io.openems.edge.kaco.blueplanet.hybrid10.core;

import io.openems.common.types.OptionsEnum;

public enum StableVersion implements OptionsEnum {

	UNDEFINED(-1, "Undefined", 0, 0), //
	VERSION_7_OR_OLDER(1, "Version 7 or older", 0, 8.2f), //
	VERSION_8(2, "Version 8", 8.3f, 100);

	private int value;
	private String name;
	private float firstComVersion;
	private float lastComVersion;

	private StableVersion(int value, String name, float firstComVersion, float lastComVersion) {
		this.value = value;
		this.name = name;
		this.firstComVersion = firstComVersion;
		this.lastComVersion = lastComVersion;
	}

	public float getFirstComVersion() {
		return this.firstComVersion;
	}

	public float getLastComVersion() {
		return this.lastComVersion;
	}

	/**
	 * Get current stable version of the comVersion.
	 * 
	 * @param comVersion com version of the kaco
	 * @return StableVersion
	 */
	public static StableVersion getCurrentStableVersion(Float comVersion) {
		if (comVersion == null) {
			return StableVersion.UNDEFINED;
		}

		for (StableVersion stableVersion : StableVersion.values()) {
			if (comVersion >= stableVersion.firstComVersion && comVersion <= stableVersion.lastComVersion) {
				return stableVersion;
			}
		}
		return StableVersion.UNDEFINED;
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
		return StableVersion.UNDEFINED;
	}
}
