package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.controller.ess.fixactivepower.HybridEssMode.TARGET_DC;
import static io.openems.edge.controller.ess.fixactivepower.Mode.MANUAL_ON;
import static io.openems.edge.ess.power.api.Phase.ALL;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;

import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;

public class Schedule {

	public record Config(Mode mode, HybridEssMode hybridEssMode, Relationship relationship, Phase phase,
			Integer power) {

		protected static Config from(io.openems.edge.controller.ess.fixactivepower.Config config) {
			return switch (config.mode()) {
			case MANUAL_ON ->
				// Apply Active-Power Set-Point
				new Config(MANUAL_ON, TARGET_DC, EQUALS, ALL, config.power());

			case MANUAL_OFF ->
				// Do nothing
				new Config(Mode.MANUAL_OFF, null, null, null, null);

			case SMART ->
				// SMART-Mode: use config from Schedule
				null;
			};
		}
	}

	public static enum Preset implements io.openems.edge.energy.api.schedulable.Schedule.Preset<Config> {
		OFF(new Config(Mode.MANUAL_OFF, null, null, null, null)), //
		FORCE_ZERO(new Config(MANUAL_ON, TARGET_DC, EQUALS, ALL, 0)), //
		FORCE_CHARGE_2500_W(new Config(MANUAL_ON, TARGET_DC, EQUALS, ALL, -2500)), //
		FORCE_CHARGE_5000_W(new Config(MANUAL_ON, TARGET_DC, EQUALS, ALL, -5000)), //
		// TODO PERCENTAGE would be more flexible
		// FORCE_CHARGE_25(new ModeConfig(MANUAL_ON, TARGET_DC, -25, Unit.PERCENTAGE)),
		// FORCE_CHARGE_50(new ModeConfig(MANUAL_ON, TARGET_DC, -50, Unit.PERCENTAGE)),
		// FORCE_CHARGE_75(new ModeConfig(MANUAL_ON, TARGET_DC, -75, Unit.PERCENTAGE)),
		// FORCE_CHARGE_100(new ModeConfig(MANUAL_ON, TARGET_DC, -100,
		// Unit.PERCENTAGE)); //
		;

		private final Config config;

		private Preset(Config config) {
			this.config = config;
		}

		@Override
		public Config getConfig() {
			return this.config;
		}
	}
}
