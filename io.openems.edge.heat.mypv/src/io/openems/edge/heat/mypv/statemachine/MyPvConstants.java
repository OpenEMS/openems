package io.openems.edge.heat.mypv.statemachine;

import java.time.Duration;

final class MyPvConstants {
	private MyPvConstants() {
	}

	static final int OFF_ACTIVE_POWER = 0;
	static final Duration FAST_HEAT_DURATION = Duration.ofHours(10);
	static final Duration FAST_HEAT_PROTECTION_PAUSE_DURATION = Duration.ofHours(1);
	static final Duration SURPLUS_UPDATE_INTERVAL = Duration.ofSeconds(10);
}
