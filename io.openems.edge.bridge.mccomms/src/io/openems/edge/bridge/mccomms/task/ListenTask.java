package io.openems.edge.bridge.mccomms.task;

import com.google.common.primitives.UnsignedBytes;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;

import java.nio.ByteBuffer;

public class ListenTask {
	private int sourceAddress;
	private int destinationAddress;
	private int command;
	private MCCommsPacket packet;
	
	public ListenTask(int sourceAddress, int destinationAddress, int command, MCCommsPacket packet) {
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destinationAddress;
		this.command = command;
		this.packet = packet;
	}
	
	public void acceptBuffer(ByteBuffer buffer) throws OpenemsException {
		if (Short.toUnsignedInt(buffer.getShort(3)) == this.sourceAddress
				&& Short.toUnsignedInt(buffer.getShort(1)) == this.destinationAddress
				&& UnsignedBytes.toInt(buffer.get(5)) == this.command) {
			packet.setBytesAndUpdateChannels(buffer.array());
		}
	}
}
