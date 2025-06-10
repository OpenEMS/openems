package io.openems.edge.bridge.mbus.api;

import java.io.IOException;
import java.io.InterruptedIOException;

import org.openmuc.jmbus.VariableDataStructure;

public class MbusTask {

	private final AbstractOpenemsMbusComponent openemsMbusComponent; // creator of this task instance
	private final BridgeMbus bridgeMbus;

	public MbusTask(BridgeMbus bridgeMbus, AbstractOpenemsMbusComponent openemsMbusComponent) {
		this.openemsMbusComponent = openemsMbusComponent;
		this.bridgeMbus = bridgeMbus;
	}

	/**
	 * Get the Request.
	 * 
	 * @return a {@link VariableDataStructure}
	 * @throws InterruptedIOException on error
	 * @throws IOException            on error
	 */
	public VariableDataStructure getRequest() throws InterruptedIOException, IOException {
		return this.bridgeMbus.getmBusConnection().read(this.openemsMbusComponent.getPrimaryAddress());
	}

	public void setResponse(VariableDataStructure data) {
		new ChannelDataRecordMapper(data, this.openemsMbusComponent.getChannelDataRecordsList());
	}

	public int getPrimaryAddress() {
		return this.openemsMbusComponent.getPrimaryAddress();
	}

}