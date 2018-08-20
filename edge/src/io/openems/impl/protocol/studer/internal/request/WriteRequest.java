package io.openems.impl.protocol.studer.internal.request;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import io.openems.impl.protocol.studer.internal.Request;
import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public abstract class WriteRequest<T> extends Request {

	private WriteResponse<T> response;

	public WriteRequest(int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId, int objectId) {
		this.srcAddress = srcAddress;
		this.dstAddress = dstAddress;
		this.objectType = objectType;
		this.propertyId = propertyId;
		this.objectId = objectId;
	}

	@Override
	protected byte getServiceId() {
		return (byte) 0x02;
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
		for (byte b : getValue()) {
			bytes.add(b);
		}
		return bytes;
	}

	protected abstract List<Byte> getValue();

	@Override
	protected void createResponse(boolean isResponse, boolean isError, boolean isDatalogSupported,
			boolean isNewDataloggerFilePresent, boolean isSdCardFull, boolean isSdCardPresent, boolean isRccRestart,
			boolean isMessagePending, int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId,
			int objectId, byte[] data) {
		if (isError || data.length > 0) {
			this.response = new WriteResponseError<T>(isResponse, isError, isDatalogSupported,
					isNewDataloggerFilePresent, isSdCardFull, isSdCardPresent, isRccRestart, isMessagePending,
					srcAddress, dstAddress, objectType, propertyId, objectId,
					ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getShort());
		} else {
			this.response = new WriteResponse<T>(isResponse, isError, isDatalogSupported, isNewDataloggerFilePresent,
					isSdCardFull, isSdCardPresent, isRccRestart, isMessagePending, srcAddress, dstAddress, objectType,
					propertyId, objectId);
		}
	}

	public WriteResponse<T> getResponse() {
		return response;
	}

}
