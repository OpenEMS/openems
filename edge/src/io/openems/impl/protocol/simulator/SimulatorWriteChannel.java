package io.openems.impl.protocol.simulator;

import io.openems.api.channel.WriteChannel;
import io.openems.api.thing.Thing;

public class SimulatorWriteChannel<T> extends WriteChannel<T> {

	public SimulatorWriteChannel(String id, Thing parent, T initialValue) {
		super(id, parent);
		updateValue(initialValue);
	}

	@Override
	public synchronized void shadowCopyAndReset() {
		super.shadowCopyAndReset();
		if (writeShadowCopy.isPresent()) {
			super.updateValue(writeShadowCopy.get());
		}
	}

	@Override
	public SimulatorWriteChannel<T> label(T value, String label) {
		// TODO Auto-generated method stub
		super.label(value, label);
		return this;
	}

}
