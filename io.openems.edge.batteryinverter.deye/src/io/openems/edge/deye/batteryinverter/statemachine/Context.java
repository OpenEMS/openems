package io.openems.edge.deye.batteryinverter.statemachine;

import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.deye.batteryinverter.BatteryInverterDeyeImpl;
import io.openems.edge.deye.batteryinverter.Config;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryInverterDeyeImpl> {

	protected final Config config;
	protected final OffGridBatteryInverter.TargetGridMode targetGridMode;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(BatteryInverterDeyeImpl parent, Config config, OffGridBatteryInverter.TargetGridMode targetGridMode, int setActivePower,
			int setReactivePower) {
		super(parent);
		this.config = config;
		this.targetGridMode = targetGridMode;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}

}