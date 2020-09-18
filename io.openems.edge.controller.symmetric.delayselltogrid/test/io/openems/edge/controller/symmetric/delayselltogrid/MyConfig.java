package io.openems.edge.controller.symmetric.delayselltogrid;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {
	private final String ess_id;
	private final String meter_id;
	private final int delaySellToGridPower;

	public MyConfig(String id, String ess_id, String meter_id, int delaySellToGridPower) {
		super(Config.class, id);
		this.ess_id = ess_id;
		this.meter_id = meter_id;
		this.delaySellToGridPower = delaySellToGridPower;
	}

	@Override
	public String ess_id() {
		return this.ess_id;
	}

	@Override
	public String meter_id() {
		return this.meter_id;
	}

	@Override
	public int delaySellToGridPower() {
		return this.delaySellToGridPower;
	}

}