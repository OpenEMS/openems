package io.openems.edge.controller.asymmetric.peakshaving;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	private final String essId;
	private final String meterId;
	private final int peakShavingPower;
	private final int rechargePower;

	public MyConfig(String id, String essId, String meterId, int peakShavingPower, int rechargePower) {
		super(Config.class, id);
		this.essId = essId;
		this.meterId = meterId;
		this.peakShavingPower = peakShavingPower;
		this.rechargePower = rechargePower;
	}

	@Override
	public String ess_id() {
		return this.essId;
	}

	@Override
	public String meter_id() {
		return this.meterId;
	}

	@Override
	public int peakShavingPower() {
		return this.peakShavingPower;
	}

	@Override
	public int rechargePower() {
		return this.rechargePower;
	}
}
