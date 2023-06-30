package io.openems.edge.evcs.test;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

public class DummyManagedEvcs extends AbstractManagedEvcsComponent
		implements Evcs, ManagedEvcs, OpenemsComponent, EventHandler {

	private final EvcsPower evcsPower;
	private int minimumHardwarePower = Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
	private int maximumHardwarePower = Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER;

	/**
	 * Constructor.
	 * 
	 * @param id        id
	 * @param evcsPower evcs power
	 */
	public DummyManagedEvcs(String id, EvcsPower evcsPower) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values() //
		);
		this.evcsPower = evcsPower;
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	/**
	 * Constructor.
	 * 
	 * @param id                   id
	 * @param evcsPower            evcs power
	 * @param minimumHardwarePower minimum hardware power
	 * @param maximumHardwarePower minimum hardware power
	 */
	public DummyManagedEvcs(String id, EvcsPower evcsPower, int minimumHardwarePower, int maximumHardwarePower) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values() //
		);
		this.evcsPower = evcsPower;
		this.minimumHardwarePower = minimumHardwarePower;
		this.maximumHardwarePower = maximumHardwarePower;
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		switch (event.getTopic()) {
		// Results of the written limits are checked after write in the Dummy Component
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			this.updateCurrentState();
		}
	}

	private void updateCurrentState() {
		Status nextStatus = this.getStatusChannel().getNextValue().asEnum();
		int nextSetChargePowerLimitValue = this.getSetChargePowerLimitChannel().getNextValue().orElse(0);

		// Status simulatedEvcsStatus = nextStatus;
		// State CHARING_FINISHED cannot be reached in the Dummy for now because the
		// read data is missing.

		// Try to simulate the calculate status given by the EVCS
		if (nextSetChargePowerLimitValue <= 0) {
			nextStatus = Status.CHARGING_REJECTED;
		} else {
			nextStatus = Status.CHARGING;
		}

		/*
		 * Check if the maximum energy limit is reached, informs the user and sets the
		 * status. Attention: Even if the state is already set in the WriteHandler, the
		 * read state of an EVCS could be CHARGING and could override this state in its
		 * read handler
		 */
		int limit = this.getSetEnergyLimit().orElse(0);
		int energy = this.getEnergySession().orElse(0);
		if (energy >= limit && limit != 0) {
			nextStatus = Status.ENERGY_LIMIT_REACHED;
		}

		this._setStatus(nextStatus);
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return false;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		this._setChargePower(power);
		this._setStatus(Status.CHARGING);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		this._setChargePower(0);
		return true;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		// Default is one second, to avoid using everywhere a ClockProvider only for
		// test-cases.
		return 1;
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.minimumHardwarePower;
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.maximumHardwarePower;
	}
}
