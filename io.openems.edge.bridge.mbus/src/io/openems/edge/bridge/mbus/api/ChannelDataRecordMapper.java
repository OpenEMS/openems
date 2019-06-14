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
		
		for(ChannelRecord channelRecord: channelDataRecordsList) {
			mapDataToChannel(data, channelRecord.getdataRecordPosition(), channelRecord.getChannel(), channelRecord.getDataType());
		}
		
	}
	
	
	public VariableDataStructure getData() {
		return data;
	}


	public void setData(VariableDataStructure data) {
		this.data = data;
	}



	public List<ChannelRecord> getChannelDataRecordsList() {
		return channelDataRecordsList;
	}

	public void setChannelDataRecordsList(List<ChannelRecord> channelDataRecordsList) {
		this.channelDataRecordsList = channelDataRecordsList;
	}

	protected void mapDataToChannel(VariableDataStructure data, int index, Channel<?> channel, DataType dataType) {
		
		if(dataType == null) {
			if(data.getDataRecords().size() > index && index >= 0) {
				channel.setNextValue(data.getDataRecords().get(index).getScaledDataValue());
				System.out.println("DataType: Data");
				System.out.println(channel.value());
			}
			
			return;
		}
		switch(dataType) {
		case Manufacturer:
			channel.setNextValue(data.getSecondaryAddress().getManufacturerId());
			System.out.println("DataType: Manufacturer");
			System.out.println(channel.value());
			break;
		case DeviceId:
			channel.setNextValue(data.getSecondaryAddress().getDeviceId());
			System.out.println("DataType: DeviceId");
			System.out.println(channel.value());
			break;
		case MeterType:
			channel.setNextValue(data.getSecondaryAddress().getDeviceType());
			System.out.println("DataType: MeterType");
			System.out.println(channel.value());
			break;
		default:
			break;
		}
	}
	
}
