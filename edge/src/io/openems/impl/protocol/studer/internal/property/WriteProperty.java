package io.openems.impl.protocol.studer.internal.property;

import java.io.IOException;
import java.util.Optional;

import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.studer.StuderBridge;
import io.openems.impl.protocol.studer.internal.request.WriteRequest;

public interface WriteProperty<T> extends ReadProperty<T> {

	@Override
	public StuderWriteChannel<T> channel();

	public WriteRequest<T> writeRequest(int srcAddress, int dstAddress, T value);

	public default void writeValue(int srcAddress, int dstAddress, StuderBridge studerBridge) throws OpenemsException {
		try {
			T value;
			Optional<T> valueOptional = this.channel().writeShadowCopy();
			if (valueOptional.isPresent()) {
				value = valueOptional.get();
			} else {
				return;
			}
			System.out.println("Write value[" + value + "] to " + this.channel().address());
			WriteRequest<T> writeRequest = writeRequest(srcAddress, dstAddress, value);
			studerBridge.execute(writeRequest);
		} catch (IOException e) {
			throw new OpenemsException("Unable to write value", e);
		}
	}

}
