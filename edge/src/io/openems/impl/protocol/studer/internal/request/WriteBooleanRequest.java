package io.openems.impl.protocol.studer.internal.request;

import java.util.ArrayList;
import java.util.List;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public class WriteBooleanRequest extends WriteRequest<Boolean> {

	private Boolean value;

	public WriteBooleanRequest(int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId, boolean value) {
		super(srcAddress, dstAddress, objectType, propertyId, objectId);
		this.value = value;
	}

	@Override
	protected List<Byte> getValue() {
		List<Byte> bytes = new ArrayList<>();
		bytes.add(value ? (byte) 0x01 : (byte) 0x00);
		return bytes;
	}

}
