package io.openems.edge.api.device.ess;

import java.util.Optional;

import io.openems.edge.api.device.Phase;
import io.openems.edge.api.power.PowerException;
import io.openems.edge.api.power.SymmetricPowerLimitationInterface;


public interface SinglePhaseEssInterface extends EssInterface{

Optional<Long> getActivePower();
	
	Optional<Long> getReactivePower();
	
	Optional<Long> getApparentPower();
	
	void applyLimitation(SymmetricPowerLimitationInterface limit) throws PowerException;
	
	Phase getPhase();
}
