package io.openems.edge.core.componentmanager;

import static io.openems.common.OpenemsConstants.PROPERTY_LAST_CHANGE_AT;
import static io.openems.common.OpenemsConstants.PROPERTY_LAST_CHANGE_BY;
import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.common.test.DummyUser.DUMMY_OWNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.DeleteComponentConfig;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.DummyConfigurationAdmin.DummyConfiguration;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyEventAdmin;
import io.openems.edge.common.test.DummyServiceComponentRuntime;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.componentmanager.tracker.TrackedComponents;

public class ComponentManagerImplTest {

	@Test
	void testHandleUpdateComponentConfigRequest() throws Exception {
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

		final var trackedComponents = new TrackedComponents();
		trackedComponents.activatedComponent(1L, ess0);

		final var sut = new ComponentManagerImpl();
		new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("components", trackedComponents) //
				.addReference("serviceComponentRuntime", new DummyServiceComponentRuntime()) //
				.addReference("eventAdmin", new DummyEventAdmin()) //
				.addComponent(ess0) //
				.activate(MyConfig.create() //
						.build());

		// Build the EdgeConfig cache synchronously (in production this is done by the
		// EdgeConfigWorker thread), so that applyWrittenConfiguration has a cache to
		// update
		var ess0InEdgeConfig = sut.getEdgeConfig().getComponentOrError(ess0.id());
		assertTrue(ess0InEdgeConfig.getProperty(PROPERTY_LAST_CHANGE_BY).isEmpty());

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

		sut.handleUpdateComponentConfigRequestNonGuarded(null, new UpdateComponentConfig.Request(ess0.id(), List.of(//
				new Property("startStop", "STOP") //
		)));

		// After
		assertEquals(10, properties.size());
		assertEquals("UNDEFINED", properties.get(PROPERTY_LAST_CHANGE_BY)); // new internal property
		assertNotNull(properties.get(PROPERTY_LAST_CHANGE_AT)); // new internal property
		assertEquals("STOP", properties.get("startStop")); // changed property
		assertEquals(battery0Target, properties.get("battery.target")); // unchanged
		assertEquals(batteryInverter0Target, properties.get("batteryInverter.target")); // unchanged

		// The written Configuration is immediately visible in the EdgeConfig cache
		// ("read your own writes") - without waiting for a ConfigurationEvent
		ess0InEdgeConfig = sut.getEdgeConfig().getComponentOrError(ess0.id());
		assertEquals("UNDEFINED",
				getAsOptionalString(ess0InEdgeConfig.getPropertyOrError(PROPERTY_LAST_CHANGE_BY)).orElse(null));
		assertNotNull(ess0InEdgeConfig.getProperty(PROPERTY_LAST_CHANGE_AT).orElse(null));

		// Second Test: Property with matching '.target' -> reset
		sut.handleUpdateComponentConfigRequestNonGuarded(null, new UpdateComponentConfig.Request(ess0.id(), List.of(//
				new Property("battery.id", "battery1") //
		)));
		assertEquals(10, properties.size());
		assertEquals("battery1", properties.get("battery.id")); // changed property
		assertEquals("(enabled=true)", properties.get("battery.target")); // reset to default
		assertEquals(batteryInverter0Target, properties.get("batteryInverter.target")); // unchanged

		// still visible in the EdgeConfig cache after the second update
		ess0InEdgeConfig = sut.getEdgeConfig().getComponentOrError(ess0.id());
		assertEquals("UNDEFINED",
				getAsOptionalString(ess0InEdgeConfig.getPropertyOrError(PROPERTY_LAST_CHANGE_BY)).orElse(null));
	}

