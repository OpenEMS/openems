package io.openems.edge.common.channel.dynamicdoctext;

import java.util.function.Function;

import io.openems.common.session.Language;
import io.openems.edge.common.component.OpenemsComponent;

class ComponentDataParameterProvider<T extends OpenemsComponent> implements ParameterProvider {
	private final Class<T> clazz;
	private final Function<T, String> function;

	private OpenemsComponent component;

	ComponentDataParameterProvider(Class<T> clazz, Function<T, String> function) {
		this.clazz = clazz;
		this.function = function;
	}

	@Override
	public void init(OpenemsComponent component) {
		this.component = component;
		if (!this.clazz.isInstance(this.component)) {
			throw new RuntimeException(
					"Component %s is not a instance of class %s".formatted(component.id(), this.clazz.getName()));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getText(Language lang) {
		return this.function.apply((T) this.component);
	}

	@Override
	public ParameterProvider copy() {
		return new ComponentDataParameterProvider<>(this.clazz, this.function);
	}
}
