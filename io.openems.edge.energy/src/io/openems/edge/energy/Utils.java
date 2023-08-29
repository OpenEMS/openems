package io.openems.edge.energy;

public class Utils {

	private Utils() {
	}

	public static io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset[] createEssFixActivePowerScheduleFromConfig(
			Config config) {
		return new io.openems.edge.controller.ess.fixactivepower.ScheduleHandler.Preset[] { config.essFix00(),
				config.essFix01(), config.essFix02(), config.essFix03(), config.essFix04(), config.essFix05(),
				config.essFix06(), config.essFix07(), config.essFix08(), config.essFix09(), config.essFix10(),
				config.essFix11(), config.essFix12(), config.essFix13(), config.essFix14(), config.essFix15(),
				config.essFix16(), config.essFix17(), config.essFix18(), config.essFix19(), config.essFix20(),
				config.essFix21(), config.essFix22(), config.essFix23() };
	}

	public static io.openems.edge.controller.evcs.ScheduleHandler.Preset[] createEvcsScheduleFromConfig(Config config) {
		return new io.openems.edge.controller.evcs.ScheduleHandler.Preset[] { config.evcs00(), config.evcs01(),
				config.evcs02(), config.evcs03(), config.evcs04(), config.evcs05(), config.evcs06(), config.evcs07(),
				config.evcs08(), config.evcs09(), config.evcs10(), config.evcs11(), config.evcs12(), config.evcs13(),
				config.evcs14(), config.evcs15(), config.evcs16(), config.evcs17(), config.evcs18(), config.evcs19(),
				config.evcs20(), config.evcs21(), config.evcs22(), config.evcs23() };
	}
}
