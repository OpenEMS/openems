package io.openems.edge.kostal.piko.core.impl;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;

public class ReadTask implements ManagedTask {

	private final OpenemsComponent component;
	private final ChannelId channelId;
	private final Priority priority;
	private final FieldType fieldType;
	private final int address;

	public ReadTask(OpenemsComponent component, ChannelId channelId, Priority priority, FieldType fieldType,
			int address) {
		this.component = component;
		this.channelId = channelId;
		this.priority = priority;
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

	@Override
	public Priority getPriority() {
		return this.priority;
	}

	@Override
	public String toString() {
		return "ReadTask [channelId=" + channelId + ", priority=" + priority + ", fieldType=" + fieldType + ", address="
				+ address + "]";
	}

}
