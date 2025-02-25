package io.openems.edge.controller.cleverpv;

import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getSubElement;
import static io.openems.edge.controller.cleverpv.LogVerbosity.NONE;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.cleverpv.ControllerCleverPvImpl.PowerStorageState;
import io.openems.edge.controller.test.ControllerTest;

public class ControllerCleverPvImplTest {

	@Test
	public void test() throws OpenemsException, Exception {
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
						.setLogVerbosity(NONE) //
						.build())
				.next(new TestCase()) //
				.deactivate();

		{
			sum //
					.withGridActivePower(1000) //
					.withEssSoc(25) //
					.withEssActivePower(-300) //
					.withProductionActivePower(500);
			var d = sut.collectData();
			assertEquals(1000, getAsInt(d, "watt"));
			assertEquals(500, getAsInt(d, "producingWatt"));
			assertEquals(25, getAsInt(d, "soc"));
			assertEquals(PowerStorageState.CHARGING.value, getAsInt(d, "powerStorageState"));
			assertEquals(300, getAsInt(d, "chargingPower"));
		}
		{
			sum.withEssActivePower(567);
			var d = sut.collectData();
			assertEquals(PowerStorageState.DISCHARGING.value, getAsInt(d, "powerStorageState"));
			assertEquals(567, getAsInt(d, "chargingPower"));
		}
		{
			sum.withEssActivePower(0);
			var d = sut.collectData();
			assertEquals(PowerStorageState.IDLE.value, getAsInt(d, "powerStorageState"));
			assertEquals(0, getAsInt(d, "chargingPower"));
		}
		{
			sum.withEssActivePower(null);
			var d = sut.collectData();
			assertEquals(PowerStorageState.DISABLED.value, getAsInt(d, "powerStorageState"));
			assertEquals(JsonNull.INSTANCE, getSubElement(d, "chargingPower"));
		}
	}
}
