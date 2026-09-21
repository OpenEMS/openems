package io.openems.edge.simulator.evse.chargepoint.enums;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.type.Phase;

public enum PhaseSwitchState implements OptionsEnum {
	UNDEFINED(-1, "Undefined", null), //
	SINGLE(1, "1 phase", Phase.SingleOrThreePhase.SINGLE_PHASE), //
	THREE(3, "3 phases", Phase.SingleOrThreePhase.THREE_PHASE);

	public final Phase.SingleOrThreePhase actual;

	private final int value;
	private final String name;

	private PhaseSwitchState(int value, String name, Phase.SingleOrThreePhase actual) {
		this.value = value;
		this.name = name;
		this.actual = actual;
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