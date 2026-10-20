package io.openems.edge.fluidcooling.hydac;

import static io.openems.edge.common.startstop.StartStop.START;
import static io.openems.edge.common.startstop.StartStop.STOP;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;

public class DummyHydacImpl extends AbstractDummyOpenemsComponent<DummyHydacImpl>
		implements Hydac, OpenemsComponent, ModbusComponent, StartStoppable {

	private StartStop startStopTarget;
	private StartStopConfig startStop;

	public DummyHydacImpl(String id) {
		super(//
				id, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Hydac.ChannelId.values() //
		);
		for (var channel : this.channels()) {
			channel.nextProcessImage();
		}
	}

	/**
	 * Sets and applies the {@link Hydac.ChannelId#OUTLET_TEMPERATURE}.
	 *
	 * @param value the temperature value
	 * @return myself
	 */
	public DummyHydacImpl withOutletTemperature(int value) {
		this.getOutletTemperatureChannel().setNextValue(value);
		this.getOutletTemperatureChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Hydac.ChannelId#COOLING_REQUIRED}.
	 *
	 * @param value the boolean value
	 * @return myself
	 */
	public DummyHydacImpl withCoolingRequired(boolean value) {
		this.getCoolingRequiredChannel().setNextValue(value);
		this.getCoolingRequiredChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Hydac.ChannelId#PUMP}.
	 *
	 * @param value the boolean value
	 * @return myself
	 */
	public DummyHydacImpl withPump(boolean value) {
		this.getPumpChannel().setNextValue(value);
		this.getPumpChannel().nextProcessImage();
		return this;
	}

	/**
	 * Sets and applies the {@link Hydac.ChannelId#PUMP}.
	 *
	 * @param value the boolean value
	 * @return myself
	 */
	public DummyHydacImpl withStartStopConfig(StartStopConfig value) {
		this.startStop = value;
		return this;
	}

	/**
	 * Sets and applies the {@link Hydac.ChannelId#COMPRESSOR}.
	 *
	 * @param value the boolean value
	 * @return myself
	 */
	public DummyHydacImpl withCompressor(boolean value) {
		this.getCompressorChannel().setNextValue(value);
		this.getCompressorChannel().nextProcessImage();
		return this;
	}

	@Override
	public void retryModbusCommunication() {

	}

	@Override
	protected DummyHydacImpl self() {
		return this;
	}

	@Override
	public StartStop getStartStopTarget() {
		return switch (this.startStop) {
		case AUTO -> this.startStopTarget;
		case START -> START;
		case STOP -> STOP;
		};
	}

	@Override
	public void setStartStop(StartStop value) {
		this.startStopTarget = value;
	}
}
