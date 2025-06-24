package io.openems.edge.evse.api.chargepoint.dummy;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Provides a simple, simulated {@link EvseChargePoint} component that can be
 * used together with the OpenEMS Component test framework.
 */
public class DummyEvseChargePoint extends AbstractDummyEvseChargePoint<DummyEvseChargePoint>
		implements EvseChargePoint, OpenemsComponent {

	public DummyEvseChargePoint(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				ElectricityMeter.ChannelId.values());
	}

	@Override
	protected final DummyEvseChargePoint self() {
		return this;
	}

	@Override
	public void apply(ChargePointActions actions) {
		// TODO Auto-generated method stub
	}
}
