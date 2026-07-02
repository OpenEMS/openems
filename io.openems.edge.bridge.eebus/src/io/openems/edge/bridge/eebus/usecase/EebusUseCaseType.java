package io.openems.edge.bridge.eebus.usecase;

import java.util.function.Function;

import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.LimitPowerConsumptionUseCase;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.LimitPowerProductionUseCase;

public enum EebusUseCaseType {
	LIMIT_POWER_PRODUCTION(LimitPowerProductionUseCase::new, "LPP"), //
	LIMIT_POWER_CONSUMPTION(LimitPowerConsumptionUseCase::new, "LPC"), //

	;

	private final Function<BridgeEebus, EebusUseCase> factory;
	private final String shortName;

	private EebusUseCaseType(Function<BridgeEebus, EebusUseCase> factory, String shortName) {
		this.factory = factory;
		this.shortName = shortName;
	}

	EebusUseCase createInstance(BridgeEebus bridge) {
		return this.factory.apply(bridge);
	}

	public String getShortName() {
		return this.shortName;
	}
}
