package io.openems.edge.api.device.meter;

import java.util.Optional;

import io.openems.edge.api.device.DeviceInterface;
import io.openems.edge.api.device.Phase;


public interface SinglePhaseMeterInterface extends DeviceInterface{

	Optional<Long> getActivePower();
	
	Optional<Long> getReactivePower();
	
	Optional<Long> getApparentPower();
	
	Optional<Long> getVoltage();
	
	Optional<Long> getFrequency();
	
	Optional<Long> getCurrent();
	
	Phase getPhase();
}
