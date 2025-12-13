package io.openems.edge.evcs.alpitronic;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;
import io.openems.edge.evse.chargepoint.alpitronic.enums.SelectedConnector;

public class AlpitronicTest {

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link EvcsAlpitronic}.
	 *
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testAlpitronicChannels(TestCase tc) throws Exception {
		tc //
				.output(EvcsAlpitronic.ChannelId.UNIX_TIME, 1700000000L) //
				.output(EvcsAlpitronic.ChannelId.NUM_CONNECTORS, 4) //
				.output(EvcsAlpitronic.ChannelId.STATION_STATE, 0) // Available
				.output(EvcsAlpitronic.ChannelId.TOTAL_STATION_POWER, 50000) // 50 kW
				.output(EvcsAlpitronic.ChannelId.SERIAL_NUMBER, "HYC12345678") //
				.output(EvcsAlpitronic.ChannelId.CHARGEPOINT_ID, "ALPITRONIC_HYC_001") //
				.output(EvcsAlpitronic.ChannelId.VID, null) // Vehicle ID
				.output(EvcsAlpitronic.ChannelId.ID_TAG, "RFID_12345") //
				.output(EvcsAlpitronic.ChannelId.LOAD_MANAGEMENT_ENABLED, true) //
				.output(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MAJOR, 2) //
				.output(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_MINOR, 5) //
				.output(EvcsAlpitronic.ChannelId.SOFTWARE_VERSION_PATCH, 3) //
				.output(EvcsAlpitronic.ChannelId.RAW_STATUS, AvailableState.CHARGING) //
				.output(EvcsAlpitronic.ChannelId.APPLY_CHARGE_POWER_LIMIT, null) //
				.output(EvcsAlpitronic.ChannelId.CHARGING_VOLTAGE, 400.0) // 400V DC
				.output(EvcsAlpitronic.ChannelId.CHARGING_CURRENT, 125.0) // 125A DC
				.output(EvcsAlpitronic.ChannelId.RAW_CHARGE_POWER, 50000) // 50 kW
				.output(EvcsAlpitronic.ChannelId.CHARGED_TIME, 1800) // 30 minutes
				.output(EvcsAlpitronic.ChannelId.CHARGED_ENERGY, 25.0) // 25 kWh
				.output(EvcsAlpitronic.ChannelId.EV_SOC, 65) // 65%
				.output(EvcsAlpitronic.ChannelId.CONNECTOR_TYPE, SelectedConnector.CCS2) //
				.output(EvcsAlpitronic.ChannelId.EV_MAX_CHARGING_POWER, 150000) // 150 kW max
				.output(EvcsAlpitronic.ChannelId.EV_MIN_CHARGING_POWER, 5000) // 5 kW min
				.output(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MAX, 50000) //
				.output(EvcsAlpitronic.ChannelId.VAR_REACTIVE_MIN, -50000) //
				.output(EvcsAlpitronic.ChannelId.SETPOINT_REACTIVE_POWER, null) //
				.output(EvcsAlpitronic.ChannelId.RAW_CHARGE_POWER_SET, 50000) //
				.output(EvcsAlpitronic.ChannelId.TOTAL_CHARGED_ENERGY, 1500000L) // 1500 kWh total
				.output(EvcsAlpitronic.ChannelId.MAX_CHARGING_POWER_AC, 22000) // 22 kW AC backup
		;
	}
}
