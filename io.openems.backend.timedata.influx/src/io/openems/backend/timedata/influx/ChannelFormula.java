package io.openems.backend.timedata.influx;

import java.util.Optional;

import com.google.gson.JsonPrimitive;

import io.openems.backend.common.timedata.EdgeCache;
import io.openems.common.types.ChannelAddress;

public class ChannelFormula {

	private final Function function;

	private final Optional<ChannelAddress> address;
	private final Optional<Integer> staticValue;

	public ChannelFormula(Function function, ChannelAddress address) {
		this.function = function;
		this.address = Optional.of(address);
		this.staticValue = Optional.empty();
	}

	public ChannelFormula(Function function, int staticValue) {
		this.function = function;
		this.address = Optional.empty();
		this.staticValue = Optional.of(staticValue);
	}

	/**
	 * Gets the Channel value.
	 *
	 * @param cache an {@link EdgeCache}
	 * @return the value
	 */
	public int getValue(EdgeCache cache) {
		if (this.address.isPresent()) {
			return cache.getChannelValue(this.address.get()).orElse(new JsonPrimitive(0)).getAsInt();
		}
		if (this.staticValue.isPresent()) {
			return this.staticValue.get();
		} else {
			return 0;
		}
	}

	public Function getFunction() {
		return this.function;
	}

}