	@Test
	void testHandleCreateAndDeleteComponentConfigRequestGuarded() throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);
		final var config = cm.addConfiguration(new DummyConfiguration() //
				.setFactoryPid("Controller.Debug.Log") //
				.addProperty("id", "ctrlDebugLog0"));

		final var sut = createSut(cm);

		// CREATE and DELETE are reserved for ADMIN, missing User (null) is denied also
		for (var user : new User[] { DUMMY_OWNER, DummyUser.DUMMY_INSTALLER, null }) {
			var e = assertThrows(OpenemsNamedException.class, //
					() -> sut.handleCreateComponentConfigRequestGuarded(user, //
							new CreateComponentConfig.Request("Controller.Api.Websocket", //
									List.of(new Property("id", "ctrlApiWebsocket0")))));
			assertEquals(OpenemsError.COMMON_ROLE_ACCESS_DENIED, e.getError());
			e = assertThrows(OpenemsNamedException.class, //
					() -> sut.handleDeleteComponentConfigRequestGuarded(user, //
							new DeleteComponentConfig.Request("ctrlDebugLog0")));
			assertEquals(OpenemsError.COMMON_ROLE_ACCESS_DENIED, e.getError());
		}
		assertEquals(0, cm.listConfigurations("(id=ctrlApiWebsocket0)").length); // not created
		assertEquals(0, config.getChangeCount()); // not deleted

		// ADMIN and the internal User.INTERNAL are allowed
		sut.handleCreateComponentConfigRequestGuarded(DUMMY_ADMIN, //
				new CreateComponentConfig.Request("Controller.Api.Websocket", //
						List.of(new Property("id", "ctrlApiWebsocket0"))));
		assertEquals(1, cm.listConfigurations("(id=ctrlApiWebsocket0)").length);
		sut.handleCreateComponentConfigRequestGuarded(User.INTERNAL, //
				new CreateComponentConfig.Request("Controller.Debug.Log", //
						List.of(new Property("id", "ctrlDebugLog1"))));
		assertEquals(1, cm.listConfigurations("(id=ctrlDebugLog1)").length);
		sut.handleDeleteComponentConfigRequestGuarded(DUMMY_ADMIN, //
				new DeleteComponentConfig.Request("ctrlDebugLog0"));
		assertEquals(1, config.getChangeCount());
	}

	@Test
	void testHandleUpdateComponentConfigRequestGuarded() throws Exception {
		final var cm = new DummyConfigurationAdmin();
		cm.getOrCreateEmptyConfiguration(ComponentManager.SINGLETON_SERVICE_PID);
		// 'condensedOutput' is allowlisted; 'showAlias' is not
		final var debugLog = cm.addConfiguration(new DummyConfiguration() //
				.setFactoryPid("Controller.Debug.Log") //
				.addProperty("id", "ctrlDebugLog0") //
				.addProperty("condensedOutput", true) //
				.addProperty("showAlias", false));
		// not allowlisted at all
		final var websocket = cm.addConfiguration(new DummyConfiguration() //
				.setFactoryPid("Controller.Api.Websocket") //
				.addProperty("id", "ctrlApiWebsocket0") //
				.addProperty("port", 8085));

		final var sut = createSut(cm);

		// OWNER can update an allowlisted Property
		sut.handleUpdateComponentConfigRequestGuarded(DUMMY_OWNER, //
				new UpdateComponentConfig.Request("ctrlDebugLog0", //
						List.of(new Property("condensedOutput", false))));
		assertEquals(false, debugLog.getProperty("condensedOutput"));

		// alias-only update is allowed on any Component (UI renaming)
		sut.handleUpdateComponentConfigRequestGuarded(DUMMY_OWNER, //
				new UpdateComponentConfig.Request("ctrlApiWebsocket0", //
						List.of(new Property("alias", "My API"))));
		assertEquals("My API", websocket.getProperty("alias"));

		// denied for OWNER - non-allowlisted Property, non-allowlisted
		// Factory-PID, mixed alias + other Property, unknown Component
		for (var request : new UpdateComponentConfig.Request[] { //
				new UpdateComponentConfig.Request("ctrlDebugLog0", //
						List.of(new Property("showAlias", true))), //
				new UpdateComponentConfig.Request("ctrlApiWebsocket0", //
						List.of(new Property("port", 8086))), //
				new UpdateComponentConfig.Request("ctrlApiWebsocket0", //
						List.of(new Property("alias", "x"), new Property("port", 8090))), //
				new UpdateComponentConfig.Request("unknown0", //
						List.of(new Property("enabled", false))) //
		}) {
			var e = assertThrows(OpenemsNamedException.class,
					() -> sut.handleUpdateComponentConfigRequestGuarded(DUMMY_OWNER, request));
			assertEquals(OpenemsError.COMMON_ROLE_ACCESS_DENIED, e.getError());
		}

		// the same Request as OWNER has surplus (non-allowlisted) Properties silently
		// removed, while ADMIN gets it applied in full - validating the difference
		final var mixedRequest = new UpdateComponentConfig.Request("ctrlDebugLog0", //
				List.of(new Property("condensedOutput", true), new Property("showAlias", true)));
		sut.handleUpdateComponentConfigRequestGuarded(DUMMY_OWNER, mixedRequest);
		assertEquals(true, debugLog.getProperty("condensedOutput")); // allowlisted Property applied
		assertEquals(false, debugLog.getProperty("showAlias")); // surplus Property removed for OWNER
		sut.handleUpdateComponentConfigRequestGuarded(DUMMY_ADMIN, mixedRequest);
		assertEquals(true, debugLog.getProperty("condensedOutput"));
		assertEquals(true, debugLog.getProperty("showAlias")); // applied in full for ADMIN

		// a missing User (null) is denied as well
		var e = assertThrows(OpenemsNamedException.class, () -> sut.handleUpdateComponentConfigRequestGuarded(null, //
				new UpdateComponentConfig.Request("ctrlDebugLog0", //
						List.of(new Property("condensedOutput", false)))));
		assertEquals(OpenemsError.COMMON_ROLE_ACCESS_DENIED, e.getError());
		assertEquals(true, debugLog.getProperty("condensedOutput")); // unchanged by the denied request

		// ADMIN can update anything; the NonGuarded variant applies no allowlist
		sut.handleUpdateComponentConfigRequestGuarded(DUMMY_ADMIN, //
				new UpdateComponentConfig.Request("ctrlApiWebsocket0", //
						List.of(new Property("port", 8086))));
		assertEquals(8086, websocket.getProperty("port"));
		sut.handleUpdateComponentConfigRequestNonGuarded(DUMMY_OWNER, //
				new UpdateComponentConfig.Request("ctrlApiWebsocket0", //
						List.of(new Property("port", 8088))));
		assertEquals(8088, websocket.getProperty("port"));
	}

	private static ComponentManagerImpl createSut(DummyConfigurationAdmin cm) throws Exception {
		final var sut = new ComponentManagerImpl();
		new ComponentTest(sut) //
				.addReference("cm", cm) //
				.addReference("components", new TrackedComponents()) //
				.addReference("serviceComponentRuntime", new DummyServiceComponentRuntime()) //
				.addReference("eventAdmin", new DummyEventAdmin()) //
				.activate(MyConfig.create() //
						.build());
		return sut;
	}

}
