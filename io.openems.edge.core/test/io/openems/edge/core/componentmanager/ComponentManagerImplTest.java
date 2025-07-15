package io.openems.edge.core.componentmanager;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyEventAdmin;
import io.openems.edge.common.test.DummyServiceComponentRuntime;
import io.openems.edge.controller.api.Controller;

public class ComponentManagerImplTest {

	@Test
	public void testHandleUpdateComponentConfigRequest() throws OpenemsException, Exception {
		final var cm = new DummyConfigurationAdmin();
		final var ess0 = new MyDummyComponent("ess0");
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);
		final var battery0Target = generateReferenceTargetFilter(ess0.servicePid(), "battery0");
		final var batteryInverter0Target = generateReferenceTargetFilter(ess0.servicePid(), "batteryInverter0");
		final var config = cm.getOrCreateEmptyConfiguration(ess0.id()) //
				.addProperty("id", "ess0") //
				.addProperty("alias", "Energy Storage System") //
				.addProperty("enabled", true) //
				.addProperty("startStop", "START") //
				.addProperty("battery.id", "battery0") //
				.addProperty("battery.target", battery0Target) //
				.addProperty("batteryInverter.id", "batteryInverter0") //
				.addProperty("batteryInverter.target", batteryInverter0Target); //
		final var properties = config.getProperties();

		final var sut = new ComponentManagerImpl();
		new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("serviceComponentRuntime", new DummyServiceComponentRuntime()) //
				.addReference("eventAdmin", new DummyEventAdmin()) //
				.addComponent(ess0) //
				.activate(MyConfig.create() //
						.build());

		// First Test: Property without matching '.target'

		// Before
		assertEquals(8, properties.size());
		assertEquals("ess0", properties.get("id"));
		assertEquals("Energy Storage System", properties.get("alias"));
		assertEquals(true, properties.get("enabled"));
		assertEquals("START", properties.get("startStop"));
		assertEquals("battery0", properties.get("battery.id"));
		assertEquals(battery0Target, properties.get("battery.target"));
		assertEquals("batteryInverter0", properties.get("batteryInverter.id"));
		assertEquals(batteryInverter0Target, properties.get("batteryInverter.target"));

		sut.handleUpdateComponentConfigRequest(null, new UpdateComponentConfig.Request(ess0.id(), List.of(//
				new Property("startStop", "STOP") //
		)));

		// After
		assertEquals(10, properties.size());
		assertEquals("UNDEFINED", properties.get("_lastChangeBy")); // new internal property
		assertNotNull(properties.get("_lastChangeAt")); // new internal property
		assertEquals("STOP", properties.get("startStop")); // changed property
		assertEquals(battery0Target, properties.get("battery.target")); // unchanged
		assertEquals(batteryInverter0Target, properties.get("batteryInverter.target")); // unchanged

		// Second Test: Property with matching '.target' -> reset
		sut.handleUpdateComponentConfigRequest(null, new UpdateComponentConfig.Request(ess0.id(), List.of(//
				new Property("battery.id", "battery1") //
		)));
		assertEquals(10, properties.size());
		assertEquals("battery1", properties.get("battery.id")); // changed property
		assertEquals("(enabled=true)", properties.get("battery.target")); // reset to default
		assertEquals(batteryInverter0Target, properties.get("batteryInverter.target")); // unchanged
	}

	private static class MyDummyComponent extends AbstractDummyOpenemsComponent<MyDummyComponent>
			implements OpenemsComponent {

		public MyDummyComponent(String id) {
			super(id, id, //
					new DummyComponentContext() //
							.addProperty("service.factoryPid", "My.Dummy.Component") //
							.addProperty("service.pid", "My.Dummy.Component." + randomUUID()), //
					OpenemsComponent.ChannelId.values(), //
					Controller.ChannelId.values());
		}

		@Override
		protected MyDummyComponent self() {
			return this;
		}
	}
}
