package io.openems.edge.bridge.modbus.channel;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.ChannelDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class ModbusIntegerReadChannel extends IntegerReadChannel implements ModbusChannel<Integer> {

	public ModbusIntegerReadChannel(OpenemsComponent component, ChannelDoc channelDoc) {
		super(component, channelDoc);
	}

}
