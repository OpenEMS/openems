package io.openems.edge.core.appmanager.formly.builder.selectgroup;

import java.util.Objects;
import java.util.stream.Stream;

import io.openems.edge.core.appmanager.OnlyIf;
import io.openems.edge.core.appmanager.Self;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;

public class OptionBuilder implements Self<OptionBuilder>, OnlyIf<OptionBuilder> {

	private final String value;
	private String title;
	private Boolean hide;

	private BooleanExpression hideExpression;
	private BooleanExpression disabledExpression;
	private StringExpression titleExpression;

	public OptionBuilder(String value) {
		super();
		this.value = Objects.requireNonNull(value);
	}

	public OptionBuilder setTitle(String title) {
		this.title = title;
		return this;
	}

	public OptionBuilder setHide(Boolean hide) {
		this.hide = hide;
		return this;
	}

	public OptionBuilder setHideExpression(BooleanExpression hideExpression) {
		this.hideExpression = hideExpression;
		return this;
	}

	public OptionBuilder setDisabledExpression(BooleanExpression disabledExpression) {
		this.disabledExpression = disabledExpression;
		return this;
	}

	public OptionBuilder setTitleExpression(StringExpression titleExpression) {
		this.titleExpression = titleExpression;
		return this;
	}

	public Option build() {
		OptionExpressions optionExpressions = null;
		if (Stream.of(this.hideExpression, this.titleExpression, this.disabledExpression) //
				.anyMatch(Objects::nonNull)) {
			optionExpressions = new OptionExpressions(this.hideExpression, this.disabledExpression,
					this.titleExpression);
		}
		return new Option(this.value, this.title, this.hide, optionExpressions);
	}

	@Override
	public OptionBuilder self() {
		return this;
	}

}
