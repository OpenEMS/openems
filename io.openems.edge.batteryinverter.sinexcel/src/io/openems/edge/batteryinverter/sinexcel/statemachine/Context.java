package io.openems.edge.batteryinverter.sinexcel.statemachine;

import io.openems.edge.batteryinverter.api.OffGridBatteryInverter.TargetGridMode;
import io.openems.edge.batteryinverter.sinexcel.Config;
import io.openems.edge.batteryinverter.sinexcel.BatteryInverterSinexcelImpl;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryInverterSinexcelImpl> {

	protected final Config config;
	protected final TargetGridMode targetGridMode;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(BatteryInverterSinexcelImpl parent, Config config, TargetGridMode targetGridMode, int setActivePower,
			int setReactivePower) {
		super(parent);
		this.config = config;
		this.targetGridMode = targetGridMode;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}

}