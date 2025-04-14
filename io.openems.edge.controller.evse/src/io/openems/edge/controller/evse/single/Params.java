package io.openems.edge.controller.evse.single;

import com.google.common.collect.ImmutableList;

import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile;

public record Params(boolean isReadyForCharging, Mode.Actual actualMode,
		/** The ActivePower value; possibly null */
		Integer activePower, //
		Limit limit, ImmutableList<Profile> profiles) {
}
