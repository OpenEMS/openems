package io.openems.impl.protocol.studer.internal.request;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public abstract class ReadResponse<T> extends Response<T> {

	public ReadResponse(boolean isResponse, boolean isError, boolean isDatalogSupported,
			boolean isNewDataloggerFilePresent, boolean isSdCardFull, boolean isSdCardPresent, boolean isRccRestart,
			boolean isMessagePending, int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId) {
		super(isResponse, isError, isDatalogSupported, isNewDataloggerFilePresent, isSdCardFull, isSdCardPresent,
				isRccRestart, isMessagePending, srcAddress, dstAddress, objectType, propertyId, objectId);
	}

	public abstract T getValue();

}
