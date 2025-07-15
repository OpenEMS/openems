package io.openems.edge.evse.api.chargepoint.test;

import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

public abstract class AbstractDummyElectricVehicle<SELF extends AbstractDummyElectricVehicle<?>>
		extends AbstractDummyOpenemsComponent<SELF> implements EvseElectricVehicle {

	private ElectricVehicleAbilities electricVehicleAbilities;

	protected AbstractDummyElectricVehicle(String id, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(id, firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Set {@link ElectricVehicleAbilities}.
	 *
	 * @param electricVehicleAbilities the {@link ElectricVehicleAbilities}
	 * @return myself
	 */
	public final SELF withElectricVehicleAbilities(ElectricVehicleAbilities electricVehicleAbilities) {
		this.electricVehicleAbilities = electricVehicleAbilities;
		return this.self();
	}

	@Override
	public ElectricVehicleAbilities getElectricVehicleAbilities() {
		return this.electricVehicleAbilities;
	}
}
