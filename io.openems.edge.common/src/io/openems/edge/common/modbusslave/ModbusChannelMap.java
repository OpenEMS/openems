package io.openems.edge.common.modbusslave;

import io.openems.edge.common.channel.doc.ChannelId;

class ModbusChannelMap {
	final int offset;
	private final ChannelId channelId;
	private final ModbusType type;

	public ModbusChannelMap(int offset, ChannelId channelId, ModbusType type) {
		this.offset = offset;
		this.channelId = channelId;
		this.type = type;
	}

	public int getOffset() {
		return offset;
	}

	public ChannelId getChannelId() {
		return channelId;
	}

	public ModbusType getType() {
		return type;
	}
}