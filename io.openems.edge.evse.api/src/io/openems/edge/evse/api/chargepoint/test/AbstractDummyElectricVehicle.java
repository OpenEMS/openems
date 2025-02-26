package io.openems.edge.evse.api.chargepoint.test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;

public abstract class AbstractDummyElectricVehicle<SELF extends AbstractDummyElectricVehicle<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements EvseElectricVehicle {

	private ChargeParams chargeParams = new ChargeParams(ImmutableList.of(), ImmutableList.of());

	protected AbstractDummyElectricVehicle(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Set {@link ChargeParams}s.
	 *
	 * @param value the value
	 * @return myself
	 */
	public final SELF withChargeParams(ChargeParams value) {
		this.chargeParams = value;
		return this.self();
	}

	public ChargeParams getChargeParams() {
		return this.chargeParams;
	}
}
