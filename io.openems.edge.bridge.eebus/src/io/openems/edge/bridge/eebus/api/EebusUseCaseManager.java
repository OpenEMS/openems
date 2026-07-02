package io.openems.edge.bridge.eebus.api;

import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.ILimitPowerConsumptionHandler;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.ILimitPowerProductionHandler;

public interface EebusUseCaseManager {
	void addLimitPowerProductionHandler(ILimitPowerProductionHandler handler);
	void removeLimitPowerProductionHandler(ILimitPowerProductionHandler handler);

	void addLimitPowerConsumptionHandler(ILimitPowerConsumptionHandler handler);
	void removeLimitPowerConsumptionHandler(ILimitPowerConsumptionHandler handler);
	
}
