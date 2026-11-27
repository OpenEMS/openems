package io.openems.edge.firealarmsystem.hekatron;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

public class DummyHekatronB9X2Impl extends AbstractDummyOpenemsComponent<DummyHekatronB9X2Impl>
		implements HekatronB9X2, OpenemsComponent, ModbusComponent {

	public DummyHekatronB9X2Impl(String id) {
		super(//
				id, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				HekatronB9X2.ChannelId.values() //
		);
		for (var channel : this.channels()) {
			channel.nextProcessImage();
		}
	}

	@Override
	public void retryModbusCommunication() {

	}

	@Override
	protected DummyHekatronB9X2Impl self() {
		return this;
	}

}
