package io.openems.edge.bridge.eebus.usecase.powerlimitation.api;

public interface LimitPowerProductionHandler {
	long getNominalMaxProduction();

	void handleLimitPowerProduction(LimitPowerState state, Double currentLimitInW);
}