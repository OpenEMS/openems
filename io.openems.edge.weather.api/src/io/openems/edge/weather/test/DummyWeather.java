package io.openems.edge.weather.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.weather.api.Weather;

public class DummyWeather extends AbstractDummyWeather<DummyWeather> implements Weather {

	public DummyWeather(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				Weather.ChannelId.values());
	}

	@Override
	protected DummyWeather self() {
		return this;
	}
}
