package io.openems.edge.ruhfass.battery.rbti.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryCellType implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	OTHER(0, "Other"), //
	NI_MH(1, "NiMH"), //
	LFP(2, "LFP"), //
	LMO(3, "LMO"), //
	LI_SULFUR(4, "LiSulfur"), //
	NMC(5, "NMC"), //
	SODIUM_SOLPHUR(6, "SodiumSolphur"), //
	LITHIUM_TITANATE(7, "LithiumTitanate"), //
	LEAD_ACID(8, "LeadAcid"), //
	METAL_AIR(9, "MetalAir");

	private final int value;
	private final String name;

	private BatteryCellType(int value, String name) {
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
