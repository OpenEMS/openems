package io.openems.edge.batteryinverter.refu.statemachine;

import java.time.Clock;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.refu.BatteryInverterRefuImpl;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryInverterRefuImpl> {

	protected final Battery battery;
	protected final int setActivePower;
	protected final int setReactivePower;
	protected final Clock clock;

	public Context(BatteryInverterRefuImpl parent, Battery battery, int setActivePower, int setReactivePower,
			Clock clock) {
		super(parent);
		this.battery = battery;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
		this.clock = clock;
	}
}
