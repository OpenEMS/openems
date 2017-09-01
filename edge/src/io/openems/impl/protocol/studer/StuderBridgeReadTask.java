package io.openems.impl.protocol.studer;

import io.openems.api.bridge.BridgeReadTask;
import io.openems.impl.protocol.studer.internal.property.ReadProperty;

public class StuderBridgeReadTask extends BridgeReadTask {

	private final ReadProperty<?> property;
	private final int srcAddress;
	private final int dstAddress;
	private final StuderBridge bridge;

	public StuderBridgeReadTask(ReadProperty<?> property, int srcAddress, int dstAddress, StuderBridge bridge) {
		super();
		this.property = property;
		this.srcAddress = srcAddress;
		this.dstAddress = dstAddress;
		this.bridge = bridge;
	}

	public ReadProperty<?> getProperty() {
		return property;
	}

	@Override
	protected void run() throws Exception {
		property.updateValue(srcAddress, dstAddress, bridge);
	}

}
