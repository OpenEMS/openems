package io.openems.impl.protocol.studer.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;

import io.openems.common.exceptions.OpenemsException;
import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

/**
 * Holds a serial connection to a Studer device
 *
 * @author stefan.feilmeier
 */
public class StuderConnection {

	private SerialPort connection = null;
	private Request request = null;

	public StuderConnection(String portName) {
		connection = SerialPort.getCommPort(portName);
		connection.setComPortParameters(38400, 8, SerialPort.ONE_STOP_BIT, SerialPort.EVEN_PARITY);
		connection.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
	}

	public void connect() {
		if (this.connection != null) {
			connection.openPort();
		}
	}

	public boolean isConnected() {
		return connection.isOpen();
	}

	public void disconnect() {
		if (this.connection != null) {
			this.connection.closePort();
		}
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public void execute() throws OpenemsException {
		try {
			if (connection != null && connection.isOpen() && request != null) {
				OutputStream out = connection.getOutputStream();
				for (byte b : request.getBytes()) {
					out.write(b);
				}
				out.flush();
				out.close();
				InputStream in = connection.getInputStream();
				byte startByte = (byte) in.read();
				if (startByte == (byte) 0xAA) {
					List<Byte> checksumBytes = new ArrayList<>();
					byte frameTags = (byte) in.read();
					boolean isMessagePending = (frameTags & 1) != 0;
					boolean isRccRestart = (frameTags & 2) != 0;
					boolean isSdCardPresent = (frameTags & 4) != 0;
					boolean isSdCardFull = (frameTags & 8) != 0;
					boolean isNewDataloggerFilePresent = (frameTags & 16) != 0;
					boolean isDatalogSupported = (frameTags & 32) != 0;
					checksumBytes.add(frameTags);
					byte[] srcAddressBytes = new byte[4];
					for (int i = 0; i < 4; i++) {
						byte b = (byte) in.read();
						srcAddressBytes[i] = b;
						checksumBytes.add(b);
					}
					int srcAddress = ByteBuffer.wrap(srcAddressBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
					byte[] dstAddressBytes = new byte[4];
					for (int i = 0; i < 4; i++) {
						byte b = (byte) in.read();
						dstAddressBytes[i] = b;
						checksumBytes.add(b);
					}
					int dstAddress = ByteBuffer.wrap(dstAddressBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
					byte[] dataLengthBytes = new byte[2];
					for (int i = 0; i < 2; i++) {
						byte b = (byte) in.read();
						dataLengthBytes[i] = b;
						checksumBytes.add(b);
					}
					short dataLength = ByteBuffer.wrap(dataLengthBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
					byte[] headerChecksumBytes = new byte[2];
					for (int i = 0; i < 2; i++) {
						byte b = (byte) in.read();
						headerChecksumBytes[i] = b;
					}
					List<Byte> calculatedHeaderChecksum = Service.calculateChecksum(checksumBytes);
					if (headerChecksumBytes[0] == calculatedHeaderChecksum.get(0)
							&& headerChecksumBytes[1] == calculatedHeaderChecksum.get(1)) {
						byte[] dataBytes = new byte[dataLength];
						checksumBytes.clear();
						for (int i = 0; i < dataLength; i++) {
							byte b = (byte) in.read();
							dataBytes[i] = b;
							checksumBytes.add(b);
						}
						byte[] dataChecksumBytes = new byte[2];
						dataChecksumBytes[0] = (byte) in.read();
						dataChecksumBytes[1] = (byte) in.read();
						List<Byte> calculatedDataChecksum = Service.calculateChecksum(checksumBytes);
						if (dataChecksumBytes[0] == calculatedDataChecksum.get(0)
								&& dataChecksumBytes[1] == calculatedDataChecksum.get(1)) {
							ByteBuffer buffer = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN);
							byte dataFlagsByte = dataBytes[0];
							boolean isError = (dataFlagsByte & 1) != 0;
							boolean isResponse = (dataFlagsByte & 2) != 0;
							byte serviceIdByte = dataBytes[1];
							ObjectType objectType = ObjectType.getByCode(buffer.getShort(2));
							int objectId = buffer.getInt(4);
							PropertyId propertyId = PropertyId.getByCode(buffer.getShort(8));
							if (this.request.getServiceId() != serviceIdByte) {
								System.out.println("ServiceId of Response is not equals Request ServiceId.");
							} else {
								this.request.createResponse(isResponse, isError, isDatalogSupported,
										isNewDataloggerFilePresent, isSdCardFull, isSdCardPresent, isRccRestart,
										isMessagePending, srcAddress, dstAddress, objectType, propertyId, objectId,
										Arrays.copyOfRange(dataBytes, 10, dataBytes.length));
							}
						} else {
							throw new OpenemsException("DataChecksum wrong");
						}
					} else {
						throw new OpenemsException("HeaderChecksum wrong");
					}
				} else {
					throw new OpenemsException("Stream start not found.");
				}
				in.close();
			} else {
				throw new OpenemsException("Connection is not open!");
			}
		} catch (IOException e) {
			throw new OpenemsException("IOException: " + e.getMessage());
		}
	}
}
