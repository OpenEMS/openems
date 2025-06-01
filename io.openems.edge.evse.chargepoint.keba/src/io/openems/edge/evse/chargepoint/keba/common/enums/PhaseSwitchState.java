package io.openems.edge.evse.chargepoint.keba.common.enums;

import static io.openems.edge.evse.api.SingleThreePhase.SINGLE_PHASE;
import static io.openems.edge.evse.api.SingleThreePhase.THREE_PHASE;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.evse.api.SingleThreePhase;

public enum PhaseSwitchState implements OptionsEnum {
	UNDEFINED(-1, "Undefined", null), //
	SINGLE(0, "1 phase", SINGLE_PHASE), //
	THREE(1, "3 phases", THREE_PHASE);

	public final SingleThreePhase actual;

	private final int value;
	private final String name;

	private PhaseSwitchState(int value, String name, SingleThreePhase actual) {
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