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
	 * Generic ess has faults.
	 * 
	 * <p>
	 * Check for any faults in the generic ess and its dependent battery or battery
	 * inverter.
	 * 
	 * @return true on any failure
	 */
	public boolean hasEssFaults() {
		return this.getParent().hasFaults() || this.battery.hasFaults() || this.batteryInverter.hasFaults();
	}

	/**
	 * Is generic ess started.
	 * 
	 * <p>
	 * Generic ess is started when battery and battery-inverter started.
	 * 
	 * @return true if battery and battery-inverter started
	 */
	public boolean isEssStarted() {
		return this.battery.isStarted() && this.batteryInverter.isStarted();
	}

	/**
	 * Is generic ess stopped.
	 * 
	 * <p>
	 * Generic ess is stopped when at least the battery stopped. In many cases the
	 * BatteryInverter is not able to not stop.
	 * 
	 * @return true if the system stopped.
	 */
	public boolean isEssStopped() {
		return this.battery.isStopped();
	}
}
