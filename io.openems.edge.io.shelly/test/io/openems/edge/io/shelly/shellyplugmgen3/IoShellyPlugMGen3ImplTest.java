
package io.openems.edge.io.shelly.shellyplugmgen3;

import static io.openems.common.types.MeterType.PRODUCTION;
import static io.openems.edge.common.type.Phase.SinglePhase.L1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.io.shelly.common.component.ShellySwitch;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPlugMGen3ImplTest {

	@Test
	public void test() throws Exception {
		final var httpTestBundle = DummyBridgeHttpBundle.of();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();
		final var sut = new IoShellyPlugMGen3Impl();

		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
				.addReference("httpBridgeShellyServiceDefinition",
						new HttpBridgeShellyService.HttpBridgeShellyServiceDefinition()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setPhase(L1) //
						.setIp("127.0.0.1") //
						.setType(PRODUCTION) //
						.setInvert(false) //
						.build()) //

				// Successful read response
				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {}
									  },
									  "switch:0": {
									    "current": 1.234,
									    "voltage": 231.5,
									    "output": false,
									    "apower": 789.1
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("-|789 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 231500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 231500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, 1234) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 1234) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.outputReadValue(ShellySwitch.ChannelId.RELAY, false) //
						.output(IoGen2ShellyBase.ChannelId.HAS_UPDATE, false) //
						.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				// Stable update available
				.next(new TestCase("Stable update available") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {
									      "beta": {
									        "version": "1.7.4-beta1"
									      },
									      "stable": {
									        "version": "1.7.1"
									      }
									    }
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("?|UNDEFINED", sut.debugLog()))

						.output(IoGen2ShellyBase.ChannelId.HAS_UPDATE, true) //
						.outputReadValue(ShellySwitch.ChannelId.RELAY, null)) //

				// No stable update available
				.next(new TestCase("No stable update available") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {
									      "beta": {
									        "version": "1.7.4-beta1"
									      }
									    }
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("?|UNDEFINED", sut.debugLog()))

						.output(IoGen2ShellyBase.ChannelId.HAS_UPDATE, false)) //

				// No update available
				.next(new TestCase("No update available") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {}
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("?|UNDEFINED", sut.debugLog()))

						.output(IoGen2ShellyBase.ChannelId.HAS_UPDATE, false)) //

				// Invalid read response
				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("?|UNDEFINED", sut.debugLog()))

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
						.outputReadValue(ShellySwitch.ChannelId.RELAY, null) //
						.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

				.deactivate();
	}

	@Test
	public void testRelay() throws Exception {
		final var httpTestBundle = DummyBridgeHttpBundle.of();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();
		final var sut = new IoShellyPlugMGen3Impl();

		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
				.addReference("httpBridgeShellyServiceDefinition",
						new HttpBridgeShellyService.HttpBridgeShellyServiceDefinition()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setPhase(L1) //
						.setIp("127.0.0.1") //
						.setType(PRODUCTION) //
						.setInvert(false) //
						.build()) //

				// Read relay OFF
				.next(new TestCase("Read Relay OFF") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {}
									  },
									  "switch:0": {
									    "current": 0.0,
									    "voltage": 231.5,
									    "output": false,
									    "apower": 0.0
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("-|0 W", sut.debugLog()))

						.outputReadValue(ShellySwitch.ChannelId.RELAY, false) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				// Write relay ON
				.next(new TestCase("Write Relay ON") //
						.onBeforeControllersCallbacks(() -> {
							sut.setRelay(true);
						}) //
						.also(testCase -> {
							final var relayTurnedOn = httpTestBundle
									.expect("http://127.0.0.1/rpc/Switch.Set?id=0&on=true").toBeCalled();

							testCase.onBeforeControllersCallbacks(dummyCycleSubscriber::triggerNextCycle);

							testCase.onAfterWriteCallbacks(
									() -> assertTrue(relayTurnedOn.get(), "Failed to turn on relay"));
						})) //

				// Read relay ON
				.next(new TestCase("Read Relay ON") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {}
									  },
									  "switch:0": {
									    "current": 1.234,
									    "voltage": 231.5,
									    "output": true,
									    "apower": 789.1
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("x|789 W", sut.debugLog()))

						.outputReadValue(ShellySwitch.ChannelId.RELAY, true) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 789) //
						.output(ElectricityMeter.ChannelId.CURRENT, 1234) //
						.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				// Write relay OFF
				.next(new TestCase("Write Relay OFF") //
						.onBeforeControllersCallbacks(() -> {
							sut.setRelay(false);
						}) //
						.also(testCase -> {
							final var relayTurnedOff = httpTestBundle
									.expect("http://127.0.0.1/rpc/Switch.Set?id=0&on=false").toBeCalled();

							testCase.onBeforeControllersCallbacks(dummyCycleSubscriber::triggerNextCycle);

							testCase.onAfterWriteCallbacks(
									() -> assertTrue(relayTurnedOff.get(), "Failed to turn off relay"));
						})) //

				// Read relay OFF again
				.next(new TestCase("Read Relay OFF again") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {}
									  },
									  "switch:0": {
									    "current": 0.0,
									    "voltage": 231.5,
									    "output": false,
									    "apower": 0.0
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("-|0 W", sut.debugLog()))

						.outputReadValue(ShellySwitch.ChannelId.RELAY, false) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(ElectricityMeter.ChannelId.CURRENT, 0) //
						.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				.deactivate();
	}

	@Test
	public void testInvert() throws Exception {
		final var httpTestBundle = DummyBridgeHttpBundle.of();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();
		final var sut = new IoShellyPlugMGen3Impl();

		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
				.addReference("httpBridgeShellyServiceDefinition",
						new HttpBridgeShellyService.HttpBridgeShellyServiceDefinition()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setPhase(L1) //
						.setIp("127.0.0.1") //
						.setType(PRODUCTION) //
						.setInvert(true) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									{
									  "sys": {
									    "available_updates": {}
									  },
									  "switch:0": {
									    "current": 1.234,
									    "voltage": 231.5,
									    "output": false,
									    "apower": 789.1
									  }
									}
									"""));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("-|-789 W", sut.debugLog()))

						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, -789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -789) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 231500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 231500) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, -1234) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, -1234) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.outputReadValue(ShellySwitch.ChannelId.RELAY, false) //
						.output(IoGen2ShellyBase.ChannelId.HAS_UPDATE, false) //
						.output(IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				.deactivate();
	}
}
