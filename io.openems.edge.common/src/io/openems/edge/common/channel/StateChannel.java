package io.openems.edge.common.channel;

import java.util.Optional;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractDoc;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a single state. Changes to the value are reported to the
 * {@link StateCollectorChannel} "State" of the OpenEMS Component.
 */
public class StateChannel extends AbstractReadChannel<AbstractDoc<Boolean>, Boolean> {

	private final Level level;
	private final int debounce;
	private Debounce debounceMode = null;

	private int debounceCounter = 0;

	protected StateChannel(OpenemsComponent component, ChannelId channelId, AbstractDoc<Boolean> channelDoc,
			Level level, int debounce, Debounce debounceMode) {
		super(OpenemsType.BOOLEAN, component, channelId, channelDoc, false);
		this.level = level;
		this.debounce = debounce;
		this.debounceMode = debounceMode;
	}

	/**
	 * Gets the Level of this {@link StateChannel}.
	 * 
	 * @return the level
	 */
	public Level getLevel() {
		return level;
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
			Optional<Boolean> currentValueOpt = this.value().asOptional();
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
