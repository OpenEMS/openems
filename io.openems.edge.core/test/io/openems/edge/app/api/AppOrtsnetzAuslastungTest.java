package io.openems.edge.app.api;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.types.EdgeConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.meter.test.DummyElectricityMeter;

class AppOrtsnetzAuslastungTest {

	private AppManagerTestBundle appManagerTestBundle;
	private AppOrtsnetzAuslastung ortsnetzAuslastung;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> List.of(//
				this.ortsnetzAuslastung = Apps.ortsnetzAuslastung(t) //
		), null, new PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
	}

	@Test
	void testHasOnlyExpectedProperties() {
		this.appManagerTestBundle //
				.withApp(this.ortsnetzAuslastung) //
				.hasOnlyProperties(//
						AppOrtsnetzAuslastung.Property.CONTROLLER_ID, //
						AppOrtsnetzAuslastung.Property.ALIAS, //
						AppOrtsnetzAuslastung.Property.METER_ID, //
						AppOrtsnetzAuslastung.Property.DATA_PROTECTION_HINT);
	}

	@Test
	void testInstallWithDefaults() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.ortsnetzAuslastung.getAppId(), "key", "alias", //
						JsonUtils.buildJsonObject() //
								.addProperty("DATA_PROTECTION_HINT", true) //
								.build()));

		this.assertController("");
	}

	@Test
	void testInstallAndUpdateWithMeter() throws Exception {
		final var instance = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.ortsnetzAuslastung.getAppId(), "key", "alias", //
						JsonUtils.buildJsonObject() //
								.addProperty("METER_ID", "meter1") //
								.addProperty("DATA_PROTECTION_HINT", true) //
								.build()))
				.instance();
		this.assertController("meter1");

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(instance.instanceId, "alias", JsonUtils.buildJsonObject() //
						.addProperty("METER_ID", "") //
						.build()));
		this.assertController("");
	}

	@Test
	void testMeterOptions() {
		final var componentManager = (DummyPseudoComponentManager) this.appManagerTestBundle.componentManger;
		componentManager.addComponent(new DummyElectricityMeter("meter1").withMeterType(MeterType.PRODUCTION));
		componentManager.addComponent(new DummyElectricityMeter("meter0").withMeterType(MeterType.GRID));

		final var options = this.getMeterField().getAsJsonObject("templateOptions").getAsJsonArray("options");
		assertEquals(3, options.size());
		assertEquals("", options.get(0).getAsJsonObject().get("value").getAsString());
		assertEquals("Standard-Netzzähler (meter0: meter0)", options.get(0).getAsJsonObject().get("label").getAsString());
		assertEquals("meter0", options.get(1).getAsJsonObject().get("value").getAsString());
		assertEquals("meter1", options.get(2).getAsJsonObject().get("value").getAsString());
	}

	@Test
	void testMeterOptionsWithoutGridMeter() {
		final var options = this.getMeterField().getAsJsonObject("templateOptions").getAsJsonArray("options");
		assertEquals(1, options.size());
		assertEquals("Standard-Netzzähler (kein Netzzähler vorhanden)",
				options.get(0).getAsJsonObject().get("label").getAsString());
	}

	private JsonObject getMeterField() {
		for (var field : this.ortsnetzAuslastung.getAppAssistant(DUMMY_ADMIN).fields) {
			final var fieldObject = field.getAsJsonObject();
			if ("METER_ID".equals(fieldObject.get("key").getAsString())) {
				return fieldObject;
			}
		}
		throw new AssertionError("METER_ID field not found");
	}

	private void assertController(String meterId) throws Exception {
		this.appManagerTestBundle.assertComponentExist(//
				new EdgeConfig.Component("ctrlOrtsnetzAuslastung0", "alias", "Controller.Ortsnetz-Auslastung", //
						JsonUtils.buildJsonObject() //
								.addProperty("meter.id", meterId) //
								.build()));
	}
}
