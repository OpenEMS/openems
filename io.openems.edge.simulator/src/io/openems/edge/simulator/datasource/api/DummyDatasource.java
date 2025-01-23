package io.openems.edge.simulator.datasource.api;

import java.util.List;
import java.util.Set;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;

public class DummyDatasource implements SimulatorDatasource {

	public final Object value;

	public DummyDatasource(Object value) {
		this.value = value;
	}

	@Override
	public Set<String> getKeys() {
		return Set.of();
	}

	@Override
	public int getTimeDelta() {
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getValues(OpenemsType type, ChannelAddress channelAddress) {
		return (List<T>) List.of(this.value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getValue(OpenemsType type, ChannelAddress channelAddress) {
		return (T) this.value;
	}
}
