package io.openems.edge.ess.sinexcel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public class ChannelHandler {

	private static class Listener<T> {
		protected final OpenemsComponent component;
		protected final ChannelId channelId;
		protected final BiConsumer<Value<T>, Value<T>> callback;

		public Listener(OpenemsComponent component, ChannelId channelId, BiConsumer<Value<T>, Value<T>> callback) {
			super();
			this.component = component;
			this.channelId = channelId;
			this.callback = callback;
		}
	}

	private final EssSinexcel parent;

	private final List<Listener<?>> listeners = new ArrayList<>();

	public ChannelHandler(EssSinexcel parent) {
		this.parent = parent;
	}

	/**
	 * Called on Component activate().
	 * 
	 * @param battery the {@link Battery}
	 */
	public void activate(Battery battery) {
		this.<Integer>addListener(battery, Battery.ChannelId.SOC, (oldValue, newValue) -> {
			this.parent._setSoc(newValue.get());
			this.parent.channel(EssSinexcel.ChannelId.BAT_SOC).setNextValue(newValue.get());
		});
		this.<Integer>addListener(battery, Battery.ChannelId.VOLTAGE, (oldValue, newValue) -> {
			this.parent.channel(EssSinexcel.ChannelId.BAT_VOLTAGE).setNextValue(newValue.get());
		});
		this.<Integer>addListener(battery, Battery.ChannelId.MIN_CELL_VOLTAGE, (oldValue, newValue) -> {
			this.parent._setMinCellVoltage(newValue.get());
		});
		this.<Integer>addListener(battery, Battery.ChannelId.CAPACITY, (oldValue, newValue) -> {
			this.parent._setCapacity(newValue.get());
		});
	}

	/**
	 * Called on deactivate(). Remove all callbacks from Channels.
	 */
	public void deactivate() {
		for (Listener<?> listener : this.listeners) {
			Channel<?> channel = listener.component.channel(listener.channelId);
			channel.removeOnChangeCallback(listener.callback);
		}
	}

	/**
	 * Adds a Listener. Also applies the callback once to make sure it applies
	 * already existing values.
	 * 
	 * @param <T>       the Channel value type
	 * @param component the Component - Battery or BatteryInverter
	 * @param channelId the ChannelId
	 * @param callback  the callback
	 */
	private <T> void addListener(OpenemsComponent component, ChannelId channelId,
			BiConsumer<Value<T>, Value<T>> callback) {
		this.listeners.add(new Listener<T>(component, channelId, callback));
		Channel<T> channel = component.channel(channelId);
		channel.onChange(callback);
		callback.accept(null, channel.getNextValue());
	}

}
