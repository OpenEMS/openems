package io.openems.impl.protocol.studer.internal.request;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public class WriteIntRequest extends WriteRequest<Integer> {

	private Integer value;

	public WriteIntRequest(int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId, int objectId,
			int value) {
		super(srcAddress, dstAddress, objectType, propertyId, objectId);
		this.value = value;
	}

	@Override
	protected List<Byte> getValue() {
		List<Byte> bytes = new ArrayList<>();
		for (byte b : ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()) {
			bytes.add(b);
		}
		return bytes;
	}

}
