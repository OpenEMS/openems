package io.openems.edge.api.device.io;

import java.util.Optional;

import io.openems.edge.api.device.DeviceInterface;


public interface DigitalInputInterface  extends DeviceInterface{
	Optional<Boolean> getInputState(int index);
}
