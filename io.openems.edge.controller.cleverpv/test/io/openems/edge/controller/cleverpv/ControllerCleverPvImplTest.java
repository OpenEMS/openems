package io.openems.edge.controller.cleverpv;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getSubElement;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_DISCHARGE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_SOC;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_ACTIVE_POWER;
import static io.openems.edge.controller.cleverpv.RemoteControlMode.NO_DISCHARGE;
import static io.openems.edge.controller.cleverpv.RemoteControlMode.OFF;
import static io.openems.edge.controller.cleverpv.ControllerCleverPv.ChannelId.REMOTE_CONTROL_MODE;
import static org.junit.Assert.assertEquals;

import java.util.Objects;

import io.openems.edge.common.test.DummyMeta;
import org.junit.Test;

import com.google.gson.JsonNull;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.types.DebugMode;
import io.openems.edge.common.host.DummyHost;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class ControllerCleverPvImplTest {

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new ControllerCleverPvImpl();
		final var sum = new DummySum();
		final var host = new DummyHost();
		new ControllerTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("host", host) //
				.addReference("sum", sum) //
				.addReference("meta", new DummyMeta("meta0")) //
				.addComponent(sum) //
				.activate(MyConfig.create() //
						.setId("ctrlCleverPv0") //
						.setUrl("") //
						.setMode(ControlMode.OFF) //
						.setDebugMode(DebugMode.OFF) //
						.build())
				.next(new TestCase().output("ctrlCleverPv0", REMOTE_CONTROL_MODE, OFF)) //
				.deactivate();

		{
			sum //
					.withGridActivePower(1000) //
					.withEssSoc(25) //
					.withEssDischargePower(-300) //
					.withProductionActivePower(500);
			var data = sut.collectData();
			var dataAsJson = Types.SendData.serializer().serialize(data);
			assertEquals(1000, getAsInt(dataAsJson, "watt"));
			assertEquals(500, getAsInt(dataAsJson, "producingWatt"));
			assertEquals(25, getAsInt(dataAsJson, "soc"));
			assertEquals(PowerStorageState.DISCHARGING.value, getAsInt(dataAsJson, "powerStorageState"));
			assertEquals(-300, getAsInt(dataAsJson, "chargingPower"));
		}
		{
			sum.withEssDischargePower(567);
			var data = sut.collectData();
			var dataAsJson = Types.SendData.serializer().serialize(data);
			assertEquals(PowerStorageState.CHARGING.value, getAsInt(dataAsJson, "powerStorageState"));
			assertEquals(567, getAsInt(dataAsJson, "chargingPower"));
		}
		{
			sum.withEssDischargePower(0);
			var data = sut.collectData();
			var dataAsJson = Types.SendData.serializer().serialize(data);
			assertEquals(PowerStorageState.IDLE.value, getAsInt(dataAsJson, "powerStorageState"));
			assertEquals(0, getAsInt(dataAsJson, "chargingPower"));
		}
		{
			sum.withEssDischargePower(null);
			var data = sut.collectData();
			var dataAsJson = Types.SendData.serializer().serialize(data);
			assertEquals(PowerStorageState.DISABLED.value, getAsInt(dataAsJson, "powerStorageState"));
			assertEquals(JsonNull.INSTANCE, getSubElement(dataAsJson, "chargingPower"));
		}
	}

	@Test
	public void testNoDischarge() throws Exception {
		final var clock = createDummyClock();
		final var executor = dummyBridgeHttpExecutor(clock);
		final var fetcher = dummyEndpointFetcher();

		fetcher.addEndpointHandler(t -> {

			if (Objects.equals(t.body(), """
					{
					  "state": 0,
					  "watt":1000,
					  "producingWatt":500,
					  "soc":25,
					  "powerStorageState":1,
					  "chargingPower":-300,
					  "currentData":{
						"sumGridActivePower":1000,
						"productionActivePower":500,
						"sumEssSoc":25,
						"sumEssDischargePower":-300
					  },
					  "availableControlModes":{
						"ess":[
						  {"mode":"NO_DISCHARGE"}
						]
					  },
					  "activeControlModes":{
						"ess" :{
						  "mode": "OFF"
						}
					  }
					}
					""".replaceAll("\\s+", ""))) {
				return HttpResponse.ok("""
						{ activateControlModes: {
							ess: {
								mode: "NO_DISCHARGE" }
							}
						}
						""");
			}

			throw HttpError.ResponseError.notFound();
		});

		final var sut = new ControllerCleverPvImpl(); //
		final var sum = new DummySum(); //
		final var host = new DummyHost();
		final var ess = new DummyManagedSymmetricEss("ess0"); //
		final var power = new DummyPower(); //

		new ControllerTest(sut) //
				.addReference("ess", ess) //
				.addReference("power", power) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofBridgeImpl(//
						() -> fetcher, //
						() -> executor))
				.addReference("host", host) //
				.addReference("sum", sum) //
				.addReference("meta", new DummyMeta("meta0")) //
				.addComponent(sum) //
				.activate(MyConfig.create() //
						.setId("ctrlCleverPv0") //
						.setReadOnly(false) //
						.setUrl("127.0.0.1") //
						.setMode(ControlMode.REMOTE_CONTROL) //
						.setDebugMode(DebugMode.OFF) //
						.build())
				.next(new TestCase() //
						.input("_sum", GRID_ACTIVE_POWER, 1000) //
						.input("_sum", PRODUCTION_ACTIVE_POWER, 500) //
						.input("_sum", ESS_SOC, 25) //
						.input("_sum", ESS_DISCHARGE_POWER, -300) //
						.onAfterWriteCallbacks(executor::update)) //
				.next(new TestCase() //
						.also(testCase -> {
							executor.update();
						}).onAfterWriteCallbacks(executor::update) //
						.output("ctrlCleverPv0", REMOTE_CONTROL_MODE, NO_DISCHARGE)) //
				.deactivate();
	}

}
