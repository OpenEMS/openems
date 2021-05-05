package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.StringReadChannel;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.wireless.WMBusConnection;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is the representation of a device for the Wireless M-Bus bridge. It is the link between the Wireless M-Bus
 * bridge and the device.
 * An instance of this class is created by the module implementing the device. It contains all the information needed by
 * the Wireless M-Bus bridge.
 */

public class WMbusProtocol {

	private final AbstractOpenemsWMbusComponent parent; // creator of this task instance
	private final byte[] key;

	protected final List<ChannelRecord> channelDataRecordsList = new ArrayList<ChannelRecord>();

	// This is the data link layer (=dll) secondary address. The radio address is part of this.
	private SecondaryAddress dllSecondaryAddress = null;

	/* The meter number is part of the transport layer (=tpl) secondary address. Most meters can be identified by the
	   radio address alone, but for some cases (like distinguishing between channel 1 and 2 for the Padpuls Relay)
	   the meter number is also needed. */
	private boolean identifyByMeterNumber = false;
	private String meterNumber = "";

	// The WMBus protocol can be used with or without error logging to a channel.
	private boolean useErrorChannel = false;
	private StringReadChannel errorMessageChannel;	// If any error is detected, an error message will be written in this channel.

	/**
	 * Constructor. Should be called in the "defineWMbusProtocol()" method of the WM-Bus component.
	 * This is the constructor without an error message channel. This will disable error logging.
	 *
	 * @param parent        	The component creating the protocol.
	 * @param keyAsHexString    The decryption key for the WM-Bus messages. Leave blank if not encrypted.
	 * @param channelRecords    The channel records. Each channelRecord consists of a channel and a data record position.
	 */
	public WMbusProtocol(AbstractOpenemsWMbusComponent parent, String keyAsHexString, ChannelRecord... channelRecords) {
		this.parent = parent;
		if (keyAsHexString.equals("")) {
			this.key = null;
		} else {
			this.key = this.hexStringToByteArray(keyAsHexString);
		}
		this.channelDataRecordsList.addAll(Arrays.asList(channelRecords));
	}

	/**
	 * Constructor. Should be called in the "defineWMbusProtocol()" method of the WM-Bus component.
	 * This is the constructor using an error message channel for error logging.
	 *
	 * @param parent        		The component creating the protocol.
	 * @param keyAsHexString    	The decryption key for the WM-Bus messages. Leave blank if not encrypted.
	 * @param errorMessageChannel   A string channel to which any error messages will be written.
	 * @param channelRecords    	The channel records. Each channelRecord consists of a channel and a data record position.
	 */
	public WMbusProtocol(AbstractOpenemsWMbusComponent parent, String keyAsHexString, StringReadChannel errorMessageChannel,
                         ChannelRecord... channelRecords) {
		this(parent, keyAsHexString, channelRecords);
		this.errorMessageChannel = errorMessageChannel;
		this.useErrorChannel = true;
		this.errorMessageChannel.setNextValue("No signal received so far.");
	}

	/**
	 * A data converter to convert a hex number in string format to a byte array.
	 *
	 * @param value		A hex number in string format.
	 * @return a byte array.
	 */
	private byte[] hexStringToByteArray(String value) {
		return DatatypeConverter.parseHexBinary(value);
	}

	/**
	 * Search the VariableDataStructure of a M-Bus or Wireless M-Bus message to extract the data records.
	 * Data records have a unit, which is compared to the unit of the OpenEMS channel associated with that record.
	 * This is done in the ChannelDataRecordMapper class. With that information the values are scaled, or an an error
	 * message is logged when the units don't match. The error logging is optional and is enabled by creating the
	 * WMbusProtocol with the right constructor.
	 *
	 * @param data the VariableDataStructure of a M-Bus or Wireless M-Bus message.
	 */
	public void processData(VariableDataStructure data) {
		if (this.parent.isDynamicDataAddress()) {
			// Analyze the data to find the right address and modify the entry in channelDataRecordsList.
			this.parent.findRecordPositions(data, this.channelDataRecordsList);
		}
		if (this.useErrorChannel) {
			new ChannelDataRecordMapper(data, this.channelDataRecordsList, this.errorMessageChannel);
		} else {
			new ChannelDataRecordMapper(data, this.channelDataRecordsList);
		}
	}

	/**
	 * Gets the radio address of the meter that is the parent of this class.
	 *
	 * @return the radio address.
	 */
	public String getRadioAddress() {
		return this.parent.getRadioAddress();
	}

	/**
	 * Gets the component Id of the meter that is the parent of this class.
	 *
	 * @return the radio address.
	 */
	public String getComponentId() { return this.parent.id(); }

	/**
	 * Register the data link layer secondary address for this device.
	 *
	 * @param dllSecondaryAddress	The data link layer secondary address.
	 */
	public void setDllSecondaryAddress(SecondaryAddress dllSecondaryAddress) {
		this.dllSecondaryAddress = dllSecondaryAddress;
	}

	/**
	 * Gets the data link layer secondary address of the WM-Bus device that is the parent of this class.
	 *
	 * @return the data link layer secondary address.
	 */
	public SecondaryAddress getDllSecondaryAddress() {
		return this.dllSecondaryAddress;
	}

	/**
	 * Register the decryption key for this device to the currently active Wireless M-Bus receiver.
	 *
	 * @param connection the WMBusConnection created by the Wireless M-Bus receiver.
	 */
	public void registerKey(WMBusConnection connection) {
		connection.addKey(this.dllSecondaryAddress, this.key);
	}

	/**
	 * Log the signal strength of the received message for this device.
	 *
	 * @param signalStrength the signal strength of the received message. Unit is decibel Milliwatt.
	 */
	public void logSignalStrength(int signalStrength) {
		this.parent.logSignalStrength(signalStrength);
	}

	/**
	 * Query whether the "identify by meter number" mode is used by this WM-Bus device.
	 *
	 * @return true or false.
	 */
	public boolean isIdentifyByMeterNumber() {
		return this.identifyByMeterNumber;
	}

	/**
	 * Set the meter number for this device and enable "identify by meter number mode".
	 *
	 * @param meterNumber The meter number as a string.
	 */
	public void setMeterNumber(String meterNumber) {
		this.meterNumber = meterNumber;
		this.identifyByMeterNumber = true;
	}

	/**
	 * Gets the meter number associated with this WM-Bus device. Only available when the WM-Bus device uses the
	 * "identify by meter number" mode.
	 *
	 * @return the meter number.
	 */
	public String getMeterNumber() {
		return this.meterNumber;
	}

	/**
	 * Query whether error logging is active for this WM-Bus device.
	 *
	 * @return true or false.
	 */
	public boolean isUseErrorChannel() {
		return this.useErrorChannel;
	}

	/**
	 * Log an error message for this device.
	 *
	 * @param value The error message as a string.
	 */
	public void setError(String value) {
		this.errorMessageChannel.setNextValue(value);
	}
}