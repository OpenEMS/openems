package io.openems.edge.common.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import io.openems.common.types.OptionsEnum;

public class DynamicDocText {

	/**
	 * Creates an Enum Builder.
	 * 
	 * @param clazz the enum class
	 * @param <E>   the enum class
	 * @return the Builder
	 */
	public static <E extends Enum<E> & OptionsEnum> EnumBuilder<E> fromEnumChannel(Class<E> clazz) {
		return new EnumBuilder<>(clazz, value -> OptionsEnum.getOption(clazz, value));
	}

	/**
	 * Creates a Number Builder.
	 *
	 * @param clazz the class
	 * @param <N>   the class that should extend Number
	 * @return the Builder
	 */
	public static <N extends Number> NumberBuilder<N> fromNumberChannel(Class<N> clazz) {
		return new NumberBuilder<>(clazz);
	}

	/**
	 * Creates a String Builder.
	 *
	 * @return the Builder
	 */
	public static StringBuilder fromStringChannel() {
		return new StringBuilder(String.class);
	}

	public abstract static class Builder<E, V> {

		private final Class<E> clazz;
		protected String defaultText;

		private Builder(Class<E> clazz) {
			this.clazz = clazz;
			this.defaultText = "";
		}

		/**
		 * Sets the default translationKey.
		 * 
		 * @param translationKey translationKey to display
		 * @return this Builder instance
		 */
		public Builder<E, V> defaultText(String translationKey) {
			this.defaultText = translationKey;
			return this;
		}

		/**
		 * Builds and returns a function that maps values to the specific text.
		 *
		 * @return the function
		 */
		public abstract Function<V, String> build();

		protected Class<E> getClazz() {
			return this.clazz;
		}
	}

	public static final class EnumBuilder<E extends Enum<E>> extends Builder<E, Integer> {
		record EnumTextItem<E extends Enum<E>>(Predicate<E> predicate, String translationKey) {
		}

		private final HashMap<List<E>, String> specificTextsMapping;
		private final Function<Integer, E> converter;

		private EnumBuilder(Class<E> clazz, Function<Integer, E> converter) {
			super(clazz);
			this.converter = converter;
			this.specificTextsMapping = new HashMap<>();
		}

		/**
		 * Maps specific enum values to a translationKey.
		 *
		 * @param translationKey translationKey to display for the specific values
		 * @param values         one or more enum vales
		 * @return this Builder instance
		 */
		public EnumBuilder<E> when(String translationKey, @SuppressWarnings("unchecked") E... values) {
			this.specificTextsMapping.put(Arrays.asList(values), translationKey);
			return this;
		}

		@Override
		public Function<Integer, String> build() {
			return (e) -> {
				if (e == null) {
					return this.defaultText;
				}
				var value = this.converter.apply(e);

				return this.specificTextsMapping.entrySet().stream() //
						.filter(entry -> entry.getKey().contains(value)) //
						.map(Map.Entry::getValue) //
						.findFirst() //
						.orElse(this.defaultText);
			};
		}
	}

	public static final class NumberBuilder<N extends Number> extends Builder<N, N> {

		record NumberTextItem<N extends Number>(Predicate<N> predicate, String translationKey) {
		}

		private final List<NumberTextItem<N>> predicates;

		private NumberBuilder(Class<N> clazz) {
			super(clazz);
			this.predicates = new ArrayList<>();
		}

		/**
		 * Maps a range of numbers to a translationKey.
		 *
		 * @param translationKey translationKey to display the channel when value is in
		 *                       range
		 * @param min            the minimum border (inclusive)
		 * @param max            the maximum border (inclusive)
		 * @return this Builder instance
		 */
		public NumberBuilder<N> whenIsInRange(String translationKey, N min, N max) {
			this.addingToList(isInRange(min, max), translationKey);
			return this;
		}

		/**
		 * Maps the minimum range of a number to a translationKey.
		 *
		 * @param translationKey translationKey to display the channel when value is in
		 *                       range
		 * @param min            the minimum border to show the translation (inclusive)
		 * @return this Builder instance
		 */
		public NumberBuilder<N> whenIsAtLeast(String translationKey, N min) {
			this.addingToList(isAtLeast(min), translationKey);
			return this;
		}

