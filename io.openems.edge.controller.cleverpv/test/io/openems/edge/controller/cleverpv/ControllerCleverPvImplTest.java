package io.openems.edge.controller.cleverpv;

import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getSubElement;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_DISCHARGE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_SOC;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_ACTIVE_POWER;
import static io.openems.edge.controller.cleverpv.ControlMode.NO_DISCHARGE;
import static io.openems.edge.controller.cleverpv.ControlMode.OFF;
import static io.openems.edge.controller.cleverpv.ControllerCleverPv.ChannelId.CONTROL_MODE;
import static io.openems.edge.controller.cleverpv.LogVerbosity.NONE;
import static org.junit.Assert.assertEquals;

import java.util.Objects;

import org.junit.Test;

import com.google.gson.JsonNull;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
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
		new ControllerTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(sum) //
				.activate(MyConfig.create() //
						.setId("ctrlCleverPv0") //
						.setUrl("") //
						.setMode(Mode.OFF) //
						.setLogVerbosity(NONE) //
						.build())
				.next(new TestCase().output("ctrlCleverPv0", CONTROL_MODE, OFF)) //
				.deactivate();

		{
			sum //
					.withGridActivePower(1000) //
					.withEssSoc(25) //
					.withEssDischargePower(-300) //
					.withProductionActivePower(500);
			var d = sut.collectData();
			assertEquals(1000, getAsInt(d, "watt"));
			assertEquals(500, getAsInt(d, "producingWatt"));
			assertEquals(25, getAsInt(d, "soc"));
			assertEquals(PowerStorageState.DISCHARGING.value, getAsInt(d, "powerStorageState"));
			assertEquals(300, getAsInt(d, "chargingPower"));
		}
		{
			sum.withEssDischargePower(567);
			var d = sut.collectData();
			assertEquals(PowerStorageState.CHARGING.value, getAsInt(d, "powerStorageState"));
			assertEquals(567, getAsInt(d, "chargingPower"));
		}
		{
			sum.withEssDischargePower(0);
			var d = sut.collectData();
			assertEquals(PowerStorageState.IDLE.value, getAsInt(d, "powerStorageState"));
			assertEquals(0, getAsInt(d, "chargingPower"));
		}
		{
			sum.withEssDischargePower(null);
			var d = sut.collectData();
			assertEquals(PowerStorageState.DISABLED.value, getAsInt(d, "powerStorageState"));
			assertEquals(JsonNull.INSTANCE, getSubElement(d, "chargingPower"));
		}
	}

	@Test
	public void testNoDischarge() throws Exception {
		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor();
		final var fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();

		fetcher.addEndpointHandler(t -> {

			if (Objects.equals(t.body(), """
					{
					  "watt":1000,
					  "producingWatt":500,
					  "soc":25,
					  "powerStorageState":1,
					  "chargingPower":300,
					  "currentData":{
						"_sum/GridActivePower":1000,
						"_sum/ProductionActivePower":500,
						"_sum/EssSoc":25,
						"_sum/EssDcDischargePower":1
					  },
					  "availableControlModes":{
						"ess":[
						  {"mode":"NO_DISCHARGE"}
						]
					  },
					  "activeControlModes":{}
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
		final var ess = new DummyManagedSymmetricEss("ess0"); //
		final var power = new DummyPower(); //

		new ControllerTest(sut) //
				.addReference("ess", ess) //
				.addReference("power", power) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofBridgeImpl(//
						DummyBridgeHttpFactory::cycleSubscriber, //
						() -> fetcher, //
						() -> executor))
				.addComponent(sum) //
				.activate(MyConfig.create() //
						.setId("ctrlCleverPv0") //
						.setUrl("127.0.0.1") //
						.setMode(Mode.REMOTE_CONTROL) //
						.setLogVerbosity(NONE) //
						.build())
				.next(new TestCase().input("_sum", GRID_ACTIVE_POWER, 1000) //
						.input("_sum", PRODUCTION_ACTIVE_POWER, 500) //
						.input("_sum", ESS_SOC, 25) //
						.input("_sum", ESS_DISCHARGE_POWER, -300) //
						.onAfterWriteCallbacks(executor::update)) //
				.next(new TestCase().onAfterWriteCallbacks(executor::update).output("ctrlCleverPv0", CONTROL_MODE,
						NO_DISCHARGE)) //
				.deactivate();
	}

}
