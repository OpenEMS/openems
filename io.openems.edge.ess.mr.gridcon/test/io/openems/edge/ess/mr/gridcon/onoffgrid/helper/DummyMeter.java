package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

public class DummyMeter extends AbstractOpenemsComponent implements SymmetricMeter {

	public static final int DEFAULT_VOLTAGE = 230_000;
	public static final int DEFAULT_FREQUENCY = 50_000;

	public DummyMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values() //
		);
		this._setFrequency(DEFAULT_FREQUENCY);
		this._setVoltage(DEFAULT_VOLTAGE);
	}

	@Override
	public MeterType getMeterType() {
		// TODO Auto-generated method stub
		return null;
	}

}
