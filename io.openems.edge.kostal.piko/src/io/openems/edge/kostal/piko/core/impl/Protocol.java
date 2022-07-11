package io.openems.edge.kostal.piko.core.impl;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.kostal.piko.core.api.KostalPikoCore.ChannelId;

public class Protocol {

	private final Logger log = LoggerFactory.getLogger(KostalPikoCoreImpl.class);
	private final KostalPikoCoreImpl parent;
	private final SocketConnection socketConnection;

	public Protocol(KostalPikoCoreImpl parent, SocketConnection socketConnection) {
		this.parent = parent;
		this.socketConnection = socketConnection;
	}

	protected void execute(List<ReadTask> nextReadTasks) {
		for (ReadTask task : nextReadTasks) {
			try {
				this.socketConnection.open();
				Channel<?> channel = task.getComponent().channel(task.getChannelId());
				switch (task.getFieldType()) {
				case STRING:
					channel.setNextValue(this.getStringValue(task.getAddress()));
					break;
				case INTEGER:
					channel.setNextValue(this.getIntegerValue(task.getAddress()));
					break;
				case BOOLEAN:
					channel.setNextValue(this.getBooleanValue(task.getAddress()));
					break;
				case INTEGER_UNSIGNED_BYTE:
					channel.setNextValue(this.getIntegerFromUnsignedByte(task.getAddress()));
					break;
				case FLOAT:
					channel.setNextValue(this.getFloatValue(task.getAddress()));
					break;
				}
			} catch (OpenemsException e) {
				this.log.warn("KOSTAL Protocol error. " + e.getClass().getSimpleName() + ": " + e.getMessage()
						+ " while executing " + task.toString());

				this.parent.channel(ChannelId.UNABLE_TO_READ_DATA).setNextValue(true);
				return;
			}
		}
		this.parent.channel(ChannelId.UNABLE_TO_READ_DATA).setNextValue(false);
	}

	protected boolean getBooleanValue(int address) throws OpenemsException {
		var bytes = this.sendAndReceive(address);
		if (bytes[0] == 1) {
			return true;
		}
		return false;
	}

	protected int getIntegerFromUnsignedByte(int address) throws OpenemsException {
		var bytes = this.sendAndReceive(address);
		return ByteBuffer.wrap(bytes).get() & 0xFF;
	}

	protected float getFloatValue(int address) throws OpenemsException {
		var bytes = this.sendAndReceive(address);
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
	}

	protected int getIntegerValue(int address) throws OpenemsException {
		var bytes = this.sendAndReceive(address);
		var b = ByteBuffer.allocate(4).putInt(0).order(ByteOrder.LITTLE_ENDIAN);
		b.rewind();
		b.put(bytes);
		b.rewind();
		return b.getInt();
	}

	protected String getStringValue(int address) throws OpenemsException {
		var stringValue = new StringBuilder();
		var byi = this.sendAndReceive(address);
		for (byte b : byi) {
			if (b == 0) {
				break;
			}
			stringValue.append((char) b);
		}
		return stringValue.toString().trim();
	}

	private byte[] addressWithByteBuffer(int address) {
		var byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put((byte) (address & 0xff));
		byteBuffer.put((byte) (address >> 8 & 0xff));
		byteBuffer.put((byte) (address >> 16 & 0xff));
		byteBuffer.put((byte) (address >> 24 & 0xff));
		return byteBuffer.array();
	}

	private byte[] sendAndReceive(int address) throws OpenemsException {

		/*
		 * convert address to byte array
		 */
		var result = this.addressWithByteBuffer(address);

		/*
		 * Calculate Checksum
		 */
		var checksum = this.calculateChecksumFromAddress(result);
		/*
		 * Build Request
		 */
		byte[] request = { 0x62, this.socketConnection.getUnitID(), 0x03, this.socketConnection.getUnitID(), 0x00,
				(byte) 0xf0, Array.getByte(result, 0), Array.getByte(result, 1), Array.getByte(result, 2),
				Array.getByte(result, 3), checksum, 0x00 };
		List<Byte> datasList = new ArrayList<>();
		try {
			/*
			 * Send
			 */
			this.socketConnection.getOut().write(request);
			this.socketConnection.getOut().flush();
			Thread.sleep(100);
			/*
			 * Receive
			 */
			while (this.socketConnection.getIn().available() > 0) {
				var data = (byte) this.socketConnection.getIn().read();
				datasList.add(data);
			}
		} catch (IOException | InterruptedException e) {
			throw new OpenemsException("Unable to read from socket: " + e.getMessage());
		}
		if (datasList.isEmpty()) {
			throw new OpenemsException("Could not receive any data");
		}
		var datas = new byte[datasList.size()];
		for (var i = 0; i < datasList.size(); i++) {
			datas[i] = datasList.get(i);
		}
		/*
		 * Verify Checksum of Reply
		 */
		var isChecksumOk = this.verifyChecksumOfReply(datas);
		if (!isChecksumOk) {
			throw new OpenemsException("Checksum cannot be verified");
		}
		/*
		 * Extract value
		 */
		byte[] results = new byte[datas.length - 7];

		for (var i = 5; i < datas.length - 2; i++) {
			results[i - 5] = datas[i];
		}
		/*
		 * Return value
		 */
		return results;
	}

	private byte calculateChecksumFromAddress(byte[] result) {
		byte checksum = 0x00;
		byte[] request = { 0x62, this.socketConnection.getUnitID(), 0x03, this.socketConnection.getUnitID(),
				Array.getByte(result, 0), 0x00, (byte) 0xf0, Array.getByte(result, 1), Array.getByte(result, 2),
				Array.getByte(result, 3) };
		for (byte element : request) {
			checksum -= element;
		}
		return checksum;
	}

	private boolean verifyChecksumOfReply(byte[] data) {
		byte checksum = 0x00;
		for (byte element : data) {
			checksum += element;
		}
		return checksum == 0x00;
	}
}
