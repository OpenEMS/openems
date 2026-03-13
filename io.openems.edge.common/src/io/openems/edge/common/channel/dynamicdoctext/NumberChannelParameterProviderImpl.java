package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.common.session.Language;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.type.TextProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

class NumberChannelParameterProviderImpl<V extends Number> extends ChannelParameterProvider<V>
		implements NumberChannelParameterProvider<V> {
	private final List<NumberPredicate<V>> predicates;
	private TextProvider defaultText;

	NumberChannelParameterProviderImpl(ChannelId channelId) {
		super(channelId);
		this.predicates = new ArrayList<>();
	}

	private NumberChannelParameterProviderImpl(ChannelId channelId, List<NumberPredicate<V>> predicates,
			TextProvider defaultText) {
		super(channelId);
		this.predicates = predicates;
		this.defaultText = defaultText;
	}

	/**
	 * Maps a range of numbers to a translationKey.
	 *
	 * @param text Text to display the channel when value is in range
	 * @param min  the minimum border (inclusive)
	 * @param max  the maximum border (inclusive)
	 * @return this Builder instance
	 */
	@Override
	public NumberChannelParameterProvider<V> whenIsInRange(V min, V max, TextProvider text) {
		this.addPredicate(isInRange(min, max), text);
		return this;
	}

	/**
	 * Maps the minimum range of a number to a translationKey.
	 *
	 * @param text Text to display the channel when value is in range
	 * @param min  the minimum border to show the translation (inclusive)
	 * @return this Builder instance
	 */
	@Override
	public NumberChannelParameterProvider<V> whenIsAtLeast(V min, TextProvider text) {
		this.addPredicate(isAtLeast(min), text);
		return this;
	}

	/**
	 * Maps the maximum range of a number to a translationKey.
	 *
	 * @param text text to display the channel when value is in range
	 * @param max  the maximum border to show the translation (inclusive)
	 * @return this Builder instance
	 */
	@Override
	public NumberChannelParameterProvider<V> whenIsAtMost(V max, TextProvider text) {
		this.addPredicate(isAtMost(max), text);
		return this;
	}

	/**
	 * Maps negative values to a translationKey.
	 *
	 * @param text Text to display the channel when value is in range
	 * @return this Builder instance
	 */
	@Override
	public NumberChannelParameterProvider<V> whenIsNegative(TextProvider text) {
		this.addPredicate(isNegative(), text);
		return this;
	}

	/**
	 * Maps positive values to a translationKey.
	 *
	 * @param text Text to display the channel when value is in range
	 * @return this Builder instance
	 */
	@Override
	public NumberChannelParameterProvider<V> whenIsPositive(TextProvider text) {
		this.addPredicate(isPositive(), text);
		return this;
	}

	@Override
	public NumberChannelParameterProvider<V> defaultText(TextProvider text) {
		this.defaultText = text;
		return this;
	}

	private void addPredicate(Predicate<V> predicate, TextProvider text) {
		this.predicates.add(new NumberPredicate<>(predicate, text));
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
		return NumberChannelParameterProviderImpl.<N, Integer>isAtLeast(0).negate();
	}

	private static <N extends Number> Predicate<N> isPositive() {
		return isAtLeast(0);
	}

	@Override
	public String getText(Language lang) {
		var value = this.getChannelValue();
		if (value != null) {
			var predicate = this.predicates.stream().filter(p -> p.predicate.test(value)).findFirst();
			if (predicate.isPresent()) {
				return predicate.get().text.getText(lang);
			}
		}

		if (this.defaultText != null) {
			return this.defaultText.getText(lang);
		}
		return this.getChannelValueAsString();
	}

	@Override
	public ParameterProvider clone() {
		return new NumberChannelParameterProviderImpl<>(this.channelId, this.predicates, this.defaultText);
	}

	record NumberPredicate<N extends Number>(Predicate<N> predicate, TextProvider text) {
	}
}