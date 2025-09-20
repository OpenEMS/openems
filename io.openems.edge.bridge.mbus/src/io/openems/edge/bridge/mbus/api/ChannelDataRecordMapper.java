package io.openems.edge.bridge.mbus.api;

import java.util.List;

import org.openmuc.jmbus.VariableDataStructure;

import io.openems.edge.bridge.mbus.api.ChannelRecord.DataType;
import io.openems.edge.common.channel.Channel;

public class ChannelDataRecordMapper {
	protected VariableDataStructure data;

	protected List<ChannelRecord> channelDataRecordsList;

	public ChannelDataRecordMapper(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {
		this.data = data;
		this.channelDataRecordsList = channelDataRecordsList;

		for (ChannelRecord channelRecord : channelDataRecordsList) {
			this.mapDataToChannel(data, channelRecord.dataRecordPosition, channelRecord.channel,
					channelRecord.dataType);
		}
	}

	public VariableDataStructure getData() {
		return this.data;
	}

	public void setData(VariableDataStructure data) {
		this.data = data;
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}

	public void setChannelDataRecordsList(List<ChannelRecord> channelDataRecordsList) {
		this.channelDataRecordsList = channelDataRecordsList;
	}

	protected void mapDataToChannel(VariableDataStructure data, int index, Channel<?> channel, DataType dataType) {

		if (dataType == null) {
			if (data.getDataRecords().size() > index && index >= 0) {
				channel.setNextValue(data.getDataRecords().get(index).getScaledDataValue());
			}
			return;
		}
		switch (dataType) {
		case Manufacturer:
			channel.setNextValue(data.getSecondaryAddress().getManufacturerId());
			break;
		case DeviceId:
			channel.setNextValue(data.getSecondaryAddress().getDeviceId());
			break;
		case MeterType:
			channel.setNextValue(data.getSecondaryAddress().getDeviceType());
			break;
		}
	}

}
