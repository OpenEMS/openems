package io.openems.edge.bridge.modbus.channel;

import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class ModbusLongReadChannel extends LongReadChannel implements ModbusChannel<Long> {

	public ModbusLongReadChannel(OpenemsComponent component, ChannelId channelId) {
		super(component, channelId);
	}

}
