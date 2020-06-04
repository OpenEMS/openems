package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.ess.generic.symmetric.Config;
import io.openems.edge.ess.generic.symmetric.GenericManagedSymmetricEss;

public class Context {
	protected final GenericManagedSymmetricEss component;
	protected final Battery battery;
	protected final ManagedSymmetricBatteryInverter batteryInverter;
	protected final Config config;

	public Context(GenericManagedSymmetricEss component, Battery battery,
			ManagedSymmetricBatteryInverter batteryInverter, Config config) {
		super();
		this.component = component;
		this.battery = battery;
		this.batteryInverter = batteryInverter;
		this.config = config;
	}
}