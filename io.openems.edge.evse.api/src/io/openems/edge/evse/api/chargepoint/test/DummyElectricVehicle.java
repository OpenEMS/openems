package io.openems.edge.evse.api.chargepoint.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;

/**
 * Provides a simple, simulated {@link EvseElectricVehicle} component that can
 * be used together with the OpenEMS Component test framework.
 */
public class DummyElectricVehicle extends AbstractDummyElectricVehicle<DummyElectricVehicle>
		implements EvseElectricVehicle, OpenemsComponent {

	public DummyElectricVehicle(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				EvseElectricVehicle.ChannelId.values());
	}

	@Override
	protected final DummyElectricVehicle self() {
		return this;
	}
}
