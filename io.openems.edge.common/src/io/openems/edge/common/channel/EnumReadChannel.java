package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class EnumReadChannel extends AbstractReadChannel<EnumDoc, Integer> {

	// Amount of values in a row to apply the value
	private final int debounce;

	// Counter of values in a row
	private int debounceCounter = 0;

	// Last requested value
	private Integer lastValue;

	protected EnumReadChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc, int debounce) {
		super(OpenemsType.INTEGER, component, channelId, channelDoc);
		this.debounce = debounce;
		this.debounceCounter = debounce;
	}

	protected EnumReadChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc,
			OptionsEnum initialValue, int debounce) {
		// Explicitly sets the initial Value
		super(OpenemsType.INTEGER, component, channelId, channelDoc.initialValue(initialValue));
		this.debounce = debounce;
	}

	protected EnumReadChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc) {
		this(component, channelId, channelDoc, 0);
	}

	protected EnumReadChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc,
			OptionsEnum initialValue) {
		this(component, channelId, channelDoc.initialValue(initialValue), 0);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void _setNextValue(Integer value) {
		if (this.debounce == 0 || value == null) {
			super._setNextValue(value);
			return;
		}

		var currentValueOpt = this.value().asOptional();
		if (currentValueOpt.isPresent() && currentValueOpt.get() != value) {

			/*
			 * Same values in a row to change the value.
			 */
			this.lastValue = this.lastValue == null ? value : this.lastValue;

			if (this.lastValue == value) {
				if (this.debounceCounter <= this.debounce) {
					this.debounceCounter++;
				}
			} else {
				this.debounceCounter = 0;
			}

			if (this.debounceCounter >= this.debounce) {
				super._setNextValue(value);
			}
			this.lastValue = value;
			return;
		}

		// Set initial value or previous value
		super._setNextValue(value);
		this.lastValue = null;
		this.debounceCounter = 0;
	}
}
