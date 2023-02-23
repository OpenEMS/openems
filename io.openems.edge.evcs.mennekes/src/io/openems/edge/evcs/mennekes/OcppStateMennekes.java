package io.openems.edge.evcs.mennekes;

import io.openems.common.types.OptionsEnum;

public enum OcppStateMennekes implements OptionsEnum{
	UNDEFINED(-1, "Undefined"), //
	AVAILABLE(0, "Available"), //
	OCCUPIED(1, "Occupied"), //
	RESERVED(2, "Reserved"), //
	UNAVAILABLE(3, "Unavailable"), //
	FAULTED(4, "Faulted"), //
	PREPARING(5, "Preparing"), //
	CHARGING(6, "Charging"), //
	SUSPENDEDEVSE(7, "SuspendedEVSE"), //
	SUSPENDEDEV(8, "SuspendedEV"), //
	FINISHING(9, "Finishing"), //
	;

	private final int value;
	private final String name;

	private OcppStateMennekes(int value, String name) {
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
