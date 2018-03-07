package io.openems.edge.api.device.io;

import java.util.Optional;

import io.openems.edge.api.device.DeviceInterface;


public interface DigitalOutputInterface extends DeviceInterface{
	
	Optional<Boolean> getOutputState(int index);
	
	void setOutputOn(int index);
	
	void setOutputOff(int index);
}
