package io.openems.edge.ess.kostal.piko;

import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.Task;

public class ReadTask extends Task {

	private final ChannelId channelId;
	private final FieldType fieldType;
	private final int address;

	public ReadTask(ChannelId channelId, Priority priority, FieldType fieldType, int address) {
		super(priority);
		this.channelId = channelId;
		this.address = address;
		this.fieldType = fieldType;
	}

	public ChannelId getChannelId() {
		return channelId;
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	public int getAddress() {
		return address;
	}

}
