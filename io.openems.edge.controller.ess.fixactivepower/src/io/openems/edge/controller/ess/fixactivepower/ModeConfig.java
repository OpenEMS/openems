package io.openems.edge.controller.ess.fixactivepower;

import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Relationship;

public record ModeConfig(Mode mode, Phase phase, HybridEssMode hybridEssMode, Relationship relationship,
		Integer value) {

	protected static ModeConfig from(Config config) {
		return switch (config.mode()) {
		case MANUAL_ON ->
			// Apply Active-Power Set-Point
			new ModeConfig(Mode.MANUAL_ON, Phase.ALL, config.hybridEssMode(), Relationship.EQUALS, config.power());

		case MANUAL_OFF ->
			// Do nothing
			new ModeConfig(Mode.MANUAL_OFF, Phase.ALL, config.hybridEssMode(), Relationship.EQUALS, null);

		case SMART ->
			// SMART-Mode: use config from Schedule
			null;
		};
	}
}