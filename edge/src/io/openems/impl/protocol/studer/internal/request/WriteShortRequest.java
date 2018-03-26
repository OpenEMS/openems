package io.openems.impl.protocol.studer.internal.request;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public class WriteShortRequest extends WriteRequest<Short> {

	private short value;

	public WriteShortRequest(int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId, int objectId,
			short value) {
		super(srcAddress, dstAddress, objectType, propertyId, objectId);
		this.value = value;
	}

	@Override
	protected List<Byte> getValue() {
		List<Byte> bytes = new ArrayList<>();
		for (byte b : ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()) {
			bytes.add(b);
		}
		return bytes;
	}

}
