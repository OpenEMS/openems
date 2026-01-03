package io.openems.edge.powerplantcontrol.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.powerplantcontrol.api.ManagedPowerPlantController;

public class DummyManagedPowerPlantController extends AbstractDummyOpenemsComponent<DummyManagedPowerPlantController>
implements OpenemsComponent, ManagedPowerPlantController {

	public DummyManagedPowerPlantController(String id) { 
			super(id, //
					OpenemsComponent.ChannelId.values(),//
					ManagedPowerPlantController.ChannelId.values());
	}

	@Override
	protected DummyManagedPowerPlantController self() {
		return this;
	}
}
