package io.openems.edge.batteryinverter.refu88k.statemachine;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.refu88k.Config;
import io.openems.edge.batteryinverter.refu88k.BatteryInverterRefuStore88k;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryInverterRefuStore88k> {

	protected final Battery battery;
	protected final Config config;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(BatteryInverterRefuStore88k parent, Battery battery, Config config, int setActivePower,
			int setReactivePower) {
		super(parent);
		this.battery = battery;
		this.config = config;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}
}