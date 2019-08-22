package io.openems.edge.bridge.mccomms.task;

import com.google.common.primitives.UnsignedBytes;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class ListenTask implements Future<MCCommsPacket> {
	private int sourceAddress;
	private int destinationAddress;
	private int command;
	private MCCommsPacket packet;
	private Predicate<ByteBuffer>[] otherConditions;
	private boolean hasReturned;
	
	public ListenTask(int sourceAddress, int destinationAddress, int command, MCCommsPacket packet, Predicate<ByteBuffer>...otherConditions) {
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destinationAddress;
		this.command = command;
		this.packet = packet;
		this.otherConditions = otherConditions;
		this.hasReturned = false;
	}
	
	private boolean checkOtherConditions(ByteBuffer buffer) {
		return Arrays.stream(otherConditions).allMatch(predicate -> predicate.test(buffer));
	}
	
	public void acceptBuffer(ByteBuffer buffer) throws OpenemsException {
		if (Short.toUnsignedInt(buffer.getShort(3)) == this.sourceAddress
				&& Short.toUnsignedInt(buffer.getShort(1)) == this.destinationAddress
				&& UnsignedBytes.toInt(buffer.get(5)) == this.command
				&& checkOtherConditions(buffer)
		) {
			packet.setBytes(buffer.array());
			this.hasReturned = true;
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
			while (!this.hasReturned)
			this.wait();
		}
		this.hasReturned = false;
		return packet;
	}
	
	@Override
	public MCCommsPacket get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		synchronized (this) {
			if (!hasReturned) {
				this.wait(unit.toMillis(timeout));
			} else {
				this.hasReturned = false;
				return packet;
			}
			if (hasReturned) {
				this.hasReturned = false;
				return packet;
			} else {
				throw new TimeoutException();
			}
		}
	}
}
