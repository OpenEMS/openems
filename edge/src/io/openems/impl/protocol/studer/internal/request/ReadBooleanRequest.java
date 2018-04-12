package io.openems.impl.protocol.studer.internal.request;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public class ReadBooleanRequest extends ReadRequest<Boolean> {

	private ReadBooleanResponse response;

	public ReadBooleanRequest(int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId) {
		super(srcAddress, dstAddress, objectType, propertyId, objectId);
	}

	@Override
	protected void createResponse(boolean isResponse, boolean isError, boolean isDatalogSupported,
			boolean isNewDataloggerFilePresent, boolean isSdCardFull, boolean isSdCardPresent, boolean isRccRestart,
			boolean isMessagePending, int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId, byte[] data) {
		response = new ReadBooleanResponse(isResponse, isError, isDatalogSupported, isNewDataloggerFilePresent,
				isSdCardFull, isSdCardPresent, isRccRestart, isMessagePending, srcAddress, dstAddress, objectType,
				propertyId, objectId, data[0] == 0x01 ? true : false);
	}

	@Override
	public ReadBooleanResponse getResponse() {
		return response;
	}

}
