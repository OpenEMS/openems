package io.openems.edge.bridge.mccomms.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.MCCommsBridge;
import io.openems.edge.bridge.mccomms.packet.MCCommsElement;
import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;

import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WriteTask {
	private MCCommsPacket packet;
	
	private WriteTask(MCCommsPacket packet) {
		this.packet = packet;
	}
	
	public static WriteTask newCommandOnlyWriteTask(int sourceAddress, int destinationAddress, int command) throws OpenemsException {
		return new WriteTask(new MCCommsPacket(
				MCCommsElement.newUnscaledNumberInstance(1, 2, destinationAddress, true),
				MCCommsElement.newUnscaledNumberInstance(3, 2, sourceAddress, true),
				MCCommsElement.newUnscaledNumberInstance(5, 1, command, true)
			)
		);
	}
	
	public byte[] getBytes() {
		return packet.getBytes();
	}
	
	public ScheduledFuture sendRepeatedly(MCCommsBridge bridge, long timePeriod, TimeUnit timeUnit) {
		return bridge.getScheduledExecutorService().scheduleAtFixedRate(() -> bridge.addWriteTask(this), 0, timePeriod, timeUnit);
	}
	
	public void sendOnce(MCCommsBridge bridge) {
		bridge.addWriteTask(this);
	}
}
