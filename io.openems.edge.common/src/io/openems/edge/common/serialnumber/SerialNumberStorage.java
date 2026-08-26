package io.openems.edge.common.serialnumber;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;

/**
 * Used to save serial numbers in a persistent storage.
 */
public interface SerialNumberStorage {

	public static final String SINGLETON_SERVICE_PID = "Core.SerialNumber";
	public static final String SINGLETON_COMPONENT_ID = "_serialNumber";

	/**
	 * Manually adds a serial number to this storage.
	 * 
	 * @param componentId the id of the component
	 * @param channelId   the id of the channel
	 * @param value       the serial number to add
	 */
	public void put(String componentId, String channelId, JsonElement value);

	/**
	 * Creates a on onChange listener used for channels, to automatically add the
	 * value if it changes.
	 * 
	 * <p>
	 * e. g.
	 * 
	 * <pre>
	 * final var channel = this.getChannel();
	 * channel.onChange(this.serialNumberSaver.createOnChangeListener(channel));
	 * </pre>
	 * </p>
	 * 
	 * @param <T>     the type of the channel
	 * @param channel the channel to add the onUpdate
	 * @return a listener which should be passed to the
	 *         {@link Channel#onUpdate(Consumer)} function
	 * 
	 * @see #put(String, String, JsonElement)
	 * @see Channel#onChange(BiConsumer)
	 * @see Channel#removeOnChangeCallback(BiConsumer)
	 */
	public default <T> BiConsumer<Value<T>, Value<T>> createOnChangeListener(Channel<?> channel) {
		return (oldValue, newValue) -> {
			newValue.ifPresent(v -> {
				final var address = channel.address();
				this.put(address.getComponentId(), address.getChannelId(), newValue.asJson());
			});
		};
	}

	/**
	 * Creates and adds a on onChange listener to a channel, to automatically add
	 * the value if it changes.
	 * 
	 * <p>
	 * e. g.
	 * 
	 * <pre>
	 * final var channel = this.getChannel();
	 * this.serialNumberSaver.createAndAddOnChangeListener(channel);
	 * </pre>
	 * </p>
	 * 
	 * @param <T>     the type of the channel
	 * @param channel the channel to add the onUpdate
	 * @return the listener which which got added to the channels onChange event; to
	 *         remove the listener call
	 *         {@link Channel#removeOnChangeCallback(BiConsumer)} and pass the
	 *         returned instance of this method
	 * 
	 * @see #put(String, String, JsonElement)
	 * @see Channel#onChange(BiConsumer)
	 * @see Channel#removeOnChangeCallback(BiConsumer)
	 */
	public default <T> BiConsumer<Value<T>, Value<T>> createAndAddOnChangeListener(Channel<T> channel) {
		return channel.onChange(this.createOnChangeListener(channel));
	}

}
