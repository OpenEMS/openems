package io.openems.impl.protocol.studer.internal.request;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public class ReadShortRequest extends ReadRequest<Short> {

	private ReadShortResponse response;

	public ReadShortRequest(int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId) {
		super(srcAddress, dstAddress, objectType, propertyId, objectId);
	}

	@Override
	protected void createResponse(boolean isResponse, boolean isError, boolean isDatalogSupported,
			boolean isNewDataloggerFilePresent, boolean isSdCardFull, boolean isSdCardPresent, boolean isRccRestart,
			boolean isMessagePending, int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId, byte[] data) {
		this.response = new ReadShortResponse(isResponse, isError, isDatalogSupported, isNewDataloggerFilePresent,
				isSdCardFull, isSdCardPresent, isRccRestart, isMessagePending, srcAddress, dstAddress, objectType,
				propertyId, objectId, ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getShort());
	}

	@Override
	public ReadShortResponse getResponse() {
		return response;
	}

}
