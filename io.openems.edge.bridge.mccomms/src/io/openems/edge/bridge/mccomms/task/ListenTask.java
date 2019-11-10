package io.openems.edge.bridge.mccomms.task;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import com.google.common.primitives.UnsignedBytes;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;

/**
 * Class used to listen for incoming packet buffers and map them to a defined
 * {@link MCCommsPacket} structure
 * 
 * @see io.openems.edge.bridge.mccomms.MCCommsBridge.PacketPicker
 */
public class ListenTask implements Future<MCCommsPacket> {
	/**
	 * The MCComms address from which incoming packets must originate in order to be
	 * mapped by this task
	 */
	private int sourceAddress;
	/**
	 * The MCComms address to which incoming packets must be addressed to in order
	 * to be mapped by this task
	 */
	private int destinationAddress;
	/**
	 * The mandatory value of the command field of the incoming packet in order for
	 * the this task to map the incoming buffer
	 */
	private int command;
	/**
	 * The defined packet structure to which the incoming buffer must be mapped
	 */
	private MCCommsPacket packet;
	/**
	 * Any other conditions that the incoming buffer must pass in the form of
	 * {@link ByteBuffer} consuming {@link Predicate}s
	 */
	private Predicate<ByteBuffer>[] otherConditions;
	/**
	 * Atomic boolean used to ensure interthread synchronisation for this
	 * {@link Future}-implementing class
	 */
	private AtomicBoolean hasReturned;

	/**
	 * Constructor
	 * 
	 * @param sourceAddress      the MCComms address from which incoming packets
	 *                           must originate in order to be mapped by this task
	 * @param destinationAddress the MCComms address to which incoming packets must
	 *                           be addressed to in order to be mapped by this task
	 * @param command            the mandatory value of the command field of the
	 *                           incoming packet in order for the this task to map
	 *                           the incoming buffer
	 * @param packet             the defined packet structure to which the incoming
	 *                           buffer must be mapped
	 * @param otherConditions    any other conditions that the incoming buffer must
	 *                           pass in the form of {@link ByteBuffer} consuming
	 *                           {@link Predicate}s
	 */
	@SafeVarargs
	public ListenTask(int sourceAddress, int destinationAddress, int command, MCCommsPacket packet,
			Predicate<ByteBuffer>... otherConditions) {
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destinationAddress;
		this.command = command;
		this.packet = packet;
		this.otherConditions = otherConditions;
		this.hasReturned = new AtomicBoolean(false);
	}

	/**
	 * Internal method to ensure all supplied additional conditions (see
	 * {@link ListenTask#ListenTask(int, int, int, MCCommsPacket, Predicate[])})
	 * return true
	 * 
	 * @param buffer the buffer to check
	 * @return true if all additional conditions pass true; false otherwise
	 */
	private boolean checkOtherConditions(ByteBuffer buffer) {
		if (otherConditions.length > 0) {
			return Arrays.stream(otherConditions).allMatch(predicate -> predicate.test(buffer));
		}
		return true;
	}

	/**
	 * Consumes a buffer, and if the source address, destination address, command
	 * and all other supplied conditions ({@link ListenTask#otherConditions}) match
	 * or return true, maps the buffer to a defined packet structure
	 * 
	 * @param buffer the buffer to consume
	 * @throws OpenemsException if the buffer cannot be mapped to the packet
	 *                          structure
	 */
	public void acceptBuffer(ByteBuffer buffer) throws OpenemsException {
		if (Short.toUnsignedInt(buffer.getShort(3)) == this.sourceAddress
				&& Short.toUnsignedInt(buffer.getShort(1)) == this.destinationAddress
				&& UnsignedBytes.toInt(buffer.get(5)) == this.command && checkOtherConditions(buffer)) {
			packet.setBytes(buffer.array());
			this.hasReturned.set(true);
			synchronized (this) {
				notifyAll();
			}
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public MCCommsPacket get() throws InterruptedException, ExecutionException {
		synchronized (this) {
			while (!this.hasReturned.get())
				this.wait();
		}
		this.hasReturned.set(false);
		return packet;
	}

	@Override
	public MCCommsPacket get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		if (!hasReturned.get()) {
			synchronized (this) {
				unit.timedWait(this, timeout);
			}
			if (hasReturned.get()) {
				this.hasReturned.set(false);
				return packet;
			} else {
				throw new TimeoutException("Listen window timed out [" + command + "]");
			}
		} else {
			this.hasReturned.set(false);
			return packet;
		}
	}
}
