package io.openems.edge.io.gpio.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public class WriteChannelId extends AbstractGpioChannel implements ChannelId {

	public WriteChannelId(int gpio, String name) {
		super(gpio, name, Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.persistencePriority(PersistencePriority.HIGH));
	}
}