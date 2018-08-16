package io.openems.impl.protocol.studer.internal.request;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public class ReadIntResponse extends ReadResponse<Integer> {

	private final Integer value;

	public ReadIntResponse(boolean isResponse, boolean isError, boolean isDatalogSupported,
			boolean isNewDataloggerFilePresent, boolean isSdCardFull, boolean isSdCardPresent, boolean isRccRestart,
			boolean isMessagePending, int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId, int value) {
		super(isResponse, isError, isDatalogSupported, isNewDataloggerFilePresent, isSdCardFull, isSdCardPresent,
				isRccRestart, isMessagePending, srcAddress, dstAddress, objectType, propertyId, objectId);
		this.value = value;
	}

	@Override
	public Integer getValue() {
		return value;
	}

}
