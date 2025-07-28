package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evcs.api.ChargeMode;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;

public class CommonNaturesTest {

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link ElectricityMeter}.
	 * 
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testElectricityMeterChannels(TestCase tc) throws Exception {
		tc //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 5678) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 2138) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1648) //
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 1892) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER, 0) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 0) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, 0) //
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, 0) //
				.output(ElectricityMeter.ChannelId.VOLTAGE, 230_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 231_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 229_000) //
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 230_000) //
				.output(ElectricityMeter.ChannelId.CURRENT, 24_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 9_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 7_000) //
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 8_000) //
				.output(ElectricityMeter.ChannelId.FREQUENCY, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 774784L) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
				// .output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
				// overwritten by DeprecatedEvcs
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
		;
	}

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link ManagedEvcs}.
	 * 
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testManagedEvcsChannels(TestCase tc) throws Exception {
		tc //
				.output(ManagedEvcs.ChannelId.POWER_PRECISION, 0.23) //
				.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT, null) //
				.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT_WITH_FILTER, null) //
				.output(ManagedEvcs.ChannelId.IS_CLUSTERED, null) // set by cluster
				.output(ManagedEvcs.ChannelId.CHARGE_MODE, ChargeMode.FORCE_CHARGE) //
				.output(ManagedEvcs.ChannelId.SET_DISPLAY_TEXT, null) //
				.output(ManagedEvcs.ChannelId.SET_CHARGE_POWER_REQUEST, null) //
				.output(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT, null) //
				.output(ManagedEvcs.ChannelId.CHARGE_STATE, ChargeState.CHARGING) //
		;
	}

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link Evcs}.
	 * 
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testEvcsChannels(TestCase tc) throws Exception {
		tc //
				.output(Evcs.ChannelId.STATUS, Status.CHARGING) //
				.output(Evcs.ChannelId.CHARGING_TYPE, ChargingType.AC) //
				.output(Evcs.ChannelId.PHASES, Phases.THREE_PHASE) //
				.output(Evcs.ChannelId.FIXED_MINIMUM_HARDWARE_POWER, 4140) //
				.output(Evcs.ChannelId.FIXED_MAXIMUM_HARDWARE_POWER, 22080) //
				.output(Evcs.ChannelId.MINIMUM_HARDWARE_POWER, 4140) //
				.output(Evcs.ChannelId.MAXIMUM_POWER, null) //
				.output(Evcs.ChannelId.MINIMUM_POWER, null) //
				.output(Evcs.ChannelId.ENERGY_SESSION, 6530) //
				.output(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED, false) //
		;
	}

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link DeprecatedEvcs}.
	 * 
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testDeprecatedEvcsChannels(TestCase tc) throws Exception {
		tc //
				.output(DeprecatedEvcs.ChannelId.CHARGE_POWER, 5678) //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 774784L) //
		;
	}
}
