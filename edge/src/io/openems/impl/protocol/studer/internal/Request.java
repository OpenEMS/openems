package io.openems.impl.protocol.studer.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

/**
 * Wraps a request to a Studer device.
 *
 * @author stefan.feilmeier
 */
public abstract class Request extends Service {

	protected abstract byte getServiceId();

	protected abstract List<Byte> getData();

	protected List<Byte> getBytes() {
		List<Byte> bytes = new ArrayList<>();
		bytes.add((byte) 0xAA);
		byte frameFlags = 0x00;
		if (isMessagePending) {
			frameFlags |= 1;
		}
		if (isRccRestart) {
			frameFlags |= 2;
		}
		if (isSdCardPresent) {
			frameFlags |= 4;
		}
		if (isSdCardFull) {
			frameFlags |= 8;
		}
		if (isNewDataloggerFilePresent) {
			frameFlags |= 16;
		}
		if (isDatalogSupported) {
			frameFlags |= 32;
		}
		bytes.add(frameFlags);
		byte[] srcBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(srcAddress).array();
		for (byte b : srcBytes) {
			bytes.add(b);
		}
		byte[] dstBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(dstAddress).array();
		for (byte b : dstBytes) {
			bytes.add(b);
		}
		byte[] dataLengthBytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
				.putShort((short) (2 + getData().size())).array();
		for (byte b : dataLengthBytes) {
			bytes.add(b);
		}
		bytes.addAll(calculateChecksum(bytes.subList(1, bytes.size())));
		byte serviceFlag = 0x00;
		if (isError) {
			serviceFlag |= 1;
		}
		if (isResponse) {
			serviceFlag |= 2;
		}
		bytes.add(serviceFlag);
		bytes.add(getServiceId());
		bytes.addAll(getData());
		bytes.addAll(calculateChecksum(bytes.subList(14, bytes.size())));
		return bytes;
	}

	protected abstract void createResponse(boolean isResponse, boolean isError, boolean isDatalogSupported,
			boolean isNewDataloggerFilePresent, boolean isSdCardFull, boolean isSdCardPresent, boolean isRccRestart,
			boolean isMessagePending, int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId, byte[] data);;
}
