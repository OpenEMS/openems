package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.Channel;

public class ChannelRecord {

	public enum DataType {
		Manufacturer, DeviceId, MeterType
	}

	public final DataType dataType;
	public final Channel<?> channel;
	public final int dataRecordPosition;

	/**
	 * In this case you will request secondary address values. eg. manufacturer,
	 * device id or meter type.
	 *
	 * @param channel  the Channel
	 * @param dataType the dataType
	 */
	public ChannelRecord(Channel<?> channel, DataType dataType) {
		this.channel = channel;
		this.dataType = dataType;
		this.dataRecordPosition = 0;
	}

	/**
	 * In this case you will request usage data.
	 *
	 * @param channel            the Channel
	 * @param dataRecordPosition the dataRecordPosition
	 */
	public ChannelRecord(Channel<?> channel, int dataRecordPosition) {
		this.channel = channel;
		this.dataRecordPosition = dataRecordPosition;
		this.dataType = null;
	}

}
