package io.openems.edge.common.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractDoc;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a single state. Changes to the value are reported to the
 * {@link StateCollectorChannel} "State" of the OpenEMS Component.
 */
public class StateChannel extends AbstractReadChannel<AbstractDoc<Boolean>, Boolean> {

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
	private final int debounce;
	private Debounce debounceMode = null;

	private int debounceCounter = 0;

	protected StateChannel(OpenemsComponent component, ChannelId channelId, AbstractDoc<Boolean> channelDoc,
			Level level, int debounce, Debounce debounceMode) {
		super(OpenemsType.BOOLEAN, component, channelId, channelDoc);
		this.level = level;
		this.debounce = debounce;
		this.debounceCounter = debounce;
		this.debounceMode = debounceMode;
	}

	/**
	 * Gets the Level of this {@link StateChannel}.
	 *
	 * @return the level
	 */
	public Level getLevel() {
		return this.level;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void _setNextValue(Boolean value) {
		// this can happen once in the beginning when called via super constructor
		if (this.debounceMode == null) {
			super._setNextValue(value);
			return;
		}

		switch (this.debounceMode) {
		case TRUE_VALUES_IN_A_ROW_TO_SET_TRUE:
			this.trueValuesInARowToSetTrue(value);
			break;

		case FALSE_VALUES_IN_A_ROW_TO_SET_FALSE:
			this.falseValuesInARowToSetFalse(value);
			break;

		case SAME_VALUES_IN_A_ROW_TO_CHANGE:
			var currentValueOpt = this.value().asOptional();
			if (!currentValueOpt.isPresent()) {
				super._setNextValue(value);
				return;
			}
			boolean currentValue = currentValueOpt.get();
			if (currentValue) {
				this.falseValuesInARowToSetFalse(value);
			} else {
				this.trueValuesInARowToSetTrue(value);
			}
			break;
		}
	}

	@SuppressWarnings("deprecation")
	private void trueValuesInARowToSetTrue(Boolean value) {
		if (value != null && value) {
			if (this.debounceCounter <= this.debounce) {
				this.debounceCounter++;
			}
		} else {
			this.debounceCounter = 0;
		}

		if (this.debounceCounter > this.debounce) {
			super._setNextValue(true);
		} else {
			super._setNextValue(false);
		}
	}

	@SuppressWarnings("deprecation")
	private void falseValuesInARowToSetFalse(Boolean value) {
		if (value != null && !value) {
			if (this.debounceCounter <= this.debounce) {
				this.debounceCounter++;
			}
		} else {
			this.debounceCounter = 0;
		}

		if (this.debounceCounter > this.debounce) {
			super._setNextValue(false);
		} else {
			super._setNextValue(true);
		}
	}
}
