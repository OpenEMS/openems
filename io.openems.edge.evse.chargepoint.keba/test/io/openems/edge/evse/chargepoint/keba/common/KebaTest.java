package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.common.channel.Level;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchState;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetEnable;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetUnlock;

public class KebaTest {

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link Keba}.
	 * 
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testKebaChannels(TestCase tc) throws Exception {
		tc //
				.output(OpenemsComponent.ChannelId.STATE, Level.OK) //

				.output(Keba.ChannelId.CABLE_STATE, CableState.PLUGGED_AND_LOCKED) //
				.output(Keba.ChannelId.CHARGING_STATE, ChargingState.CHARGING) //
				.output(Keba.ChannelId.POWER_FACTOR, 905) //
				.output(Keba.ChannelId.PHASE_SWITCH_SOURCE, PhaseSwitchSource.NONE) //
				.output(Keba.ChannelId.PHASE_SWITCH_STATE, PhaseSwitchState.SINGLE) //
				.output(Keba.ChannelId.DEBUG_SET_ENABLE, SetEnable.UNDEFINED) //
				.output(Keba.ChannelId.SET_ENABLE, SetEnable.UNDEFINED) //
				.output(Keba.ChannelId.DEBUG_SET_CHARGING_CURRENT, null) //
				.output(Keba.ChannelId.SET_CHARGING_CURRENT, null) //
				.output(Keba.ChannelId.SET_UNLOCK_PLUG, SetUnlock.UNDEFINED) //
				.output(Keba.ChannelId.SET_PHASE_SWITCH_SOURCE, PhaseSwitchSource.UNDEFINED) //
				.output(Keba.ChannelId.SET_PHASE_SWITCH_STATE, PhaseSwitchState.UNDEFINED) //
		;
	}
}
