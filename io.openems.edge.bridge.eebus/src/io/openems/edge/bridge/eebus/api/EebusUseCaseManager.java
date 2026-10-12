package io.openems.edge.bridge.eebus.api;

import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.LimitPowerConsumptionHandler;
import io.openems.edge.bridge.eebus.usecase.powerlimitation.api.LimitPowerProductionHandler;

public interface EebusUseCaseManager {
	void addLimitPowerProductionHandler(LimitPowerProductionHandler handler);
	void removeLimitPowerProductionHandler(LimitPowerProductionHandler handler);

	void addLimitPowerConsumptionHandler(LimitPowerConsumptionHandler handler);
	void removeLimitPowerConsumptionHandler(LimitPowerConsumptionHandler handler);
	
}
