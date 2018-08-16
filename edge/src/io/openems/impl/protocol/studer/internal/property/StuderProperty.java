package io.openems.impl.protocol.studer.internal.property;

import io.openems.impl.protocol.studer.StuderChannel;

public interface StuderProperty<T> {
	public StuderChannel<T> channel();
}
