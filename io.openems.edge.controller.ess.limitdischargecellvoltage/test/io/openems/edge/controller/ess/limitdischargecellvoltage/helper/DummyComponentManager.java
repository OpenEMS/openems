package io.openems.edge.controller.ess.limitdischargecellvoltage.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class DummyComponentManager extends io.openems.edge.common.test.DummyComponentManager
		implements ComponentManager {

	private ManagedSymmetricEss ess = createEss();

	@SuppressWarnings("unchecked")
	@Override
	public <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException {
		if (CreateTestConfig.ESS_ID.equals(componentId)) {
			return (T) ess;
		}
		return null;
	}

	private ManagedSymmetricEss createEss() {

		return new DummyEss(OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values() // ;
		);
	}

	public void destroyEss() {
		this.ess = null;
	}
}
