package io.openems.impl.protocol.studer.internal.request;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public class WriteFloatRequest extends WriteRequest<Float> {

	private float value;

	public WriteFloatRequest(int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId, int objectId,
			float value) {
		super(srcAddress, dstAddress, objectType, propertyId, objectId);
		this.value = value;
	}

	@Override
	protected List<Byte> getValue() {
		List<Byte> bytes = new ArrayList<>();
		for (byte b : ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()) {
			bytes.add(b);
		}
		return bytes;
	}

}
