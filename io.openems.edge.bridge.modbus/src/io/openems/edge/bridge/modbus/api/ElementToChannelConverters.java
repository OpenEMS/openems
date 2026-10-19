package io.openems.edge.bridge.modbus.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.converter.StaticConverters;
import io.openems.edge.common.type.TypeUtils;

import java.util.Objects;

// CHECKSTYLE:OFF: MethodName
public final class ElementToChannelConverters {

	/**
	 * Converts directly 1-to-1 between Element and Channel.
	 */
	public static final ElementToChannelConverter DIRECT_1_TO_1 = new ElementToChannelConverter(//
			// element -> channel
			value -> value, //
			// channel -> element
			value -> value);
	/**
	 * Applies a scale factor of -1. Converts value [1] to [0.1].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_MINUS_1 = new ElementToChannelScaleFactorConverter(-1);

	/**
	 * Applies a scale factor of -2. Converts value [1] to [0.01].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_MINUS_2 = new ElementToChannelScaleFactorConverter(-2);

	/**
	 * Applies a scale factor of -3. Converts value [1] to [0.001].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_MINUS_3 = new ElementToChannelScaleFactorConverter(-3);

	/**
	 * Applies a scale factor of 1. Converts value [1] to [10].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_1 = new ElementToChannelScaleFactorConverter(1);

	/**
	 * Applies a scale factor of 2. Converts value [1] to [100].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2 = new ElementToChannelScaleFactorConverter(2);

	/**
	 * Applies a scale factor of 3. Converts value [1] to [1000].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_3 = new ElementToChannelScaleFactorConverter(3);

	/**
	 * Converts only positive values from Element to Channel.
	 */
	public static final ElementToChannelConverter KEEP_POSITIVE = new ElementToChannelConverter(//
			// element -> channel
			StaticConverters.KEEP_POSITIVE, //
			// channel -> element
			value -> value);

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_2} and
	 * CONVERT_POSITIVE.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_POSITIVE = ElementToChannelConverter
			.chain(SCALE_FACTOR_2, KEEP_POSITIVE);

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_1} and
	 * CONVERT_POSITIVE.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_1_AND_KEEP_POSITIVE = ElementToChannelConverter
			.chain(SCALE_FACTOR_1, KEEP_POSITIVE);

	/**
	 * Inverts the value from Element to Channel.
	 */
	public static final ElementToChannelConverter INVERT = new ElementToChannelConverter(//
			// element -> channel
			StaticConverters.INVERT, //
			// channel -> element
			StaticConverters.INVERT);

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_2} and INVERT.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2_AND_INVERT = ElementToChannelConverter
			.chain(SCALE_FACTOR_2, INVERT);

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_MINUS_1} and
	 * {@link ElementToChannelConverters#INVERT}.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_MINUS_1_AND_INVERT = ElementToChannelConverter
			.chain(SCALE_FACTOR_MINUS_1, INVERT);

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_1} and
	 * {@link ElementToChannelConverters#INVERT}.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_1_AND_INVERT = ElementToChannelConverter
			.chain(SCALE_FACTOR_1, INVERT);

	/**
	 * Converts only negative values from Element to Channel and inverts them (makes
	 * the value positive).
	 */
	public static final ElementToChannelConverter KEEP_NEGATIVE_AND_INVERT = ElementToChannelConverter.chain(INVERT,
			KEEP_POSITIVE);

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_2} and @see
	 * {@link ElementToChannelConverters#KEEP_NEGATIVE_AND_INVERT}.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_NEGATIVE_AND_INVERT = ElementToChannelConverter
			.chain(SCALE_FACTOR_2, KEEP_NEGATIVE_AND_INVERT);

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_2} and @see
	 * {@link ElementToChannelConverters#INVERT}.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_NEGATIVE = ElementToChannelConverter
			.chain(SCALE_FACTOR_2_AND_KEEP_NEGATIVE_AND_INVERT, INVERT);

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_1} and
	 * CONVERT_NEGATIVE_AND_INVERT.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_1_AND_KEEP_NEGATIVE_AND_INVERT = ElementToChannelConverter
			.chain(SCALE_FACTOR_1, KEEP_NEGATIVE_AND_INVERT);

	private ElementToChannelConverters() {
		// private constructor to prevent instantiation
	}

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_1} and INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter SCALE_FACTOR_1_AND_INVERT_IF_TRUE(boolean invert) {
		return ElementToChannelConverter.chain(SCALE_FACTOR_1, INVERT_IF_TRUE(invert));
	}

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_2} and INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter SCALE_FACTOR_2_AND_INVERT_IF_TRUE(boolean invert) {
		return ElementToChannelConverter.chain(SCALE_FACTOR_2, INVERT_IF_TRUE(invert));
	}

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_3} and INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter SCALE_FACTOR_3_AND_INVERT_IF_TRUE(boolean invert) {
		return ElementToChannelConverter.chain(SCALE_FACTOR_3, INVERT_IF_TRUE(invert));
	}

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_MINUS_1} and
	 * INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(boolean invert) {
		return ElementToChannelConverter.chain(SCALE_FACTOR_MINUS_1, INVERT_IF_TRUE(invert));
	}

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_MINUS_2} and
	 * INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter SCALE_FACTOR_MINUS_2_AND_INVERT_IF_TRUE(boolean invert) {
		return ElementToChannelConverter.chain(SCALE_FACTOR_MINUS_2, INVERT_IF_TRUE(invert));
	}

	/**
	 * Applies {@link ElementToChannelConverters#SCALE_FACTOR_MINUS_3} and
	 * INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter SCALE_FACTOR_MINUS_3_AND_INVERT_IF_TRUE(boolean invert) {
		return ElementToChannelConverter.chain(SCALE_FACTOR_MINUS_3, INVERT_IF_TRUE(invert));
	}

	/**
	 * Sets the value to 'zero' if parameter is true; otherwise
	 * {@link #DIRECT_1_TO_1}.
	 *
	 * <ul>
	 * <li>true: set zero
	 * <li>false: apply {@link #DIRECT_1_TO_1}
	 * </ul>
	 *
	 * @param setZero true to set to null
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter SET_ZERO_IF_TRUE(boolean setZero) {
		if (setZero) {
			return new ElementToChannelConverter(//
					// element -> channel
					value -> 0, //
					// channel -> element
					value -> 0);
		}
		return DIRECT_1_TO_1;
	}

	/**
	 * Converts depending on the given parameter.
	 *
	 * <ul>
	 * <li>true: invert value
	 * <li>false: keep value (1-to-1)
	 * </ul>
	 *
	 * @param invert true if Converter should invert
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter INVERT_IF_TRUE(boolean invert) {
		if (invert) {
			return INVERT;
		}
		return DIRECT_1_TO_1;
	}

	/**
	 * Sets the null value for given {@link Integer} value.
	 * 
	 * @param defaultValue to ignore {@link Integer}
	 * @return null if actual value is equal to default value.
	 */
	public static ElementToChannelConverter SET_NULL_FOR_DEFAULT(int defaultValue) {
		return new ElementToChannelConverter(value -> {
			var v = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);

			if (v == null || v == defaultValue) {
				return null;
			}

			return v;
		});
	}

	/**
	 * Sets the null value for given {@link Long} value.
	 * 
	 * @param defaultValue to ignore {@link Long}
	 * @return null if actual value is equal to default value.
	 */
	public static ElementToChannelConverter SET_NULL_FOR_DEFAULT(long defaultValue) {
		return new ElementToChannelConverter(value -> {
			var v = TypeUtils.<Long>getAsType(OpenemsType.LONG, value);

			if (v == null || v == defaultValue) {
				return null;
			}

			return v;
		});
	}

	/**
	 * Sets the null value for given {@link Float} value.
	 * 
	 * @param defaultValue to ignore {@link Float}
	 * @return null if actual value is equal to default value.
	 */
	public static ElementToChannelConverter SET_NULL_FOR_DEFAULT(float defaultValue) {
		return new ElementToChannelConverter(value -> {
			var v = TypeUtils.<Float>getAsType(OpenemsType.FLOAT, value);

			if (v == null || Float.compare(v, defaultValue) == 0) {
				return null;
			}

			return v;
		});
	}

	/**
	 * Sets the null value for given {@link Double} value.
	 * 
	 * @param defaultValue to ignore {@link Double}
	 * @return null if actual value is equal to default value.
	 */
	public static ElementToChannelConverter SET_NULL_FOR_DEFAULT(double defaultValue) {
		return new ElementToChannelConverter(value -> {
			var v = TypeUtils.<Double>getAsType(OpenemsType.DOUBLE, value);

			if (v == null || Double.compare(v, defaultValue) == 0) {
				return null;
			}

			return v;
		});
	}
}
// CHECKSTYLE:ON: MethodName
