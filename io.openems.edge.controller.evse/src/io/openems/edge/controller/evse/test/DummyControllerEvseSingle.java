package io.openems.edge.controller.evse.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;

/**
 * Provides a simple, simulated {@link ControllerEvseSingle} component that can
 * be used together with the OpenEMS Component test framework.
 */
public class DummyControllerEvseSingle extends AbstractDummyControllerEvseSingle<DummyControllerEvseSingle>
		implements ControllerEvseSingle, OpenemsComponent {

	public DummyControllerEvseSingle(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ControllerEvseSingle.ChannelId.values());
	}

	@Override
	protected final DummyControllerEvseSingle self() {
		return this;
	}

	@Override
	public void apply(Mode mode, ChargePointActions actions) {
		// do nothing
	}
}