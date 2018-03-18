package io.openems.edge.api.device.charger;

import java.util.Optional;

public interface ChargerInterface {

	Optional<Long> getPower();
	
	Optional<Long> getNominalPower();
	
	Optional<Long> getVoltage();
	
	Optional<Long> getCurrent();
	
	void setMaxPower(long power);
}
