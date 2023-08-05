package io.openems.edge.controller.evcs;

import io.openems.edge.evcs.api.ChargeMode;

public record ModeConfig(boolean enabledCharging, ChargeMode chargeMode, int forceChargeMinPower,
		int defaultChargeMinPower, Priority priority, int energySessionLimit) {
}