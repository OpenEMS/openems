package io.openems.api.channel;

import io.openems.api.value.Value;
import io.openems.core.databus.DataBus;

public class Channel {
	private DataBus dataBus = null;

	private Value value;

	private void setValue(Value value) {
		dataBus.channelValueUpdated(this);
		this.value = value;
	}
}
