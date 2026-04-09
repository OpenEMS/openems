package io.openems.core.referencetarget;

import java.util.Dictionary;

public class ValueProviderFromConfig implements ValueProvider {
	private final Dictionary<String, Object> properties;

	public ValueProviderFromConfig(Dictionary<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public Object getValue(String variable) {
		return this.properties.get(variable.replaceAll("_", "."));
	}

}
