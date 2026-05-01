package io.openems.edge.heat.askoma.statemachine;

import java.time.Duration;

final class AskomaConstants {
	private AskomaConstants() {
	}

	static final int OFF_ACTIVE_POWER = 0;
	static final Duration FAST_HEAT_DURATION = Duration.ofHours(10);
	static final Duration FAST_HEAT_PAUSE = Duration.ofHours(1);
}
