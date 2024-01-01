package io.openems.edge.common.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a single state. Changes to the value are reported to the
 * {@link StateCollectorChannel} "State" of the OpenEMS Component.
 */
public class StateChannel extends BooleanReadChannel {

	/**
	 * OnInit-Function for {@link StateChannelDoc}. The StateChannel gets set to
	 * true if any of the Source {@link BooleanReadChannel}s is true.
	 */
	public static class TriggerOnAny implements Consumer<Channel<Boolean>> {

		private final Logger log = LoggerFactory.getLogger(TriggerOnAny.class);
		private final ChannelId[] sourceChannelIds;

		public TriggerOnAny(ChannelId... sourceChannelIds) {
			this.sourceChannelIds = sourceChannelIds;
		}

		@Override
		public void accept(Channel<Boolean> channel) {
			// Create shared onChangeCallback
			final var parent = channel.getComponent();
			final BiConsumer<Value<Boolean>, Value<Boolean>> onChangeCallback = (oldValue, newValue) -> {
				List<String> activeSourceStates = new ArrayList<>();
				for (ChannelId sourceChannelId : this.sourceChannelIds) {
					Channel<Boolean> sourceChannel = parent.channel(sourceChannelId);
					if (sourceChannel.value().orElse(false)) {
						activeSourceStates.add(sourceChannelId.doc().getText());
						break;
					}
				}
				if (activeSourceStates.isEmpty()) {
					// No active Source StateChannels
					channel.setNextValue(false);
				} else {
					channel.setNextValue(!activeSourceStates.isEmpty());
					// TODO replace with OpenemsComponent.logInfo() once #1329 is merged
					this.log.info("Setting [" + channel.channelId().id() + "] because of ["
							+ String.join(", ", activeSourceStates) + "]");
				}
			};

			// Register callback on each Source-Channel
			for (ChannelId sourceChannelId : this.sourceChannelIds) {
				Channel<Boolean> sourceChannel = parent.channel(sourceChannelId);
				sourceChannel.onChange(onChangeCallback);
			}
		}
	}

	private final Level level;

	protected StateChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc, Level level,
			int debounce, Debounce debounceMode) {
		super(component, channelId, //
				(BooleanDoc) channelDoc.initialValue(false), // -> StateChannels are always `false` by default
				debounce, debounceMode);
		this.level = level;
	}

	/**
	 * Gets the Level of this {@link StateChannel}.
	 *
	 * @return the level
	 */
	public Level getLevel() {
		return this.level;
	}

}
