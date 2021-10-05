package io.openems.edge.ess.generic.offgrid.statemachine;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;

public class Context extends AbstractContext<GenericManagedEss> {

	protected final Battery battery;
	protected final OffGridBatteryInverter batteryInverter;
	protected final OffGridSwitch offGridSwitch;
	protected final ComponentManager componentManager;

	public Context(GenericManagedEss parent, Battery battery, OffGridBatteryInverter batteryInverter,
			OffGridSwitch offGridSwitch, ComponentManager componentManager) {
		super(parent);
		this.battery = battery;
		this.batteryInverter = batteryInverter;
		this.offGridSwitch = offGridSwitch;
		this.componentManager = componentManager;
	}
}
