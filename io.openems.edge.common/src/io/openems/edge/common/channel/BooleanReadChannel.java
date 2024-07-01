package io.openems.edge.common.channel;

import io.openems.common.channel.Debounce;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Boolean>, Boolean> {

	private final int debounce;
	private Debounce debounceMode = null;

	private int debounceCounter = 0;

	protected BooleanReadChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc, int debounce,
			Debounce debounceMode) {
		super(OpenemsType.BOOLEAN, component, channelId, channelDoc);
		this.debounce = debounce;
		this.debounceCounter = debounce;
		this.debounceMode = debounceMode;
	}

	protected BooleanReadChannel(OpenemsComponent component, ChannelId channelId, BooleanDoc channelDoc) {
		this(component, channelId, channelDoc, 0, null);
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
