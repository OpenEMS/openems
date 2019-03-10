package io.openems.edge.common.channel.doc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.common.channel.ShortReadChannel;
import io.openems.edge.common.channel.ShortWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class OpenemsTypeDoc extends Doc {

	private final Logger log = LoggerFactory.getLogger(OpenemsTypeDoc.class);

	public OpenemsTypeDoc(OpenemsType type) {
		this.type(type);
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
		switch (this.getType2()) {
		case BOOLEAN:
			switch (this.getAccessMode()) {
			case READ_ONLY:
				return new BooleanWriteChannel(component, channelId);
			case READ_WRITE:
			case WRITE_ONLY:
				return new BooleanReadChannel(component, channelId);
			}
			// TODO StateChannel
		case DOUBLE:
			switch (this.getAccessMode()) {
			case READ_ONLY:
				return new DoubleWriteChannel(component, channelId);
			case READ_WRITE:
			case WRITE_ONLY:
				return new DoubleReadChannel(component, channelId);
			}
		case FLOAT:
			switch (this.getAccessMode()) {
			case READ_ONLY:
				return new FloatWriteChannel(component, channelId);
			case READ_WRITE:
			case WRITE_ONLY:
				return new FloatReadChannel(component, channelId);
			}
		case INTEGER:
			switch (this.getAccessMode()) {
			case READ_ONLY:
				return new IntegerWriteChannel(component, channelId);
			case READ_WRITE:
			case WRITE_ONLY:
				return new IntegerReadChannel(component, channelId);
			}
		case LONG:
			switch (this.getAccessMode()) {
			case READ_ONLY:
				return new LongWriteChannel(component, channelId);
			case READ_WRITE:
			case WRITE_ONLY:
				return new LongReadChannel(component, channelId);
			}
		case SHORT:
			switch (this.getAccessMode()) {
			case READ_ONLY:
				return new ShortWriteChannel(component, channelId);
			case READ_WRITE:
			case WRITE_ONLY:
				return new ShortReadChannel(component, channelId);
			}
		case STRING:
			switch (this.getAccessMode()) {
			case READ_ONLY:
				return new StringWriteChannel(component, channelId);
			case READ_WRITE:
			case WRITE_ONLY:
				return new StringReadChannel(component, channelId);
			}
		case ENUM:
			// TODO is already handled by doc.hasOptions() above.
//			return new EnumReadChannel(this, channelId);
		}
		throw new IllegalArgumentException(
				"Unable to initialize Channel-ID [" + channelId.id() + "]. OpenemsType is unknown!");
	}

}
