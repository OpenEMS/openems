package io.openems.edge.ess.generic.symmetric.statemachine;

import java.time.Clock;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.generic.symmetric.essfaultbehaviour.EssFaultBehaviourConfig;

public class Context extends AbstractContext<GenericManagedEss> {

	public final Battery battery;
	public final ManagedSymmetricBatteryInverter batteryInverter;

	protected final Clock clock;

	private final EssFaultBehaviourConfig essFaultBehaviour;

	public Context(GenericManagedEss parent, Battery battery, ManagedSymmetricBatteryInverter batteryInverter,
			Clock clock, EssFaultBehaviourConfig essFaultBehaviour) {
		super(parent);
		this.battery = battery;
		this.batteryInverter = batteryInverter;
		this.clock = clock;
		this.essFaultBehaviour = essFaultBehaviour;
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
		return this.essFaultBehaviour.hasEssError(this);
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
		return this.essFaultBehaviour.isEssStarted(this);
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
