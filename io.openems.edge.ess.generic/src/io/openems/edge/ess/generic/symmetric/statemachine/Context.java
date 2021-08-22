package io.openems.edge.ess.generic.symmetric.statemachine;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ess.generic.common.GenericManagedEss;

public class Context extends AbstractContext<GenericManagedEss> {

	protected final Battery battery;
	protected final ManagedSymmetricBatteryInverter batteryInverter;

	public Context(GenericManagedEss parent, Battery battery, ManagedSymmetricBatteryInverter batteryInverter) {
		super(parent);
		this.battery = battery;
		this.batteryInverter = batteryInverter;
	}
}