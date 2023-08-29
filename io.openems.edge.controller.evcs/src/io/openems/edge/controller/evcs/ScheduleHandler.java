package io.openems.edge.controller.evcs;

import io.openems.edge.controller.evcs.ScheduleHandler.DynamicConfig;
import io.openems.edge.controller.evcs.ScheduleHandler.Preset;
import io.openems.edge.energy.api.schedulable.Schedule;
import io.openems.edge.evcs.api.ChargeMode;

public class ScheduleHandler extends Schedule.Handler<Config, Preset, DynamicConfig> {

	public record DynamicConfig(boolean enabledCharging, ChargeMode chargeMode, int forceChargeMinPower,
			int defaultChargeMinPower, Priority priority, int energySessionLimit) {
	}

	public static enum Preset implements Schedule.Preset {
		OFF, //
		EXCESS_POWER, //
		FORCE_CHARGE;
	}

	protected ScheduleHandler() {
		super(Preset.values());
	}

	@Override
	protected DynamicConfig toConfig(Config config) {
		return new DynamicConfig(config.enabledCharging(), config.chargeMode().toChargeMode(),
				config.forceChargeMinPower(), config.defaultChargeMinPower(), config.priority(),
				config.energySessionLimit());
	}

	@Override
	protected DynamicConfig toConfig(Config config, Preset preset) {
		return switch (config.chargeMode()) {
		case EXCESS_POWER, FORCE_CHARGE -> this.toConfig(config);

		case SMART -> //
			switch (preset) {
			case OFF -> new DynamicConfig(false, ChargeMode.EXCESS_POWER, 0, 0, Priority.CAR, 0);
			case EXCESS_POWER ->
				new DynamicConfig(false, ChargeMode.EXCESS_POWER, 0, config.defaultChargeMinPower(), Priority.CAR, 0);
			case FORCE_CHARGE -> new DynamicConfig(false, ChargeMode.EXCESS_POWER, config.forceChargeMinPower(),
					config.defaultChargeMinPower(), Priority.STORAGE, 0);
			};
		};
	}

}
