package io.openems.edge.io.shelly.shellyaddontemperature;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.bridge.http.cycle.dummy.DummyCycleSubscriber;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.thermometer.api.Thermometer;

public class IoShellyAddonTemperatureImplTest {

	private static final String STATUS_RESPONSE = """
			{
			   "switch:0":{
			      "id":0,
			      "output":false,
			      "apower":0.0
			   },
			   "temperature:100":{
			      "id":100,
			      "tC":21.5,
			      "tF":70.7
			   },
			   "temperature:101":{
			      "id":101,
			      "tC":-3.2,
			      "tF":26.2
			   },
			   "sys":{
			      "mac":"80646FE34998",
			      "restart_required":false
			   }
			}
			""";

	@Test
	public void test() throws Exception {
		final var sut = new IoShellyAddonTemperatureImpl();
		final var httpTestBundle = DummyBridgeHttpBundle.of();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
				.activate(MyConfig.create() //
						.setId("temp0") //
						.setIp("127.0.0.1") //
						.setSensorId(100) //
						.build()) //

				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(STATUS_RESPONSE));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.output(Thermometer.ChannelId.TEMPERATURE, 215) //
						.output(IoShellyAddonTemperature.ChannelId.SLAVE_COMMUNICATION_FAILED, false) //
						.output(IoShellyAddonTemperature.ChannelId.NEEDS_RESTART, false)) //

				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.output(Thermometer.ChannelId.TEMPERATURE, null) //
						.output(IoShellyAddonTemperature.ChannelId.NEEDS_RESTART, null)) //

				.deactivate();
	}

	@Test
	public void testOtherSensorId() throws Exception {
		final var sut = new IoShellyAddonTemperatureImpl();
		final var httpTestBundle = DummyBridgeHttpBundle.of();
		final var dummyCycleSubscriber = new DummyCycleSubscriber();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("httpBridgeCycleServiceDefinition",
						new HttpBridgeCycleServiceDefinition(dummyCycleSubscriber)) //
				.activate(MyConfig.create() //
						.setId("temp1") //
						.setIp("127.0.0.1") //
						.setSensorId(101) //
						.build()) //

				.next(new TestCase("Second sensor") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(STATUS_RESPONSE));
							dummyCycleSubscriber.triggerNextCycle();
						}) //
						.output(Thermometer.ChannelId.TEMPERATURE, -32) //
						.output(IoShellyAddonTemperature.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				.deactivate();
	}
}
