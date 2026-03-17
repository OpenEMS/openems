package io.openems.edge.powerplantcontrol.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.powerplantcontrol.api.PowerControlUnit;

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
