package io.openems.edge.system.fenecon.masterbox2v0.utils;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.util.function.Supplier;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.value.Value;

public class IocReadValueMapping<T> {
	private final Supplier<Value<T>> source;
	private final ChannelId target;

	public IocReadValueMapping(Supplier<Value<T>> source, ChannelId target) {
		this.source = source;
		this.target = target;
	}

	/**
	 * Applies the read value mappings for the given component.
	 * 
	 * @param component the {@link MasterBoxModbusComponent}
	 * @throws IllegalArgumentException on error
	 */
	protected void apply(MasterBoxModbusComponent component) throws IllegalArgumentException {
		Value<T> value = this.source.get();
		if (value.isDefined()) {
			setValue(component, this.target, value.get());
		}
	}
}
