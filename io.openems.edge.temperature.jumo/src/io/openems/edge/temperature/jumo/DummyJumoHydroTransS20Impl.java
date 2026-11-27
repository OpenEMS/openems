package io.openems.edge.temperature.jumo;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

public class DummyJumoHydroTransS20Impl extends AbstractDummyOpenemsComponent<DummyJumoHydroTransS20Impl>
		implements JumoHydroTransS20, OpenemsComponent, ModbusComponent {

	public DummyJumoHydroTransS20Impl(String id) {
		super(//
				id, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				JumoHydroTransS20.ChannelId.values() //
		);
		for (var channel : this.channels()) {
			channel.nextProcessImage();
		}
	}

	/**
	 * Sets and applies the {@link JumoHydroTransS20.ChannelId#TEMPERATURE}.
	 *
	 * @param value the Temperature in [C]
	 * @return myself
	 */
	public DummyJumoHydroTransS20Impl withTemperature(int value) {
		this.getTemperatureChannel().setNextValue(value);
		this.getTemperatureChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link JumoHydroTransS20.ChannelId#RELATIVE_HUMIDITY}.
	 *
	 * @param value the Humidity in [GRAMS_PER_CUBIC_METER]
	 * @return myself
	 */
	public DummyJumoHydroTransS20Impl withHumidity(int value) {
		this.getRelativeHumidityChannel().setNextValue(value);
		this.getRelativeHumidityChannel().nextProcessImage();
		return this;
	}

	@Override
	public void retryModbusCommunication() {

	}

	@Override
	protected DummyJumoHydroTransS20Impl self() {
		return this;
	}

}
