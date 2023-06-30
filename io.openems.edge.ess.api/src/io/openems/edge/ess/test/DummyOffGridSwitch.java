package io.openems.edge.ess.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.offgrid.api.OffGridSwitch;

/**
 * Provides a simple, simulated OffGridSwitch component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyOffGridSwitch extends AbstractOpenemsComponent implements OffGridSwitch {

	public DummyOffGridSwitch(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				OffGridSwitch.ChannelId.values() //
		);
		super.activate(null, id, "", true);
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
