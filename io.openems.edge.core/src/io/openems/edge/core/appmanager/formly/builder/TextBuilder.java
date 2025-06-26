package io.openems.edge.core.appmanager.formly.builder;

public final class TextBuilder extends FormlyBuilder<TextBuilder> {

	public TextBuilder() {
		// null because no key is needed if no input happens
		super(null);
	}

	public TextBuilder setText(String text) {
		return this.setDescription(text);
	}

	@Override
	protected String getType() {
		return "text";
	}

}