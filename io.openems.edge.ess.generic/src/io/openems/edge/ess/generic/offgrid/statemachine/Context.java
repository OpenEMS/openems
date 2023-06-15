package io.openems.edge.ess.generic.offgrid.statemachine;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ess.generic.common.GenericManagedEss;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;
import io.openems.edge.ess.offgrid.api.OffGridSwitch.Contactor;

public class Context extends AbstractContext<GenericManagedEss> {

	protected final Battery battery;
	protected final OffGridBatteryInverter batteryInverter;
	protected final OffGridSwitch offGridSwitch;
	protected final ComponentManager componentManager;
	private final AtomicBoolean fromOffToOnGrid;

	public Context(GenericManagedEss parent, Battery battery, OffGridBatteryInverter batteryInverter,
			OffGridSwitch offGridSwitch, ComponentManager componentManager, AtomicBoolean isFromOffToOnGrid) {
		super(parent);
		this.battery = battery;
		this.batteryInverter = batteryInverter;
		this.offGridSwitch = offGridSwitch;
		this.componentManager = componentManager;
		this.fromOffToOnGrid = isFromOffToOnGrid;
	}

	/**
	 * Proof if all {@link OffGridSwitch channels are present}.
	 * 
	 * @return true if all defined.
	 */
	public boolean isChannelsDefined() {
		return Stream.of(//
				this.offGridSwitch.getMainContactor(), //
				this.offGridSwitch.getGroundingContactor())//
				.allMatch(Optional::isPresent);
	}

	/**
	 * Checks if contactors set properly for on-grid connection.
	 * 
	 * @return true if all contactors adjust as they are required.
	 */
	public boolean isOnGridContactorsSet() {
		var mainContactor = this.offGridSwitch.getMainContactor();
		var groundingContactor = this.offGridSwitch.getGroundingContactor();
		if (mainContactor.isEmpty() || groundingContactor.isEmpty()) {
			return false;
		}
		return mainContactor.get() == Contactor.CLOSE && groundingContactor.get() == Contactor.OPEN;
	}

	/**
	 * Checks if contactors set properly for off-grid connection.
	 * 
	 * @return true if all contactors adjust as they are required.
	 */
	public boolean isOffGridContactorsSet() {
		var mainContactor = this.offGridSwitch.getMainContactor();
		var groundingContactor = this.offGridSwitch.getGroundingContactor();
		if (mainContactor.isEmpty() || groundingContactor.isEmpty()) {
			return false;
		}
		return mainContactor.get() == Contactor.OPEN && groundingContactor.get() == Contactor.CLOSE;
	}

	/**
	 * Set contactors for on-grid connection.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	public void setContactorsForOnGrid() throws OpenemsNamedException {
		this.offGridSwitch.setMainContactor(Contactor.CLOSE);
		this.offGridSwitch.setGroundingContactor(Contactor.OPEN);
	}

	/**
	 * Set contactors for on-grid connection.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	public void setContactorsForOffGrid() throws OpenemsNamedException {
		this.offGridSwitch.setMainContactor(Contactor.OPEN);
		this.offGridSwitch.setGroundingContactor(Contactor.CLOSE);
	}

	/**
	 * Gets the grid state changes from off to on grid in
	 * {@link StartedInOffGridHandler} state.
	 * 
	 * @return true if it goes from off to on grid.
	 */
	public boolean isFromOffToOnGrid() {
		return this.fromOffToOnGrid.get();
	}

	/**
	 * Sets if grid state changes from off to on grid.
	 * 
	 * @param isFromOffToOnGrid value to be set.
	 */
	public void setFromOffToOnGrid(boolean isFromOffToOnGrid) {
		this.fromOffToOnGrid.set(isFromOffToOnGrid);
	}
}
