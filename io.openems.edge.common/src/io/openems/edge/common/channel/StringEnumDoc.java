package io.openems.edge.common.channel;

import io.openems.edge.common.channel.Doc.OptionsDoc;

public final class StringEnumDoc extends StringDoc implements OptionsDoc<String> {

	private final String[] options;

	public StringEnumDoc(String[] options) {
		super();
		this.options = options;
	}

	@Override
	public String[] getOptions() {
		return this.options;
	}
}
