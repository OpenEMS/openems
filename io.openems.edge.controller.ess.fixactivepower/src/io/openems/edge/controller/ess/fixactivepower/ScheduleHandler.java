package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.controller.ess.fixactivepower.HybridEssMode.TARGET_DC;
import static io.openems.edge.controller.ess.fixactivepower.Mode.MANUAL_OFF;
import static io.openems.edge.controller.ess.fixactivepower.Mode.MANUAL_ON;
import static io.openems.edge.ess.power.api.Phase.ALL;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;

import io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.DynamicConfig;
import io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset;
import io.openems.edge.energy.api.schedulable.Schedule;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;

public class ScheduleHandler extends Schedule.Handler<Config, Preset, DynamicConfig> {

	public static record DynamicConfig(Mode mode, HybridEssMode hybridEssMode, Relationship relationship, Phase phase,
			int power) {
	}

	public static enum Preset implements Schedule.Preset {
		OFF, //
		FORCE_ZERO, //
		FORCE_CHARGE_2500_W, //
		FORCE_CHARGE_5000_W, //
		// TODO PERCENTAGE would be more flexible
		// FORCE_CHARGE_25(new ModeConfig(MANUAL_ON, TARGET_DC, -25, Unit.PERCENTAGE)),
		// FORCE_CHARGE_50(new ModeConfig(MANUAL_ON, TARGET_DC, -50, Unit.PERCENTAGE)),
		// FORCE_CHARGE_75(new ModeConfig(MANUAL_ON, TARGET_DC, -75, Unit.PERCENTAGE)),
		// FORCE_CHARGE_100(new ModeConfig(MANUAL_ON, TARGET_DC, -100,
		// Unit.PERCENTAGE)); //
		;
	}

	protected ScheduleHandler() {
		super(Preset.values());
	}

	@Override
	protected DynamicConfig toConfig(Config config) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected DynamicConfig toConfig(Config config, Preset preset) {
		return switch (config.mode()) {
		case MANUAL_ON, MANUAL_OFF -> new DynamicConfig(config.mode().toMode(), config.hybridEssMode(),
				config.relationship(), config.phase(), config.power());

		case SMART -> //
			preset == null ? null : switch (preset) {
			case OFF -> new DynamicConfig(MANUAL_OFF, null, null, null, 0);
			case FORCE_ZERO -> new DynamicConfig(MANUAL_ON, TARGET_DC, EQUALS, ALL, 0);
			case FORCE_CHARGE_2500_W -> new DynamicConfig(MANUAL_ON, TARGET_DC, EQUALS, ALL, -2500);
			case FORCE_CHARGE_5000_W -> new DynamicConfig(MANUAL_ON, TARGET_DC, EQUALS, ALL, -5000);
			};
		};
	}

}
