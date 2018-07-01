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

	public Priority getPriority() {
		return this.priority;
	}

	public void execute(EssKostalPiko parent) {
		try {
			Channel<?> channel = parent.channel(this.channelId);
			switch (this.fieldType) {
			case STRING:
				channel.setNextValue(parent.getStringValue(this.address));
				break;
			case INTEGER:
				channel.setNextValue(parent.getIntegerValue(this.address));
				break;
			case BOOLEAN:
				channel.setNextValue(parent.getBooleanValue(this.address));
				break;
			case INTEGER_UNSIGNED_BYTE:
				channel.setNextValue(parent.getIntegerFromUnsignedByte(this.address));
				break;
			case FLOAT:
				channel.setNextValue(parent.getFloatValue(this.address));
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
