package io.openems.edge.api.device.ess;

import java.util.Optional;

import io.openems.edge.api.power.AsymmetricPowerLimitationInterface;
import io.openems.edge.api.power.PowerException;

public interface AsymmetricEssInterface {

	Optional<Long> getActivePowerL1();

	Optional<Long> getActivePowerL2();

	Optional<Long> getActivePowerL3();

	Optional<Long> getReactivePowerL1();

	Optional<Long> getReactivePowerL2();

	Optional<Long> getReactivePowerL3();
	
	void applyLimitation(AsymmetricPowerLimitationInterface limit) throws PowerException;
}
