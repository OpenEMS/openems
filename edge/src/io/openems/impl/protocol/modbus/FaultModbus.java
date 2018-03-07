package io.openems.impl.protocol.modbus;

import io.openems.api.channel.thingstate.FaultEnum;
import io.openems.common.types.ThingStateInfo;

@ThingStateInfo(reference = { ModbusRtu.class, ModbusTcp.class })
public enum FaultModbus implements FaultEnum {
	ConfigurationFault(0), //
	ConnectionFault(1);

	private final int value;

	private FaultModbus(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}
}
