package io.openems.edge.io.shelly.shellypro3em;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPro3EmImplTest {

	private static final String COMPONENT_ID = "io0";

	private static final ChannelAddress PHASE_SEQUENCE_ERROR = new ChannelAddress(COMPONENT_ID, "PhaseSequenceError");
	private static final ChannelAddress POWER_METER_FAILURE = new ChannelAddress(COMPONENT_ID, "PhaseSequenceError");
	private static final ChannelAddress NO_LOAD = new ChannelAddress(COMPONENT_ID, "PhaseSequenceError");
	private static final ChannelAddress ACTIVE_POWER = new ChannelAddress(COMPONENT_ID, "ActivePower");
	private static final ChannelAddress ACTIVE_POWER_L1 = new ChannelAddress(COMPONENT_ID, "ActivePowerL1");
	private static final ChannelAddress ACTIVE_POWER_L2 = new ChannelAddress(COMPONENT_ID, "ActivePowerL2");
	private static final ChannelAddress ACTIVE_POWER_L3 = new ChannelAddress(COMPONENT_ID, "ActivePowerL3");
	private static final ChannelAddress VOLTAGE_L1 = new ChannelAddress(COMPONENT_ID, "VoltageL1");
	private static final ChannelAddress VOLTAGE_L2 = new ChannelAddress(COMPONENT_ID, "VoltageL2");
	private static final ChannelAddress VOLTAGE_L3 = new ChannelAddress(COMPONENT_ID, "VoltageL3");
	private static final ChannelAddress CURRENT_L1 = new ChannelAddress(COMPONENT_ID, "CurrentL1");
	private static final ChannelAddress CURRENT_L2 = new ChannelAddress(COMPONENT_ID, "CurrentL2");
	private static final ChannelAddress CURRENT_L3 = new ChannelAddress(COMPONENT_ID, "CurrentL3");
	private static final ChannelAddress SLAVE_COMMUNICATION_FAILED = new ChannelAddress(COMPONENT_ID,
			"SlaveCommunicationFailed");
	private static final ChannelAddress PRODUCTION_ENERGY = new ChannelAddress(COMPONENT_ID, "ActiveProductionEnergy");
	private static final ChannelAddress CONSUMPTION_ENERGY = new ChannelAddress(COMPONENT_ID,
			"ActiveConsumptionEnergy");

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();

		final var sut = new IoShellyPro3EmImpl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setType(MeterType.GRID) //
						.build()) //

				.next(new TestCase("Successful read response").onBeforeControllersCallbacks(() -> {
					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "id": 0,
									  "a_current": 0.593,
									  "a_voltage": 230.5,
									  "a_act_power": -75.4,
									  "a_aprt_power": 136.9,
									  "a_pf": 0.68,
									  "a_freq": 50,
									  "b_current": 11.608,
									  "b_voltage": 228.5,
									  "b_act_power": 2655.2,
									  "b_aprt_power": 2656.6,
									  "b_pf": 1,
									  "b_freq": 50,
									  "c_current": 0.058,
									  "c_voltage": 232.1,
									  "c_act_power": 2.1,
									  "c_aprt_power": 13.5,
									  "c_pf": 0.54,
									  "c_freq": 50,
									  "n_current": null,
									  "total_current": 12.259,
									  "total_act_power": 2581.781,
									  "total_aprt_power": 2806.935,
									  "user_calibrated_phase": []
									}
							"""));
					httpTestBundle.triggerNextCycle();
				}) //
						.output(ACTIVE_POWER, 2582) //
						.output(ACTIVE_POWER_L1, -75) //
						.output(ACTIVE_POWER_L2, 2655) //
						.output(ACTIVE_POWER_L3, 2) //
						.output(VOLTAGE_L1, 230500) //
						.output(VOLTAGE_L2, 228500) //
						.output(VOLTAGE_L3, 232100) //
						.output(CURRENT_L1, 593) //
						.output(CURRENT_L2, 11608) //
						.output(CURRENT_L3, 58) //
						.output(SLAVE_COMMUNICATION_FAILED, false)) //

				.next(new TestCase("Invalid read response")
						.onBeforeControllersCallbacks(() -> assertEquals("L:2582 W", sut.debugLog()))

						.onBeforeControllersCallbacks(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.output(PHASE_SEQUENCE_ERROR, false) //
						.output(POWER_METER_FAILURE, false) //
						.output(NO_LOAD, false) //
						.output(ACTIVE_POWER, null) //
						.output(ACTIVE_POWER_L1, null) //
						.output(ACTIVE_POWER_L2, null) //
						.output(ACTIVE_POWER_L3, null) //
						.output(VOLTAGE_L1, null) //
						.output(VOLTAGE_L2, null) //
						.output(VOLTAGE_L3, null) //
						.output(CURRENT_L1, null) //
						.output(CURRENT_L2, null) //
						.output(CURRENT_L3, null) //
						.output(PRODUCTION_ENERGY, 0L) //
						.output(CONSUMPTION_ENERGY, 0L) //
						.output(SLAVE_COMMUNICATION_FAILED, true)) //

				.deactivate();
	}
}
