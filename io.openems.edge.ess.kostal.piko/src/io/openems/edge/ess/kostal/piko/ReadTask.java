package io.openems.edge.ess.kostal.piko;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.ChannelId;

public class ReadTask {

	private final ChannelId channelId;
	private final Priority priority;
	private final FieldType fieldType;
	private final int address;

	public ReadTask(ChannelId channelId, Priority priority, FieldType fieldType, int address) {
		this.channelId = channelId;
		this.priority = priority;
		this.address = address;
		this.fieldType = fieldType;
	}

	public ChannelId getChannelId() {
		return channelId;
	}

	public Priority getPriority() {
		return this.priority;
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	public int getAddress() {
		return address;
	}

}
