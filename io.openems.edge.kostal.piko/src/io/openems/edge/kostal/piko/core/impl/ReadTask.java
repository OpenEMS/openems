package io.openems.edge.kostal.piko.core.impl;

import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.Task;

public class ReadTask extends Task {

	private final OpenemsComponent component;
	private final ChannelId channelId;
	private final FieldType fieldType;
	private final int address;

	public ReadTask(OpenemsComponent component, ChannelId channelId, Priority priority, FieldType fieldType,
			int address) {
		super(priority);
		this.component = component;
		this.channelId = channelId;
		this.address = address;
		this.fieldType = fieldType;
	}

	public OpenemsComponent getComponent() {
		return component;
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
