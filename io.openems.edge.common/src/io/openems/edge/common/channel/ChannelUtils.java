package io.openems.edge.common.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.base.CaseFormat;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.Disposable;
import io.openems.common.types.OptionsEnum;
import io.openems.common.types.Tuple2;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public final class ChannelUtils {

	private ChannelUtils() {
	}

	public record ChangedValue<C extends OpenemsComponent, T>(//
			C component, //
			ChannelId channelId, //
			Value<T> prevValue, //
			Value<T> newValue //
	) {
	}

	/**
	 * Adds a callback to the Channel, which is called on every update by
	 * nextProcessImage() call of the Channel.
	 *
	 * @param <T>        the type of the Component
	 * @param <V1>       the type of the first Channel Value
	 * @param <V2>       the type of the second Channel Value
	 * @param components the {@link OpenemsComponent OpenemsComponents}
	 * @param channelId1 the first {@link ChannelId}
	 * @param channelId2 the second {@link ChannelId}
	 * @param onUpdate   the callback {@link BiConsumer}
	 * @return a {@link Disposable} to remove the callback
	 */
	public static <T extends OpenemsComponent, V1, V2> Disposable subscribeOnUpdate(//
			final List<T> components, //
			final ChannelId channelId1, //
			final ChannelId channelId2, //
			final Consumer<Map<T, Tuple2<Value<V1>, Value<V2>>>> onUpdate //
	) {
		final var currentValues = new ConcurrentHashMap<T, Tuple2<Value<V1>, Value<V2>>>();
		final var subscription1 = ChannelUtils.<T, V1>subscribeOnUpdate(components, channelId1,
				(current, changedValue) -> {

					currentValues.compute(changedValue.component(), (t, valueValueTuple) -> {
						return (valueValueTuple == null ? new Tuple2<Value<V1>, Value<V2>>(null, null)
								: valueValueTuple) //
								.withA(changedValue.newValue());
					});

					onUpdate.accept(currentValues);
				});
		final var subscription2 = ChannelUtils.<T, V2>subscribeOnUpdate(components, channelId2,
				(tValueMap, changedValue) -> {

					currentValues.compute(changedValue.component(), (t, valueValueTuple) -> {
						return (valueValueTuple == null ? new Tuple2<Value<V1>, Value<V2>>(null, null)
								: valueValueTuple) //
								.withB(changedValue.newValue());
					});

					onUpdate.accept(currentValues);
				});

		return () -> {
			subscription1.dispose();
			subscription2.dispose();
		};
	}

	/**
	 * Adds a callback to the Channel, which is called on every update by
	 * nextProcessImage() call of the Channel.
	 *
	 * @param <T>        the type of the Component
	 * @param <V>        the type of the Channel Value
	 * @param components the {@link OpenemsComponent OpenemsComponents}
	 * @param channelId  the {@link ChannelId}
	 * @param onUpdate   the callback {@link BiConsumer}
	 * @return a {@link Disposable} to remove the callback
	 */
	public static <T extends OpenemsComponent, V> Disposable subscribeOnUpdate(//
			final List<T> components, //
			final ChannelId channelId, //
			final BiConsumer<Map<T, Value<V>>, ChangedValue<T, V>> onUpdate //
	) {
		final var currentValues = new ConcurrentHashMap<T, Value<V>>();
		final var subscriptions = components.stream() //
				.map(component -> ChannelUtils.<T, V>subscribeOnUpdate(component, channelId, value -> {
					final var prev = currentValues.put(component, value);
					onUpdate.accept(currentValues, new ChangedValue<>(component, channelId, prev, value));
				})) //
				.toList();
		return () -> subscriptions.forEach(Disposable::dispose);
	}

	/**
	 * Adds a callback to the Channel, which is called on every update by
	 * nextProcessImage() call of the Channel.
	 *
	 * @param <T>          the type of the Component
	 * @param <V>          the type of the Channel Value
	 * @param component    the {@link OpenemsComponent}
	 * @param channelId    the {@link ChannelId}
	 * @param subscription the callback {@link Consumer}
	 * @return a {@link Disposable} to remove the callback
	 */
	public static <T extends OpenemsComponent, V> Disposable subscribeOnUpdate(T component, ChannelId channelId,
			Consumer<Value<V>> subscription) {
		final Channel<V> channel = component.channel(channelId);
		channel.onUpdate(subscription);

		return () -> channel.removeOnUpdateCallback(subscription);
	}

	/**
	 * Adds a callback to the Channel, which is called on every setNextValue() call
	 * of the Channel.
	 *
	 * @param <T>        the type of the Component
	 * @param <V1>       the type of the first Channel Value
	 * @param <V2>       the type of the second Channel Value
	 * @param components the {@link OpenemsComponent OpenemsComponents}
	 * @param channelId1 the first {@link ChannelId}
	 * @param channelId2 the second {@link ChannelId}
	 * @param onUpdate   the callback {@link BiConsumer}
	 * @return a {@link Disposable} to remove the callback
	 */
	public static <T extends OpenemsComponent, V1, V2> Disposable subscribeOnSetNextValue(//
			final List<T> components, //
			final ChannelId channelId1, //
			final ChannelId channelId2, //
			final Consumer<Map<T, Tuple2<Value<V1>, Value<V2>>>> onUpdate //
	) {
		final var currentValues = new ConcurrentHashMap<T, Tuple2<Value<V1>, Value<V2>>>();

		final var subscription1 = ChannelUtils.<T, V1>subscribeOnSetNextValue(components, channelId1,
				(current, changedValue) -> {

					currentValues.compute(changedValue.component(), (t, valueValueTuple) -> {
						return (valueValueTuple == null ? new Tuple2<Value<V1>, Value<V2>>(null, null)
								: valueValueTuple) //
								.withA(changedValue.newValue());
					});

					onUpdate.accept(new HashMap<>(currentValues));
				});
		final var subscription2 = ChannelUtils.<T, V2>subscribeOnSetNextValue(components, channelId2,
				(tValueMap, changedValue) -> {

					currentValues.compute(changedValue.component(), (t, valueValueTuple) -> {
						return (valueValueTuple == null ? new Tuple2<Value<V1>, Value<V2>>(null, null)
								: valueValueTuple) //
								.withB(changedValue.newValue());
					});

					onUpdate.accept(new HashMap<>(currentValues));
				});

		return () -> {
			subscription1.dispose();
			subscription2.dispose();
		};
	}

	/**
	 * Adds a callback to the Channel, which is called on every setNextValue() call
	 * of the Channel.
	 *
	 * @param <T>        the type of the Component
	 * @param <V>        the type of the Channel Value
	 * @param components the {@link OpenemsComponent OpenemsComponents}
	 * @param channelId  the {@link ChannelId}
	 * @param onUpdate   the callback {@link BiConsumer}
	 * @return a {@link Disposable} to remove the callback
	 */
	public static <T extends OpenemsComponent, V> Disposable subscribeOnSetNextValue(//
			final List<T> components, //
			final ChannelId channelId, //
			final BiConsumer<Map<T, Value<V>>, ChangedValue<T, V>> onUpdate //
	) {
		final var currentValues = new ConcurrentHashMap<T, Value<V>>();
		final var subscriptions = components.stream() //
				.map(component -> ChannelUtils.<T, V>subscribeOnSetNextValue(component, channelId, value -> {
					final var prev = currentValues.put(component, value);
					onUpdate.accept(currentValues, new ChangedValue<>(component, channelId, prev, value));
				})) //
				.toList();
		return () -> subscriptions.forEach(Disposable::dispose);
	}

	/**
	 * Adds a callback to the Channel, which is called on every setNextValue() call
	 * of the Channel.
	 * 
	 * @param <T>          the type of the Component
	 * @param <V>          the type of the Channel Value
	 * @param component    the {@link OpenemsComponent}
	 * @param channelId    the {@link ChannelId}
	 * @param subscription the callback {@link Consumer}
	 * @return a {@link Disposable} to remove the callback
	 */
	public static <T extends OpenemsComponent, V> Disposable subscribeOnSetNextValue(T component, ChannelId channelId,
			Consumer<Value<V>> subscription) {
		final Channel<V> channel = component.channel(channelId);
		channel.onSetNextValue(subscription);

		subscription.accept(channel.getNextValue());

		return () -> channel.removeOnSetNextValueCallback(subscription);
	}

	/**
	 * Gets a {@link Record} with all {@link Channel} {@link Value}s.
	 * 
	 * @param <T>       the type of the {@link Record}. The names of the Members of
	 *                  the Record must match the Channel-Ids of the Component
	 * @param component the {@link OpenemsComponent}
	 * @param clazz     the Class of the {@link Record}
	 * @return an object of type T
	 * @throws OpenemsException if at least one Value is not available
	 */
	public static <T extends Record> T getValuesOrError(OpenemsComponent component, Class<T> clazz)
			throws OpenemsException {
		return getValues(component, clazz, false);
	}

	/**
	 * Gets a {@link Record} with all {@link Channel} {@link Value}s; or empty if at
	 * least one Value is not available.
	 * 
	 * @param <T>       the type of the {@link Record}. The names of the Members of
	 *                  the Record must match the Channel-Ids of the Component
	 * @param component the {@link OpenemsComponent}
	 * @param clazz     the Class of the {@link Record}
	 * @return an Optional object of type T
	 */
	public static <T extends Record> Optional<T> getValues(OpenemsComponent component, Class<T> clazz) {
		try {
			return Optional.ofNullable(getValues(component, clazz, false));

		} catch (OpenemsException e) {
			// will never happen
			return Optional.empty();
		}
	}

	/**
	 * Gets a {@link Record} with all {@link Channel} {@link Value}s; or null if at
	 * least one Value is not available.
	 * 
	 * @param <T>            the type of the {@link Record}. The names of the
	 *                       Members of the Record must match the Channel-Ids of the
	 *                       Component
	 * @param component      the {@link OpenemsComponent}
	 * @param clazz          the Class of the {@link Record}
	 * @param throwException true if an {@link OpenemsException} should be thrown on
	 *                       error; otherwise the method returns null in this case.
	 * @return an object of type T
	 * @throws OpenemsException on error if `throwException` is true
	 */
	private static <T extends Record> T getValues(OpenemsComponent component, Class<T> clazz, boolean throwException)
			throws OpenemsException {
		var params = new ArrayList<Object>();
		for (var cmp : clazz.getRecordComponents()) {
			// Get Channel object for each record component
			@SuppressWarnings("deprecation")
			final var channel = component._channel(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, cmp.getName()));
			if (channel == null) {
				if (throwException) {
					throw new OpenemsException(
							"Component [" + component.id() + "] does not have a Channel [" + cmp.getName() + "]");
				} else {
					return null;
				}
			}

			var value = channel.value().get();
			if (value == null) {
				if (throwException) {
					throw new OpenemsException("Component [" + component.id() + "] Channel [" + channel.channelId().id()
							+ "] value is UNDEFINED");
				} else {
					return null;
				}
			}

			params.add(value);
		}

		var types = Arrays.stream(clazz.getRecordComponents()) //
				.map(rc -> rc.getType()) //
				.toArray(Class<?>[]::new);
		try {
			var constructor = clazz.getDeclaredConstructor(types);
			constructor.setAccessible(true);
			return constructor.newInstance(params.toArray(Object[]::new));

		} catch (Exception e) {
			if (throwException) {
				throw new OpenemsException(e.getClass().getSimpleName() + ": " + e.getMessage());
			} else {
				return null;
			}
		}
	}

	/**
	 * Gets the Class Name of the Nature for the given Channel.
	 * 
	 * @param channel the {@link Channel}
	 * @return a name like "ElectricityMeter" or empty String if not found
	 */
	public static String getChannelNature(Channel<?> channel) {
		return Optional.ofNullable(channel.channelId().getClass().getEnclosingClass()) //
				.filter(c -> c != AbstractOpenemsComponent.class && c != ChannelId.class) //
				.map(Class::getSimpleName) //
				.orElse("");
	}

	/**
	 * Set next read value of a {@link Channel}.
	 * 
	 * <p>
	 * Use this method as a short form for `this.channel(XYZ).setNextValue(value)`.
	 * 
	 * @param component the {@link OpenemsComponent}
	 * @param channelId the {@link ChannelId}
	 * @param value     value to be set
	 * @throws IllegalArgumentException on error
	 */
	public static void setValue(OpenemsComponent component, ChannelId channelId, Object value)
			throws IllegalArgumentException {
		component.channel(channelId).setNextValue(value);
	}

	/**
	 * Set write value of a {@link EnumWriteChannel} if the read value is not equal.
	 * 
	 * <p>
	 * Use this method if you do not want to write a Channel on every cycle, but
	 * only if the Write-Values differs from the current Read-Value.
	 * 
	 * @param channel the {@link EnumWriteChannel}
	 * @param value   value to be set
	 * @throws OpenemsNamedException on error
	 */
	public static void setWriteValueIfNotRead(EnumWriteChannel channel, OptionsEnum value)
			throws OpenemsNamedException {
		if (Objects.equals(channel.value().get(), value.getValue())) {
			return;
		}
		channel.setNextWriteValue(value);
	}

	/**
	 * Set write value of a {@link IntegerWriteChannel} if the read value is not
	 * equal.
	 * 
	 * <p>
	 * Use this method if you do not want to write a Channel on every cycle, but
	 * only if the Write-Values differs from the current Read-Value.
	 * 
	 * @param channel the {@link IntegerWriteChannel}
	 * @param value   value to be set
	 * @throws OpenemsNamedException on error
	 */
	public static void setWriteValueIfNotRead(IntegerWriteChannel channel, Integer value) throws OpenemsNamedException {
		setWriteValueIfNotReadHelper(channel, value);
	}

	/**
	 * Set write value of a {@link LongWriteChannel} if the read value is not equal.
	 *
	 * <p>
	 * Use this method if you do not want to write a Channel on every cycle, but
	 * only if the Write-Values differs from the current Read-Value.
	 *
	 * @param channel the {@link LongWriteChannel}
	 * @param value   value to be set
	 * @throws OpenemsNamedException on error
	 */
	public static void setWriteValueIfNotRead(LongWriteChannel channel, Long value) throws OpenemsNamedException {
		setWriteValueIfNotReadHelper(channel, value);
	}

	/**
	 * Set write value of a {@link BooleanWriteChannel} if the read value is not
	 * equal.
	 * 
	 * <p>
	 * Use this method if you do not want to write a Channel on every cycle, but
	 * only if the Write-Values differs from the current Read-Value.
	 * 
	 * @param channel the {@link BooleanWriteChannel}
	 * @param value   value to be set
	 * @throws OpenemsNamedException on error
	 */
	public static void setWriteValueIfNotRead(BooleanWriteChannel channel, Boolean value) throws OpenemsNamedException {
		setWriteValueIfNotReadHelper(channel, value);
	}

	private static <T> void setWriteValueIfNotReadHelper(WriteChannel<T> channel, T value)
			throws OpenemsNamedException {
		if (Objects.equals(channel.value().get(), value)) {
			return;
		}
		channel.setNextWriteValue(value);
	}
}