		/**
		 * Maps the maximum range of a number to a translationKey.
		 *
		 * @param translationKey translationKey to display the channel when value is in
		 *                       range
		 * @param max            the maximum border to show the translation (inclusive)
		 * @return this Builder instance
		 */
		public NumberBuilder<N> whenIsAtMost(String translationKey, N max) {
			this.addingToList(isAtMost(max), translationKey);
			return this;
		}

		/**
		 * Maps negative values to a translationKey.
		 *
		 * @param translationKey translationKey to display the channel when value is in
		 *                       range
		 * @return this Builder instance
		 */
		public NumberBuilder<N> whenIsNegative(String translationKey) {
			this.addingToList(isNegative(), translationKey);
			return this;
		}

		/**
		 * Maps positive values to a translationKey.
		 *
		 * @param translationKey translationKey to display the channel when value is in
		 *                       range
		 * @return this Builder instance
		 */
		public NumberBuilder<N> whenIsPositive(String translationKey) {
			this.addingToList(isPositive(), translationKey);
			return this;
		}

		private void addingToList(Predicate<N> predicate, String translationKey) {
			this.predicates.add(new NumberTextItem<>(predicate, translationKey));
		}

		private static <N extends Number> Predicate<N> isInRange(N min, N max) {
			return e -> {
				return min.doubleValue() <= e.doubleValue() //
						&& max.doubleValue() >= e.doubleValue();
			};
		}

		private static <N extends Number, N2 extends Number> Predicate<N> isAtLeast(N2 min) {
			return e -> {
				return min.doubleValue() <= e.doubleValue();
			};
		}

		private static <N extends Number, N2 extends Number> Predicate<N> isAtMost(N2 max) {
			return e -> {
				return max.doubleValue() >= e.doubleValue();
			};
		}

		private static <N extends Number> Predicate<N> isNegative() {
			return NumberBuilder.<N, Integer>isAtLeast(0).negate();
		}

		private static <N extends Number> Predicate<N> isPositive() {
			return isAtLeast(0);
		}

		@Override
		public Function<N, String> build() {
			return (e) -> {
				if (e == null) {
					return this.defaultText;
				}
				return this.predicates.stream() //
						.filter(p -> p.predicate.test(e)) //
						.map(NumberTextItem::translationKey) //
						.findFirst() //
						.orElse(this.defaultText);
			};
		}
	}

	public static final class StringBuilder extends Builder<String, String> {

		record StringTextItem(Predicate<String> predicate, String translationKey) {
		}

		private final List<StringTextItem> predicates;

		private StringBuilder(Class<String> clazz) {
			super(clazz);
			this.predicates = new ArrayList<>();
		}

		/**
		 * Maps specific strings to a translationKey.
		 *
		 * @param translationKey translationKey to display for the specific values
		 * @param values         one or more strings
		 * @return this Builder instance
		 */
		public StringBuilder when(String translationKey, String... values) {
			this.addingToList(equalsAnyOf(List.of(values)), translationKey);
			return this;
		}

		/**
		 * Maps strings that are in the value of the channel to a translationKey.
		 *
		 * @param translationKey translationKey to display for the specific strings that
		 *                       are contained by the value
		 * @param values         one or more strings
		 * @return this Builder instance
		 */
		public StringBuilder whenStringContains(String translationKey, String... values) {
			this.addingToList(containsAnyOf(List.of(values)), translationKey);
			return this;
		}

		private void addingToList(Predicate<String> predicate, String translationKey) {
			this.predicates.add(new StringTextItem(predicate, translationKey));
		}

		private static Predicate<String> equalsAnyOf(List<String> values) {
			return (e) -> {
				return values.stream() //
						.anyMatch(v -> v.equals(e));
			};
		}

		private static Predicate<String> containsAnyOf(List<String> values) {
			return (e) -> {
				if (e == null) {
					return false;
				}
				return !values.stream() //
						.filter(e::contains) //
						.toList() //
						.isEmpty();
			};
		}

		@Override
		public Function<String, String> build() {
			return (e) -> {
				return this.predicates.stream() //
						.filter(p -> p.predicate.test(e)) //
						.map(StringTextItem::translationKey) //
						.findFirst() //
						.orElse(this.defaultText);

			};
		}
	}
}
