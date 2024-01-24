package io.openems.edge.common.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.CaseFormat;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public final class ChannelUtils {

	private ChannelUtils() {
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
