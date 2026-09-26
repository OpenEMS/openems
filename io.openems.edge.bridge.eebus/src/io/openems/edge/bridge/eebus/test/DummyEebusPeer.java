package io.openems.edge.bridge.eebus.test;

import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.api.EebusPeer;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

public class DummyEebusPeer extends AbstractDummyOpenemsComponent<DummyEebusPeer> implements EebusPeer {
	protected DummyEebusPeer(String id) {
		super(//
				id, //
				OpenemsComponent.ChannelId.values(), //
				BridgeEebus.ChannelId.values() //
		);
	}

	@Override
	public String getSki() {
		return "DUMMY";
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	protected DummyEebusPeer self() {
		return this;
	}
}
