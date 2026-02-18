package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.common.session.Language;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.type.TextProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class StringChannelParameterProviderImpl extends ChannelParameterProvider<String>
		implements StringChannelParameterProvider {
	private final List<StringPredicate> predicates;
	private TextProvider defaultText;

	StringChannelParameterProviderImpl(ChannelId channelId) {
		super(channelId);
		this.predicates = new ArrayList<>();
	}

	private StringChannelParameterProviderImpl(ChannelId channelId, List<StringPredicate> predicates,
			TextProvider defaultText) {
		super(channelId);
		this.predicates = predicates;
		this.defaultText = defaultText;
	}

	@Override
	public StringChannelParameterProvider when(String value, TextProvider text) {
		this.addPredicate(x -> x.equals(value), text);
		return this;
	}

	@Override
	public StringChannelParameterProvider when(String[] values, TextProvider text) {
		var set = Arrays.stream(values).collect(Collectors.toSet());
		this.addPredicate(set::contains, text);
		return this;
	}

	@Override
	public StringChannelParameterProvider whenStringContains(String value, TextProvider text) {
		this.addPredicate(x -> x.contains(value), text);
		return this;
	}

	@Override
	public StringChannelParameterProvider defaultText(TextProvider text) {
		this.defaultText = text;
		return this;
	}

	private void addPredicate(Predicate<String> predicate, TextProvider text) {
		this.predicates.add(new StringPredicate(predicate, text));
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
		return new StringChannelParameterProviderImpl(this.channelId, this.predicates, this.defaultText);
	}

	record StringPredicate(Predicate<String> predicate, TextProvider text) {
	}
}