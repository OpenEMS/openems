package io.openems.impl.protocol.studer.internal.request;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.openems.impl.protocol.studer.internal.Request;
import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public abstract class ReadRequest<T> extends Request {

	public ReadRequest(int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId, int objectId) {
		this.srcAddress = srcAddress;
		this.dstAddress = dstAddress;
		this.objectType = objectType;
		this.propertyId = propertyId;
		this.objectId = objectId;
	}

	@Override
	protected byte getServiceId() {
		return (byte) 0x01;
	}

	@Override
	protected List<Byte> getData() {
		List<Byte> bytes = new ArrayList<>();
		byte[] objectTypeBytes = objectType.getByte();
		for (byte b : objectTypeBytes) {
			bytes.add(b);
		}
		byte[] objectIdBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(objectId).array();
		for (byte b : objectIdBytes) {
			bytes.add(b);
		}
		byte[] propertyIdBytes = propertyId.getByte();
		for (byte b : propertyIdBytes) {
			bytes.add(b);
		}
		return bytes;
	}

	public abstract ReadResponse<T> getResponse();
}
