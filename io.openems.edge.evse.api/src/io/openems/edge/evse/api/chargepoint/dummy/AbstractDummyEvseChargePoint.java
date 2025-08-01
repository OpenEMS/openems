package io.openems.edge.evse.api.chargepoint.dummy;

import static io.openems.edge.common.test.TestUtils.withValue;

import io.openems.common.types.MeterType;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.meter.test.AbstractDummyElectricityMeter;

public abstract class AbstractDummyEvseChargePoint<SELF extends AbstractDummyEvseChargePoint<?>>
		extends AbstractDummyElectricityMeter<SELF> implements EvseChargePoint {

	private ChargePointAbilities chargePointAbilities;
	private PhaseRotation phaseRotation;
	private boolean isReadOnly;

	protected AbstractDummyEvseChargePoint(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Set the {@link ChargePointAbilities}.
	 * 
	 * @param chargePointAbilities the {@link ChargePointAbilities}
	 * @return myself
	 */
	public SELF withChargePointAbilities(ChargePointAbilities chargePointAbilities) {
		this.chargePointAbilities = chargePointAbilities;
		return this.self();
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		return this.chargePointAbilities;
	}

	/**
	 * Set the {@link PhaseRotation}.
	 * 
	 * @param phaseRotation the {@link PhaseRotation}
	 * @return myself
	 */
	public SELF withPhaseRotation(PhaseRotation phaseRotation) {
		this.phaseRotation = phaseRotation;
		return this.self();
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.phaseRotation;
	}

	/**
	 * Set the {@link EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING}.
	 * 
	 * @param isReadyForCharging the value
	 * @return myself
	 */
	public SELF withIsReadyForCharging(Boolean isReadyForCharging) {
		withValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReadyForCharging);
		return this.self();
	}

	/**
	 * Set isReadOnly.
	 * 
	 * @param value the value
	 * @return myself
	 */
	public SELF withIsReadOnly(boolean value) {
		this.isReadOnly = value;
		return this.self();
	}

	@Override
	public boolean isReadOnly() {
		return this.isReadOnly;
	}

	@Override
	public MeterType getMeterType() {
		return EvseChargePoint.super.getMeterType();
	}
}
