package io.openems.impl.protocol.studer;

import io.openems.api.bridge.BridgeWriteTask;
import io.openems.impl.protocol.studer.internal.property.WriteProperty;

public class StuderBridgeWriteTask extends BridgeWriteTask {

	private final WriteProperty<?> property;
	private final int srcAddress;
	private final int dstAddress;
	private final StuderBridge bridge;

	public StuderBridgeWriteTask(WriteProperty<?> property, int srcAddress, int dstAddress, StuderBridge bridge) {
		super();
		this.property = property;
		this.srcAddress = srcAddress;
		this.dstAddress = dstAddress;
		this.bridge = bridge;
	}

	@Override
	protected void run() throws Exception {
		property.writeValue(srcAddress, dstAddress, bridge);
	}

}
