package io.openems.impl.device.byd;

import io.openems.api.channel.thingstate.WarningEnum;

public enum Warning implements WarningEnum{
	Status_abnormal_of_AC_surge_protector(0),Close_of_control_switch(1),Emergency_stop(2),Status_abnormal_of_frog_detector(3),Serious_leakage(4),Normal_leakage(5),Failure_of_temperature_sensor_in_control_cabinet(6),Failure_of_humidity_sensor_in_control_cabinet(7),Failure_of_storage_device(8),Exceeding_of_humidity_in_control_cabinet(9);
	private final int value;

	private Warning(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
