package io.openems.edge.powercontrolunit.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.powercontrolunit.api.PowerControlUnit;

public class DummyPowerControlUnit extends AbstractDummyOpenemsComponent<DummyPowerControlUnit>
implements OpenemsComponent, PowerControlUnit {

	public DummyPowerControlUnit(String id) {
			super(id, //
					OpenemsComponent.ChannelId.values(),//
					PowerControlUnit.ChannelId.values());
	}

	@Override
	protected DummyPowerControlUnit self() {
		return this;
	}
}
