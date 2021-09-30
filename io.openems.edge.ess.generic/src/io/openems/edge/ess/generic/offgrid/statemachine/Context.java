package io.openems.edge.ess.generic.offgrid.statemachine;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;

public class Context extends AbstractContext<GenericManagedEss> {

	protected final Battery battery;
	protected final OffGridBatteryInverter batteryInverter;
	protected final OffGridSwitch offGridSwitch;

	public Context(GenericManagedEss parent, Battery battery, OffGridBatteryInverter batteryInverter,
			OffGridSwitch offGridSwitch) {
		super(parent);
		this.battery = battery;
		this.batteryInverter = batteryInverter;
		this.offGridSwitch = offGridSwitch;
	}
}
