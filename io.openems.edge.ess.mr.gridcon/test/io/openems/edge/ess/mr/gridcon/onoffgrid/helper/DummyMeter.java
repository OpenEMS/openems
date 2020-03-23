package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

public class DummyMeter extends AbstractOpenemsComponent implements SymmetricMeter {

	public static final Object DEFAULT_VOLTAGE = 230_000;
	public static final Object DEFAULT_FREQUENCY = 50_000;

	public DummyMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values() //
		);
		this.getFrequency().setNextValue(DEFAULT_FREQUENCY);
		this.getVoltage().setNextValue(DEFAULT_VOLTAGE);
		this.getFrequency().nextProcessImage();
		this.getVoltage().nextProcessImage();
	}

	@Override
	public MeterType getMeterType() {
		// TODO Auto-generated method stub
		return null;
	}

}
