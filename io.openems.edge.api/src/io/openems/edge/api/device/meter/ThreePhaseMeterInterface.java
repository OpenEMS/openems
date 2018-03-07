package io.openems.edge.api.device.meter;

import java.util.Optional;

import io.openems.edge.api.device.DeviceInterface;


public interface ThreePhaseMeterInterface extends DeviceInterface {

	Optional<Long> getActivePowerL1();

	Optional<Long> getActivePowerL2();

	Optional<Long> getActivePowerL3();

	Optional<Long> getReactivePowerL1();

	Optional<Long> getReactivePowerL2();

	Optional<Long> getReactivePowerL3();

	Optional<Long> getCurrentL1();

	Optional<Long> getCurrentL2();

	Optional<Long> getCurrentL3();

	Optional<Long> getVoltageL1();

	Optional<Long> getVoltageL2();

	Optional<Long> getVoltageL3();

	Optional<Long> getFrequencyL1();

	Optional<Long> getFrequencyL2();

	Optional<Long> getFrequencyL3();
}
