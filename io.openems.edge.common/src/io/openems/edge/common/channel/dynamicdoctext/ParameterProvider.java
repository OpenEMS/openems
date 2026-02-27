package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.common.session.Language;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public interface ParameterProvider {
	/**
	 * Links this parameter to this component. Must only be called once. Create a
	 * copy with clone() before calling this method.
	 * 
	 * @param component The component to register in
	 */
	public void init(OpenemsComponent component);

	/**
	 * Calculates and returns the current text.
	 * 
	 * @param lang Language used for translation fetching
	 * @return Calculated text as string
	 */
	public String getText(Language lang);

	/**
	 * Creates a cloned instance and returns it.
	 * 
	 * @return Cloned instance
	 */
	public ParameterProvider clone();

	/**
	 * Creates a parameter that just returns the value of the given channel.
	 * 
	 * @param channelId channelId that should be used to get the value from
	 * @return ParameterProvider
	 */
	public static ParameterProvider byChannel(ChannelId channelId) {
		return new DefaultChannelParameterProviderImpl<>(channelId);
	}

	/**
	 * Creates a parameter that returns the value of the given enum channel.
	 * 
	 * @param enumClass Enum class (for example GoodweType.class)
	 * @param channelId channelId that should be used to get the value from
	 * @param <V> Enum type
	 * @return ParameterProvider
	 */
	public static <V extends Enum<V> & OptionsEnum> EnumChannelParameterProvider<V> byEnumChannel(//
			Class<V> enumClass, ChannelId channelId) {
		return new EnumChannelParameterProviderImpl<>(enumClass, channelId);
	}

	/**
	 * Creates a parameter that returns the value of the given number channel.
	 *
	 * @param channelId channelId that should be used to get the value from
	 * @param <V> Number type
	 * @return ParameterProvider
	 */
	public static <V extends Number> NumberChannelParameterProvider<V> byNumberChannel(ChannelId channelId) {
		return new NumberChannelParameterProviderImpl<>(channelId);
	}

	/**
	 * Creates a parameter that returns the value of the given string channel.
	 *
	 * @param channelId channelId that should be used to get the value from
	 * @return ParameterProvider
	 */
	public static StringChannelParameterProvider byStringChannel(ChannelId channelId) {
		return new StringChannelParameterProviderImpl(channelId);
	}

	/**
	 * Creates a parameter that just returns the given static value.
	 *
	 * @param value Static value that is always returned
	 * @return ParameterProvider
	 */
	public static ParameterProvider staticValue(String value) {
		return new StaticParameterProvider(value);
	}
}