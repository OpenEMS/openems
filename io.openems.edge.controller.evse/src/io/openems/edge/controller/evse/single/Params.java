package io.openems.edge.controller.evse.single;

import com.google.common.collect.ImmutableList;

import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile;

public record Params(//
		/** EV is ready for charging anytime. */
		boolean isReadyForCharging, //
		Mode.Actual actualMode, //
		/** The ActivePower value; possibly null */
		Integer activePower, //
		Limit limit, //
		Hysteresis hysteresis, //
		/** EV appears to be fully charged. */
		boolean appearsToBeFullyCharged, //
		ImmutableList<Profile> profiles) {
}
