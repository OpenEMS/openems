package io.openems.edge.io.shelly.shellypro3em;

import static io.openems.common.types.MeterType.GRID;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPro3EmImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShellyPro3EmImpl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.setType(GRID) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
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
						.onAfterProcessImage(() -> assertEquals("L:2582 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 2582) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -75) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 2655) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 2) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230367) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 228500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 232100) //
						.output(ElectricityMeter.ChannelId.CURRENT, 12259) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 593) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 11608) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 58) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(IoShellyPro3Em.ChannelId.NO_LOAD, false) //
						.output(IoShellyPro3Em.ChannelId.PHASE_SEQUENCE_ERROR, false) //
						.output(IoShellyPro3Em.ChannelId.POWER_METER_FAILURE, false) //
						.output(IoShellyPro3Em.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("L:UNDEFINED", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 0L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 0L) //
						.output(IoShellyPro3Em.ChannelId.NO_LOAD, false) //
						.output(IoShellyPro3Em.ChannelId.PHASE_SEQUENCE_ERROR, false) //
						.output(IoShellyPro3Em.ChannelId.POWER_METER_FAILURE, false) //
						.output(IoShellyPro3Em.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

				.deactivate();
	}
}
