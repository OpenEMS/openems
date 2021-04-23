package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.StringReadChannel;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.wireless.WMBusConnection;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.List;

public class WMbusProtocol {

	private final AbstractOpenemsWMbusComponent parent; // creator of this task instance
	private final byte[] key;

	protected final List<ChannelRecord> channelDataRecordsList = new ArrayList<ChannelRecord>();

	// This is the data link layer (=dll) secondary address. The radio address is part of this.
	private SecondaryAddress dllSecondaryAddress = null;

	// The meter number is part of the transport layer (=tpl) secondary address. Most meters can be identified by the
	// radio address alone, but for some cases (like distinguishing between channel 1 and 2 for the Padpuls Relay)
	// the meter number is also needed.
	private boolean identifyByMeterNumber = false;
	private String meterNumber = "";

	// The WMBus protocol can be used with or without error logging to a channel.
	private boolean useErrorChannel = false;
	private StringReadChannel errorMessageChannel;	// If any error is detected, an error message will be written in this channel.

	public WMbusProtocol(AbstractOpenemsWMbusComponent parent, String keyAsHexString, ChannelRecord... channelRecords) {
		this.parent = parent;
		if (keyAsHexString.equals("")) {
			this.key = null;
		} else {
			this.key = hexStringToByteArray(keyAsHexString);
		}
		for (ChannelRecord channelRecord : channelRecords) {
			this.channelDataRecordsList.add(channelRecord);
		}
	}

	public WMbusProtocol(AbstractOpenemsWMbusComponent parent, String keyAsHexString, StringReadChannel errorMessageChannel,
                         ChannelRecord... channelRecords) {
		this(parent, keyAsHexString, channelRecords);
		this.errorMessageChannel = errorMessageChannel;
		this.useErrorChannel = true;
		this.errorMessageChannel.setNextValue("No signal received so far.");
	}


	private byte[] hexStringToByteArray(String value) {
		return DatatypeConverter.parseHexBinary(value);
	}

	public void processData(VariableDataStructure data) {
		if (parent.isDynamicDataAddress()) {
			// Analyze the data to find the right address and modify the entry in channelDataRecordsList.
			parent.findRecordPositions(data, this.channelDataRecordsList);
		}
		if (useErrorChannel) {
			new ChannelDataRecordMapper(data, this.channelDataRecordsList, errorMessageChannel);
		} else {
			new ChannelDataRecordMapper(data, this.channelDataRecordsList);
		}
	}

	public String getRadioAddress() {
		return this.parent.getRadioAddress();
	}

	public String getComponentId() { return this.parent.id(); }

	public void setDllSecondaryAddress(SecondaryAddress dllSecondaryAddress) {
		this.dllSecondaryAddress = dllSecondaryAddress;
	}

	public SecondaryAddress getDllSecondaryAddress() {
		return dllSecondaryAddress;
	}

	public void registerKey(WMBusConnection connection) {
		connection.addKey(dllSecondaryAddress, key);
	}

	public void logSignalStrength(int signalStrength) {
		parent.logSignalStrength(signalStrength);
	}

	public boolean isIdentifyByMeterNumber() {
		return identifyByMeterNumber;
	}

	public void setMeterNumber(String meterNumber) {
		this.meterNumber = meterNumber;
		this.identifyByMeterNumber = true;
	}

	public String getMeterNumber() {
		return meterNumber;
	}

	public boolean isUseErrorChannel() {
		return useErrorChannel;
	}

	public void setError(String value) {
		this.errorMessageChannel.setNextValue(value);
	}
}