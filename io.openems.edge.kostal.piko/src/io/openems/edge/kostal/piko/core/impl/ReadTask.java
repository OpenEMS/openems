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
	private final int skipCycles = 0;

	public ReadTask(OpenemsComponent component, ChannelId channelId, Priority priority, FieldType fieldType,
			int address) {
		this.component = component;
		this.channelId = channelId;
		this.priority = priority;
		this.address = address;
		this.fieldType = fieldType;
	}

	public OpenemsComponent getComponent() {
		return this.component;
	}

	public ChannelId getChannelId() {
		return this.channelId;
	}

	public FieldType getFieldType() {
		return this.fieldType;
	}

	public int getAddress() {
		return this.address;
	}

	@Override
	public Priority getPriority() {
		return this.priority;
	}

	public int getSkipCycles() {
		return this.skipCycles;
	}

	@Override
	public String toString() {
		return "ReadTask [channelId=" + this.channelId + ", priority=" + this.priority + ", fieldType=" + this.fieldType
				+ ", address=" + this.address + "]";
	}

}
