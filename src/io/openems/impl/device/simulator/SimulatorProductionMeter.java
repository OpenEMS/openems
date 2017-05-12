package io.openems.impl.device.simulator;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.FunctionalReadChannel;
import io.openems.api.channel.FunctionalReadChannelFunction;
import io.openems.api.channel.ReadChannel;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.ConfigException;
import io.openems.core.utilities.ControllerUtils;

public class SimulatorProductionMeter extends SimulatorMeter {

	@ConfigInfo(title = "ActivePower", type = Long.class)
	public ConfigChannel<Long> activePower = new ConfigChannel("ActivePower", this);
	private FunctionalReadChannel<Long> apparentPower;
	@ConfigInfo(title = "ReactivePower", type = Long.class)
	public ConfigChannel<Long> reactivePower = new ConfigChannel<Long>("ReactivePower", this);

	public SimulatorProductionMeter(String thingId) throws ConfigException {
		super(thingId);
		this.apparentPower = new FunctionalReadChannel<Long>("ApparentPower", this,
				new FunctionalReadChannelFunction<Long>() {

					@Override
					public Long handle(ReadChannel<Long>... channels) {
						return ControllerUtils.calculateApparentPower(channels[0].valueOptional().orElse(0L),
								channels[1].valueOptional().orElse(0L));
					}

				}, activePower, reactivePower);
	}

	@Override
	public ReadChannel<Long> activePower() {
		return activePower;
	}

	@Override
	public ReadChannel<Long> apparentPower() {
		return apparentPower;
	}

	@Override
	public ReadChannel<Long> reactivePower() {
		return reactivePower;
	}

}
