package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.StringReadChannel;
import org.openmuc.jmbus.VariableDataStructure;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
			usePollTimer = true;
			lastPollTimestamp = LocalDateTime.now().minusSeconds(this.pollingIntervalSeconds);	// Subtract pollingIntervalSeconds so the first poll is immediately.
		}
	}

	public MbusTask(BridgeMbus bridgeMbus, AbstractOpenemsMbusComponent openemsMbusComponent, int pollingIntervalSeconds,
					StringReadChannel errorMessageChannel) {
		this(bridgeMbus, openemsMbusComponent, pollingIntervalSeconds);
		this.errorMessageChannel = errorMessageChannel;
		this.useErrorChannel = true;
		this.errorMessageChannel.setNextValue("No data received so far.");
	}

	public VariableDataStructure getRequest() throws InterruptedIOException, IOException {
		lastPollTimestamp = LocalDateTime.now();
		return this.bridgeMbus.getmBusConnection().read(this.openemsMbusComponent.getPrimaryAddress());
	}

	public boolean permissionToPoll() {
		if (usePollTimer == false) {
			return true;
		}
		if (ChronoUnit.SECONDS.between(lastPollTimestamp, LocalDateTime.now()) >= pollingIntervalSeconds) {
			return true;
		}
		return false;
	}

	public void processData(VariableDataStructure data) {
		if (openemsMbusComponent.isDynamicDataAddress()) {
			// Analyze the data to find the right address and modify the entry in channelDataRecordsList.
			openemsMbusComponent.findRecordPositions(data, this.openemsMbusComponent.getChannelDataRecordsList());
		}
		if (useErrorChannel) {
			new ChannelDataRecordMapper(data, this.openemsMbusComponent.getChannelDataRecordsList(), errorMessageChannel);
		} else {
			new ChannelDataRecordMapper(data, this.openemsMbusComponent.getChannelDataRecordsList());
		}
	}

	public int getPrimaryAddress() {
		return this.openemsMbusComponent.getPrimaryAddress();
	}

	public String getMeterId() { return this.openemsMbusComponent.getModuleId(); }

	public AbstractOpenemsMbusComponent getOpenemsMbusComponent() {
		return openemsMbusComponent;
	}
}