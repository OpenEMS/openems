package io.openems.edge.ess.generic.common.statemachine;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.ess.generic.common.GenericManagedEss;

public class Context {
	protected final GenericManagedEss component;
	protected final Battery battery;
	protected final ManagedSymmetricBatteryInverter batteryInverter;

	public Context(GenericManagedEss component, Battery battery, ManagedSymmetricBatteryInverter batteryInverter) {
		super();
		this.component = component;
		this.battery = battery;
		this.batteryInverter = batteryInverter;
	}
}