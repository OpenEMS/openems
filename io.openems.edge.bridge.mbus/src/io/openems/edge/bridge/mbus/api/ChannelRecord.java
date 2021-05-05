package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.Channel;

/**
 * This class is used as the entries in the channelDataRecordsList. It links a channel to a data record address.
 */

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

	/**
	 * Get the channel associated with this channel record.
	 *
	 * @return the channel.
	 */
	public Channel<?> getChannel() {
		return this.channel;
	}

	/**
	 * Set the channel associated with this channel record.
	 *
	 * @param channel	The channel.
	 */
	public void setChannelId(Channel<?> channel) {
		this.channel = channel;
	}

	/**
	 * Get the data record position associated with this channel record.
	 *
	 * @return the data record position.
	 */
	public int getDataRecordPosition() {
		return this.dataRecordPosition;
	}

	/**
	 * Set the data record position associated with this channel record.
	 *
	 * @param dataRecordPosition	The data record position.
	 */
	public void setDataRecordPosition(int dataRecordPosition) {
		this.dataRecordPosition = dataRecordPosition;
	}

	/**
	 * Get the data type.
	 *
	 * @return the data type.
	 */
	public DataType getDataType() {
		return this.dataType;
	}

}
