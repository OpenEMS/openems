package io.openems.edge.bridge.mccomms.task;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.IMCCommsBridge;
import io.openems.edge.bridge.mccomms.MCCommsBridge;
import io.openems.edge.bridge.mccomms.packet.MCCommsElement;
import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;

import java.io.OutputStream;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to represent packets that must be written out to the serial bus via an {@link MCCommsBridge}
 * Capable of repeatedly writing out
 */
public class WriteTask {
	/**
	 * The packet to be written out
	 */
	private MCCommsPacket packet;
	
	/**
	 * Constructor
	 * @param packet the packet to be written out
	 */
	private WriteTask(MCCommsPacket packet) {
		this.packet = packet;
	}
	
	/**
	 * Static constructor for sending a packet with an empty payload and custom command value
	 * @param sourceAddress The MCComms address to which the packet must be sent
	 * @param destinationAddress The MCComms address from which this packet must appear to originate
	 * @param command The command value to populate the packet with
	 * @return a new WriteTask instance
	 * @throws OpenemsException if the outgoing packet cannot be constructed
	 */
	static WriteTask newCommandOnlyWriteTask(int sourceAddress, int destinationAddress, int command) throws OpenemsException {
		return new WriteTask(new MCCommsPacket(
				MCCommsElement.newUnscaledNumberInstance(1, 2, destinationAddress, true),
				MCCommsElement.newUnscaledNumberInstance(3, 2, sourceAddress, true),
				MCCommsElement.newUnscaledNumberInstance(5, 1, command, true)
			)
		);
	}
	
	/**
	 * @return the byte buffer to be written out
	 */
	public byte[] getBytes() {
		return packet.getBytes();
	}
	
	/**
	 * Method for this instance to repeatedly schedule itself to be written out by a {@link MCCommsBridge} instance
	 * @param bridge the bridge from which the buffers must be sent
	 * @param timePeriod the length of the time period between serial writes
	 * @param timeUnit the time unit the time period must be measured in
	 * @return a {@link ScheduledFuture} which can be used to cancel repetition
	 */
	public ScheduledFuture sendRepeatedly(IMCCommsBridge bridge, long timePeriod, TimeUnit timeUnit) {
		return bridge.getScheduledExecutorService().scheduleAtFixedRate(() -> bridge.addWriteTask(this), 0, timePeriod, timeUnit);
	}
	
	/**
	 * Method to transmit a buffer via a {@link MCCommsBridge} instance just once
	 * @param bridge the bridge from which to transmit the buffer
	 */
	public void sendOnce(IMCCommsBridge bridge) {
		bridge.addWriteTask(this);
	}
}
