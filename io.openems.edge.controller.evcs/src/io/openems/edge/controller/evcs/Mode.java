package io.openems.edge.controller.evcs;

import io.openems.edge.evcs.api.ChargeMode;

public class Mode {

	public static enum Config {
		FORCE_CHARGE, EXCESS_POWER, SMART;

		public ChargeMode toChargeMode() {
			return switch (this) {
			case FORCE_CHARGE -> ChargeMode.FORCE_CHARGE;
			case EXCESS_POWER -> ChargeMode.EXCESS_POWER;
			case SMART -> throw new UnsupportedOperationException();
			};
		}
	}
}