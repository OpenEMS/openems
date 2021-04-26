package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.StringReadChannel;
import org.openmuc.jmbus.VariableDataStructure;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// This class links the M-Bus bridge to a device. It is created by the module implementing the device and contains all
// the information needed by the M-Bus bridge.

public class MbusTask {

	private final AbstractOpenemsMbusComponent openemsMbusComponent; // creator of this task instance
	private final BridgeMbus bridgeMbus;
	private boolean usePollTimer = false;
	private LocalDateTime lastPollTimestamp;
	private int pollingIntervalSeconds = 0;

	// The MbusTask can be used with or without error logging to a channel.
	private boolean useErrorChannel = false;
	private StringReadChannel errorMessageChannel;	// If any error is detected, an error message will be written in this channel.

	public MbusTask(BridgeMbus bridgeMbus, AbstractOpenemsMbusComponent openemsMbusComponent, int pollingIntervalSeconds) {
		this.openemsMbusComponent = openemsMbusComponent;
		this.bridgeMbus = bridgeMbus;
		this.pollingIntervalSeconds = pollingIntervalSeconds;
		if (pollingIntervalSeconds > 0) {
			this.usePollTimer = true;
			this.lastPollTimestamp = LocalDateTime.now().minusSeconds(this.pollingIntervalSeconds);	// Subtract pollingIntervalSeconds so the first poll is immediately.
		}
	}

	public MbusTask(BridgeMbus bridgeMbus, AbstractOpenemsMbusComponent openemsMbusComponent, int pollingIntervalSeconds,
					StringReadChannel errorMessageChannel) {
		this(bridgeMbus, openemsMbusComponent, pollingIntervalSeconds);
		this.errorMessageChannel = errorMessageChannel;
		this.useErrorChannel = true;
		this.errorMessageChannel.setNextValue("No data received so far.");
	}

	/**
	 * Send a request for data to the M-Bus device. Get the content of the answer message from the device as a
	 * variable data structure and return it to the caller of the method.
	 *
	 * @return the variable data structure.
	 */
	public VariableDataStructure getRequest() throws InterruptedIOException, IOException {
		this.lastPollTimestamp = LocalDateTime.now();
		return this.bridgeMbus.getmBusConnection().read(this.openemsMbusComponent.getPrimaryAddress());
	}

	/**
	 * Ask if the poll timer for this device has reached zero or not. Used to control the polling interval for a device.
	 * Will always return true when no polling interval has been set for the device.
	 *
	 * @return true or false.
	 */
	public boolean permissionToPoll() {
		if (this.usePollTimer == false) {
			return true;
		}
		if (ChronoUnit.SECONDS.between(this.lastPollTimestamp, LocalDateTime.now()) >= this.pollingIntervalSeconds) {
			return true;
		}
		return false;
	}

	/**
	 * Search the VariableDataStructure of a M-Bus or Wireless M-Bus message to extract the data records.
	 * Data records have a unit, which is compared to the unit of the OpenEMS channel associated with that record.
	 * This is done in the ChannelDataRecordMapper class. With that information the values are scaled, or an an error
	 * message is logged when the units don't match. The error logging is optional and is enabled by creating the
	 * MbusTask with the right constructor.
	 *
	 * @param data the VariableDataStructure of a M-Bus or Wireless M-Bus message.
	 */
	public void processData(VariableDataStructure data) {
		if (this.openemsMbusComponent.isDynamicDataAddress()) {
			// Analyze the data to find the right address and modify the entry in channelDataRecordsList.
			this.openemsMbusComponent.findRecordPositions(data, this.openemsMbusComponent.getChannelDataRecordsList());
		}
		if (this.useErrorChannel) {
			new ChannelDataRecordMapper(data, this.openemsMbusComponent.getChannelDataRecordsList(), this.errorMessageChannel);
		} else {
			new ChannelDataRecordMapper(data, this.openemsMbusComponent.getChannelDataRecordsList());
		}
	}

	/**
	 * Get the primary address of the M-Bus device that is the parent of this class.
	 *
	 * @return true or false.
	 */
	public int getPrimaryAddress() {
		return this.openemsMbusComponent.getPrimaryAddress();
	}

	public String getMeterId() { return this.openemsMbusComponent.getModuleId(); }

	/**
	 * Get the parent of this class as it's super class.
	 *
	 * @return the super class AbstractOpenemsMbusComponent of the parent.
	 */
	public AbstractOpenemsMbusComponent getOpenemsMbusComponent() {
		return this.openemsMbusComponent;
	}
}