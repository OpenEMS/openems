package io.openems.edge.aircooling.blueeplus;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

public class DummyBlueeplusImpl extends AbstractDummyOpenemsComponent<DummyBlueeplusImpl>
		implements BlueEPlus, OpenemsComponent, ModbusComponent {

	public DummyBlueeplusImpl(String id) {
		super(//
				id, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				BlueEPlus.ChannelId.values() //
		);
		for (var channel : this.channels()) {
			channel.nextProcessImage();
		}
	}

	/**
	 * Sets and applies the
	 * {@link BlueEPlus.ChannelId#SETUP_STANDARD_CONTROL_SETPOINT}.
	 *
	 * @param value the int value
	 * @return myself
	 */
	public DummyBlueeplusImpl withSetupStandardControlSetpoint(int value) {
		this.getSetupStandardControlSetpointChannel().setNextValue(value);
		this.getSetupStandardControlSetpointChannel().nextProcessImage();
		return this;
	}

	@Override
	public void retryModbusCommunication() {

	}

	@Override
	protected DummyBlueeplusImpl self() {
		return this;
	}

}
