package io.openems.edge.evse.api.chargepoint.dummy;

import static io.openems.edge.common.test.TestUtils.withValue;

import io.openems.common.types.MeterType;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.meter.test.AbstractDummyElectricityMeter;

public abstract class AbstractDummyEvseChargePoint<SELF extends AbstractDummyEvseChargePoint<?>>
		extends AbstractDummyElectricityMeter<SELF> implements EvseChargePoint {

	private ChargeParams chargeParams;
	private PhaseRotation phaseRotation;

	protected AbstractDummyEvseChargePoint(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}

	/**
	 * Set the {@link ChargeParams}.
	 * 
	 * @param chargeParams the {@link ChargeParams}
	 * @return myself
	 */
	public SELF withChargeParams(ChargeParams chargeParams) {
		this.chargeParams = chargeParams;
		return this.self();
	}

	@Override
	public ChargeParams getChargeParams() {
		return this.chargeParams;
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
}
