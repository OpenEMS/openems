package io.openems.edge.controller.battery.batteryprotection.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class DummyComponentManager extends io.openems.edge.common.test.DummyComponentManager
		implements ComponentManager {

	private ManagedSymmetricEss ess = createEss();
	private Battery bms = createBms();

	@SuppressWarnings("unchecked")
	@Override
	public <T extends OpenemsComponent> T getComponent(String componentId) throws OpenemsNamedException {
		if (Creator.ESS_ID.equals(componentId)) {
			return (T) ess;
		}
		if (Creator.BMS_ID.equals(componentId)) {
			return (T) bms;
		}
		return null;
	}

	private Battery createBms() {
		return new DummyBattery();
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

	public void initEss() {
		this.ess = createEss();
	}

	public void destroyBms() {
		this.bms = null;
	}

	public void initBms() {
		this.bms = createBms();
	}

}
