package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.common.session.Language;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.type.TextProvider;

import java.util.HashMap;
import java.util.Map;

class EnumChannelParameterProviderImpl<V extends Enum<V> & OptionsEnum> extends ChannelParameterProvider<Integer>
		implements EnumChannelParameterProvider<V> {
	private final Class<V> enumClass;
	private final Map<V, TextProvider> specificMappings;
	private TextProvider defaultText;

	EnumChannelParameterProviderImpl(Class<V> enumClass, ChannelId channelId) {
		super(channelId);
		this.enumClass = enumClass;
		this.specificMappings = new HashMap<>();
	}

	EnumChannelParameterProviderImpl(Class<V> enumClass, ChannelId channelId, Map<V, TextProvider> specificMappings,
			TextProvider defaultText) {
		super(channelId);
		this.enumClass = enumClass;
		this.specificMappings = specificMappings;
		this.defaultText = defaultText;
	}

	@Override
	public EnumChannelParameterProvider<V> defaultText(TextProvider text) {
		this.defaultText = text;
		return this;
	}

	@Override
	public EnumChannelParameterProvider<V> when(V value, TextProvider text) {
		this.specificMappings.put(value, text);
		return this;
	}

	@Override
	public EnumChannelParameterProvider<V> when(V[] values, TextProvider text) {
		for (V value : values) {
			this.specificMappings.put(value, text);
		}
		return this;
	}

	@Override
	public String getText(Language lang) {
		var value = this.getChannelValue();
		var enumValue = value != null ? OptionsEnum.getOption(this.enumClass, value) : null;

		var specificMapping = this.specificMappings.get(enumValue);
		if (specificMapping != null) {
			return specificMapping.getText(lang);
		}

		if (this.defaultText != null) {
			return this.defaultText.getText(lang);
		}

		return this.getChannelValueAsString();
	}

	@Override
	public ParameterProvider clone() {
		return new EnumChannelParameterProviderImpl<>(this.enumClass, this.channelId, this.specificMappings,
				this.defaultText);
	}
}