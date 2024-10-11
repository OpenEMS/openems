package io.openems.edge.ess.generic.symmetric.statemachine;

import java.time.Clock;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ess.generic.common.GenericManagedEss;

public class Context extends AbstractContext<GenericManagedEss> {

	protected final Battery battery;
	protected final ManagedSymmetricBatteryInverter batteryInverter;
	protected final Clock clock;

	public Context(GenericManagedEss parent, Battery battery, ManagedSymmetricBatteryInverter batteryInverter,
			Clock clock) {
		super(parent);
		this.battery = battery;
		this.batteryInverter = batteryInverter;
		this.clock = clock;
	}

	/**
	 * Does Ess, Battery or BatteryInverter have any failure?.
	 * 
	 * @return true on any failure
	 */
	public boolean hasFaults() {
		return this.getParent().hasFaults() || this.battery.hasFaults() || this.batteryInverter.hasFaults();
	}

	/**
	 * Are Battery and BatteryInverter started?.
	 * 
	 * @return true if battery and battery-inverter are started
	 */
	public boolean isStarted() {
		return this.battery.isStarted() && this.batteryInverter.isStarted();
	}

	/**
	 * Are Ess and Battery stopped.
	 * 
	 * @return true if there is no DC voltage.
	 */
	public boolean isStopped() {
		return this.getParent().isStopped() || this.battery.isStopped();
	}
}