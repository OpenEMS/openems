package io.openems.edge.ess.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;

/**
 * Provides a simple, simulated {@link OffGridSwitch} component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyOffGridSwitch extends AbstractDummyOpenemsComponent<DummyOffGridSwitch> implements OffGridSwitch {

	public DummyOffGridSwitch(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				OffGridSwitch.ChannelId.values());
	}

	@Override
	protected DummyOffGridSwitch self() {
		return this;
	}

	@Override
	public void setMainContactor(Contactor operation) throws IllegalArgumentException, OpenemsNamedException {
		System.out.println("setMainContactor: " + operation);
	}

	@Override
	public void setGroundingContactor(Contactor operation) throws IllegalArgumentException, OpenemsNamedException {
		System.out.println("setGroundingContactor: " + operation);
	}

}
