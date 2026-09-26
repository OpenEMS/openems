package io.openems.edge.bridge.eebus.usecase.powerlimitation.api;

public interface ILimitPowerConsumptionHandler {
	long getNominalMaxConsumption();

	void handleLimitPowerConsumption(LimitPowerState state, Double currentLimitInW);
}
