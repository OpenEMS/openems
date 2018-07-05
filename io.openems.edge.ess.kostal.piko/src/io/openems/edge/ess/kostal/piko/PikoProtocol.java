package io.openems.edge.ess.kostal.piko;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;

public class PikoProtocol {

	private final Logger log = LoggerFactory.getLogger(PikoProtocol.class);
	
	private final EssKostalPiko parent;
	// private final short deviceId;
	private final SocketConnection socketConnection;

	private final static boolean DEBUG_MODE = false;

	// , short deviceId
	public PikoProtocol(EssKostalPiko parent, String host, int port) {
		this.parent = parent;
		// this.deviceId = deviceId;
		this.socketConnection = new SocketConnection(host, port);
	}

	public void execute(List<ReadTask> nextReadTasks) {
		try {
			for (ReadTask task : nextReadTasks) {
				this.socketConnection.open();
				try {
					Channel<?> channel = this.parent.channel(task.getChannelId());
					switch (task.getFieldType()) {
					case STRING:
						channel.setNextValue(getStringValue(task.getAddress()));
						break;
					case INTEGER:
						channel.setNextValue(getIntegerValue(task.getAddress()));
						break;
					case BOOLEAN:
						channel.setNextValue(getBooleanValue(task.getAddress()));
						break;
					case INTEGER_UNSIGNED_BYTE:
						channel.setNextValue(getIntegerFromUnsignedByte(task.getAddress()));
						break;
					case FLOAT:
						channel.setNextValue(getFloatValue(task.getAddress()));
						break;
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected boolean getBooleanValue(int address) throws Exception {
		byte[] bytes = sendAndReceive(address);
		if (bytes[0] == 1) {
			return true;
		}
		return false;
	}

	protected int getIntegerFromUnsignedByte(int address) throws Exception {
		byte[] bytes = sendAndReceive(address);
		return (int) ByteBuffer.wrap(bytes).get() & (0xFF);
	}

	protected float getFloatValue(int address) throws Exception {
		byte[] bytes = sendAndReceive(address);
		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
	}

	protected int getIntegerValue(int address) throws Exception {
		byte[] bytes = sendAndReceive(address);
		ByteBuffer b = ByteBuffer.allocate(4).putInt(0).order(ByteOrder.LITTLE_ENDIAN);
		b.rewind();
		b.put(bytes);
		b.rewind();
		return b.getInt();
	}

	protected String getStringValue(int address) throws Exception {
		String stringValue = "";
		byte[] byi = sendAndReceive(address);
		for (byte b : byi) {
			if (b == 0) {
				break;
			}
			stringValue += (char) b;
		}
		return stringValue.trim();
	}

	private static byte[] addressWithByteBuffer(int address) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put((byte) ((address) & (0xff)));
		byteBuffer.put((byte) (((address) >> 8) & (0xff)));
		byteBuffer.put((byte) (((address) >> 16) & (0xff)));
		byteBuffer.put((byte) (((address) >> 24) & (0xff)));
		byte[] result = byteBuffer.array();
		return result;
	}

	private byte[] sendAndReceive(int address) throws Exception {
		/*
		 * convert address to byte array
		 */
		byte[] result = addressWithByteBuffer(address);

		/*
		 * Calculate Checksum
		 */
		byte checksum = calculateChecksumFromAddress(result);
		/*
		 * Build Request
		 */
		byte[] request = new byte[] { 0x62, (byte) 0Xff, 0x03, (byte) 0xff, 0x00, (byte) 0xf0, Array.getByte(result, 0),
				Array.getByte(result, 1), Array.getByte(result, 2), Array.getByte(result, 3), checksum, 0x00 };
		/*
		 * Send
		 */
		if (DEBUG_MODE) {
			for (byte b : request) {
				System.out.print(Integer.toHexString(b));
			}
		}

		this.socketConnection.getOut().write(request);
		this.socketConnection.getOut().flush();
		Thread.sleep(100);
		/*
		 * Receive
		 */
		List<Byte> datasList = new ArrayList<>();
		while (this.socketConnection.getIn().available() > 0) {
			byte data = (byte) this.socketConnection.getIn().read();
			datasList.add(data);
		}
		if (datasList.isEmpty()) {
			throw new Exception("Could not receive any data");
		}
		byte[] datas = new byte[datasList.size()];
		for (int i = 0; i < datasList.size(); i++) {
			datas[i] = datasList.get(i);
		}
		/*
		 * Verify Checksum of Reply
		 */
		boolean isChecksumOk = verifyChecksumOfReply(datas);
		if (!isChecksumOk) {
			throw new Exception("Checksum cannot be verified");
		}
		/*
		 * Extract value
		 */
		byte[] results = new byte[datas.length - 7];
		for (int i = 5; i < datas.length - 2; i++) {
			results[i - 5] = datas[i];
		}
		/*
		 * Return value
		 */
		return results;
	}

	private static byte calculateChecksumFromAddress(byte[] result) {
		byte checksum = 0x00;
		byte[] request = new byte[] { 0x62, (byte) 0xff, 0x03, (byte) 0xff, Array.getByte(result, 0), 0x00, (byte) 0xf0,
				Array.getByte(result, 1), Array.getByte(result, 2), Array.getByte(result, 3) };
		for (int i = 0; i < request.length; i++) {
			checksum -= request[i];
		}
		return checksum;
	}

	private static boolean verifyChecksumOfReply(byte[] data) {
		byte checksum = 0x00;
		for (int i = 0; i < data.length; i++) {
			checksum += data[i];
		}
		return checksum == 0x00;
	}

	/**
	 * Closing the socket, streams and Deactivate the Component
	 */
	public void deactivate() {
		this.socketConnection.close();
	}
}
