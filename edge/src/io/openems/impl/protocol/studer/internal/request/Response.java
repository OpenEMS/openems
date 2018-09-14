package io.openems.impl.protocol.studer.internal.request;

import io.openems.impl.protocol.studer.internal.Request;
import io.openems.impl.protocol.studer.internal.Service;
import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

/**
 * Holds a response to a {@link Request} to a Studer device.
 *
 * @author stefan.feilmeier
 *
 * @param <T>
 */
public abstract class Response<T> extends Service {

	public Response(boolean isResponse, boolean isError, boolean isDatalogSupported, boolean isNewDataloggerFilePresent,
			boolean isSdCardFull, boolean isSdCardPresent, boolean isRccRestart, boolean isMessagePending,
			int srcAddress, int dstAddress, ObjectType objectType, PropertyId propertyId, int objectId) {
		this.isResponse = isResponse;
		this.isError = isError;
		this.isDatalogSupported = isDatalogSupported;
		this.isNewDataloggerFilePresent = isNewDataloggerFilePresent;
		this.isSdCardFull = isSdCardFull;
		this.isSdCardPresent = isSdCardPresent;
		this.isRccRestart = isRccRestart;
		this.isMessagePending = isMessagePending;
		this.srcAddress = srcAddress;
		this.dstAddress = dstAddress;
		this.objectType = objectType;
		this.objectId = objectId;
		this.propertyId = propertyId;
	}

	public boolean isResponse() {
		return isResponse;
	}

	public boolean isError() {
		return isError;
	}

	public boolean isDatalogSupported() {
		return isDatalogSupported;
	}

	public boolean isNewDataloggerFilePresent() {
		return isNewDataloggerFilePresent;
	}

	public boolean isSdCardFull() {
		return isSdCardFull;
	}

	public boolean isSdCardPresent() {
		return isSdCardPresent;
	}

	public boolean isRccRestart() {
		return isRccRestart;
	}

	public boolean isMessagePending() {
		return isMessagePending;
	}

	public int getSrcAddress() {
		return srcAddress;
	}

	public int getDstAddress() {
		return dstAddress;
	}

	public ObjectType getObjectType() {
		return objectType;
	}

	public PropertyId getPropertyId() {
		return propertyId;
	}

	public int getObjectId() {
		return objectId;
	}

}
