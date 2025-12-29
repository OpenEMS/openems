package io.openems.edge.victron.batteryinverter.statemachine;

import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.victron.batteryinverter.Config;
import io.openems.edge.victron.batteryinverter.VictronBatteryInverterImpl;

public class Context extends AbstractContext<VictronBatteryInverterImpl> {

	protected final Config config;
	protected final TargetGridMode targetGridMode;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(VictronBatteryInverterImpl parent, Config config, TargetGridMode targetGridMode, int setActivePower,
			int setReactivePower) {
		super(parent);
		this.config = config;
		this.targetGridMode = targetGridMode;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}

}