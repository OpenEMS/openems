package io.openems.edge.bridge.mbus.api.task;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.openmuc.jmbus.VariableDataStructure;

import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelDataRecordMapper;

public class MbusTask {

	protected AbstractOpenemsMbusComponent openemsMbusComponent; //creator of this task instance
	protected BridgeMbus bridgeMbus;

	public MbusTask(BridgeMbus bridgeMbus, AbstractOpenemsMbusComponent openemsMbusComponent ) {
		this.openemsMbusComponent = openemsMbusComponent;
		this.bridgeMbus = bridgeMbus;
	}

	public VariableDataStructure getRequest() throws InterruptedIOException, IOException {
		return this.bridgeMbus.getmBusConnection().read(this.openemsMbusComponent.getPrimaryAddress());
	};

	public void setResponse(VariableDataStructure data) {
		new ChannelDataRecordMapper(data, this.openemsMbusComponent.getChannelDataRecordsList());
	};

	public int getPrimaryAddress() {
		return this.openemsMbusComponent.getPrimaryAddress();
	};

}