package io.openems.impl.protocol.studer.internal.request;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public class ReadShortResponse extends ReadResponse<Short> {

	private final Short value;

	public ReadShortResponse(boolean isResponse, boolean isError, boolean isDatalogSupported,
			boolean isNewDataloggerFilePresent, boolean isSdCardFull, boolean isSdCardPresent, boolean isRccRestart,
			boolean isMessagePending, int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId, short value) {
		super(isResponse, isError, isDatalogSupported, isNewDataloggerFilePresent, isSdCardFull, isSdCardPresent,
				isRccRestart, isMessagePending, srcAddress, dstAddress, objectType, propertyId, objectId);
		this.value = value;
	}

	@Override
	public Short getValue() {
		return value;
	}

}
