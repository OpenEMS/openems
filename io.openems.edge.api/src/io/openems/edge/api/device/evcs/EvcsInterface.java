package io.openems.edge.api.device.evcs;

import java.util.Optional;

import io.openems.edge.api.device.DeviceInterface;


public interface EvcsInterface extends DeviceInterface {

	Optional<Long> getActivePower();
	
	void setActivePower(long power);
	
	void enable();
	
	void disable();
	
	Optional<Boolean> isEnabled();
	
	Optional<Boolean> isCarConnected();
	
}
