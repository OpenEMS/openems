package io.openems.edge.evcs.test;

import static io.openems.common.types.MeterType.MANAGED_CONSUMPTION_METERED;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

// TODO should extend AbstractDummyElectricityMeter<DummyManagedEvcs>
public class DummyManagedEvcs extends AbstractManagedEvcsComponent
		implements Evcs, ManagedEvcs, ElectricityMeter, OpenemsComponent, EventHandler {

	private final EvcsPower evcsPower;
	private int minimumHardwarePower = Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
	private int maximumHardwarePower = Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER;
	private MeterType meterType = MANAGED_CONSUMPTION_METERED;

	/**
	 * Instantiates a disabled {@link DummyManagedEvcs}.
	 * 
	 * @param id the Component-ID
	 * @return a new {@link DummyManagedEvcs}
	 */
	public static DummyManagedEvcs ofDisabled(String id) {
		return new DummyManagedEvcs(id, new DummyEvcsPower(new DisabledRampFilter()), false);
	}

	public DummyManagedEvcs(String id, EvcsPower evcsPower) {
		this(id, evcsPower, true);
	}

	private DummyManagedEvcs(String id, EvcsPower evcsPower, boolean isEnabled) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values() //
		);
		this.evcsPower = evcsPower;
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", isEnabled);
	}

	/**
	 * Set the {@link MeterType}.
	 *
	 * @param meterType the meterType
	 * @return myself
	 */
	public DummyManagedEvcs withMeterType(MeterType meterType) {
		this.meterType = meterType;
		return this;
	}

	/**
	 * Set {@link ElectricityMeter.ChannelId#ACTIVE_POWER}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyManagedEvcs withActivePower(Integer value) {
		TestUtils.withValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, value);
		return this;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
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
	public MeterType getMeterType() {
		return this.meterType;
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
		this._setActivePower(power);
		this._setStatus(Status.CHARGING);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		this._setActivePower(0);
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
