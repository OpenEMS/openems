package io.openems.impl.protocol.studer.internal.property;

import java.io.IOException;

import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.studer.StuderBridge;
import io.openems.impl.protocol.studer.StuderChannel;
import io.openems.impl.protocol.studer.internal.request.ReadRequest;
import io.openems.impl.protocol.studer.internal.request.ReadResponse;

public interface ReadProperty<T> extends StuderProperty<T> {

	public default void updateValue(int srcAddress, int dstAddress, StuderBridge studerBridge) throws OpenemsException {
		try {
			ReadRequest<T> readRequest = readRequest(srcAddress, dstAddress);
			studerBridge.execute(readRequest);
			ReadResponse<T> response = readRequest.getResponse();
			T value = response.getValue();
			StuderChannel<T> channel = channel();
			if (channel == null) {
				return;
			} else if (channel instanceof StuderReadChannel) {
				((StuderReadChannel<T>) channel).updateValue(value);
			} else if (channel instanceof StuderWriteChannel) {
				((StuderWriteChannel<T>) channel).updateValue(value);
			} else {
				throw new OpenemsException("Unable to set value [" + value + "]. Channel [" + channel.address()
						+ "] is no StuderReadChannel or StuderWriteChannel.");
			}
		} catch (IOException e) {
			throw new OpenemsException("Unable to update value", e);
		}
	}

	public ReadRequest<T> readRequest(int srcAddress, int dstAddress);
}
