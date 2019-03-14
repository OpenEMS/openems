package io.openems.edge.common.channel.doc;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class OptionsEnumDoc extends Doc {

	public OptionsEnumDoc(OptionsEnum[] options) {
		this.type(OpenemsType.INTEGER);
		this.options(options);
	}

	/**
	 * Creates an instance of {@link Channel} for the given Channel-ID using its
	 * Channel-{@link Doc}.
	 * 
	 * @param channelId the Channel-ID
	 * @return the Channel
	 */
	@Override
	public Channel<?> createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.doc.ChannelId channelId) {
		switch (this.getAccessMode()) {
		case READ_ONLY:
			return new EnumWriteChannel(component, channelId, this.getUndefinedOption());
		case READ_WRITE:
		case WRITE_ONLY:
			return new EnumReadChannel(component, channelId, this.getUndefinedOption());
		}
		throw new IllegalArgumentException(
				"Unable to initialize Channel-ID [" + channelId.id() + "] from OptionsEnumDoc!");
	}

	/**
	 * Get the Undefined-Option.
	 * 
	 * @return
	 */
	private OptionsEnum getUndefinedOption() {
		if (this.options.isEmpty()) {
			return null;
		}
		OptionsEnum undefined = this.options.values().iterator().next().getUndefined();
		return undefined;
	}
}
