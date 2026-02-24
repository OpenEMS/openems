package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.common.session.Language;
import io.openems.edge.common.component.OpenemsComponent;

class StaticParameterProvider implements ParameterProvider {
	private final String value;

	StaticParameterProvider(String value) {
		this.value = value;
	}

	@Override
	public void init(OpenemsComponent component) {
	}

	@Override
	public String getText(Language lang) {
		return this.value;
	}

	@Override
	public ParameterProvider clone() {
		return new StaticParameterProvider(this.value);
	}
}
