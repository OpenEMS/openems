package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.Channel;

public class ChannelRecord {

	private Channel<?> channel;
	private int dataRecordPosition;

	public enum DataType {
		Manufacturer, DeviceId, MeterType
	}

	public DataType dataType;

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
	}

	public Channel<?> getChannel() {
		return this.channel;
	}

	public void setChannelId(Channel<?> channel) {
		this.channel = channel;
	}

	public int getdataRecordPosition() {
		return this.dataRecordPosition;
	}

	public void setdataRecordPosition(int dataRecordPosition) {
		this.dataRecordPosition = dataRecordPosition;
	}

	public DataType getDataType() {
		return this.dataType;
	}

}
