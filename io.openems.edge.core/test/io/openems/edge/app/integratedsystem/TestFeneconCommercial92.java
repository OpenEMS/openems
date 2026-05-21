package io.openems.edge.app.integratedsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial92;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.MyConfig;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestFeneconCommercial92 {

	private AppManagerTestBundle appManagerTestBundle;

	private SocomecMeter meterApp;

	private void before(final MyConfig initialAppManagerConfig) throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, initialAppManagerConfig, t -> {
			return List.of(//
					Apps.feneconCommercial92(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.prepareBatteryExtension(t), //
					Apps.predictionDefault(t), //
					Apps.predictionUnmanagedConsumption(t), //
					Apps.kdkGridMeter(t), //
					Apps.kdkMeter(t), //
					this.meterApp = Apps.socomecMeter(t) //
			);
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
	}

	private void beforeWithExistingGridMeter() throws Exception {
		this.before(MyConfig.create().setApps("""
				[
				  {
				    "appId": "App.FENECON.Commercial.92",
				    "alias": "alias",
				    "instanceId": "e3a63021-a28a-4b95-b67e-4a8eb55aeefa",
				    "properties": {
					  "SAFETY_COUNTRY": "GERMANY",
					  "FEED_IN_TYPE": "NO_LIMITATION",
					  "BATTERY_TARGET": "AUTO"
				  },
				    "dependencies": [
					  {
					    "key": "GRID_METER",
					    "instanceId": "e56451b7-d122-4baf-8ccd-b19e4c3c4fad"
					  }
				    ]
				  },
				  {
					"appId": "App.Meter.Kdk",
					"alias": "Netzzähler",
					"instanceId": "e56451b7-d122-4baf-8ccd-b19e4c3c4fad",
					"properties": {
					  "METER_ID": "meter0",
					  "MODBUS_ID": "modbus2",
					  "MODBUS_UNIT_ID": 5,
					  "TYPE": "GRID"
					}
				  }
				]
				""").build());
	}

	@Test
	public void testInitialDependencies() throws Exception {
		this.before(null);
		this.createFullCommercial92();

		var dependencyApps = List.of(this.findInstantiatedAppByAppId("App.PvSelfConsumption.GridOptimizedCharge"),
				this.findInstantiatedAppByAppId("App.PvSelfConsumption.SelfConsumptionOptimization"),
				this.findInstantiatedAppByAppId("App.Ess.PrepareBatteryExtension"),
				this.findInstantiatedAppByAppId("App.Prediction.Default"),
				this.findInstantiatedAppByAppId("App.Prediction.UnmanagedConsumption"),
				this.findInstantiatedAppByAppId("App.GridMeter.Kdk"));

		assertEquals(7, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertTrue(dependencyApps.stream().allMatch(Optional::isPresent));
	}

	@Test
	public void testGetMeterDefaultModbusIdValue() throws Exception {
		this.before(null);
		this.createFullCommercial92();

		final var modbusIdProperty = Arrays.stream(this.meterApp.getProperties()) //
				.filter(t -> t.name.equals(SocomecMeter.Property.MODBUS_ID.name())) //
				.findFirst().orElseThrow();

		assertEquals("modbus3", modbusIdProperty.getDefaultValue(Language.DEFAULT) //
				.map(JsonElement::getAsString) //
				.orElseThrow());
	}

	@Test
	public void testGetGridMeterModbusId() throws Exception {
		this.before(null);
		this.createFullCommercial92();

		var gridMeter = this.appManagerTestBundle.componentManger.getComponent("meter0");

		var modbusId = gridMeter.getComponentContext().getProperties().get("modbus.id");

		assertEquals("modbus2", modbusId);
	}

	@Test
	public void testOldGridMeterDependency() throws Exception {
		this.beforeWithExistingGridMeter();

		final var commercialInstance = this.appManagerTestBundle.sut.getInstantiatedApps().stream() //
				.filter(i -> i.appId.equals("App.FENECON.Commercial.92")) //
				.findFirst() //
				.orElseThrow();

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DummyUser.DUMMY_ADMIN,
				new UpdateAppInstance.Request(commercialInstance.instanceId, commercialInstance.alias, fullSettings()));

		var gridMeter = this.findInstantiatedAppByAppId("App.Meter.Kdk");
		var gridMeterApp = this.findInstantiatedAppByAppId("App.GridMeter.Kdk");

		assertEquals(6, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertTrue(gridMeter.isPresent());
		assertFalse(gridMeterApp.isPresent());

	}

	private void createFullCommercial92() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DummyUser.DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Commercial.92", "key", "alias", fullSettings()));
	}

	private Optional<OpenemsAppInstance> findInstantiatedAppByAppId(String appId) {
		return this.appManagerTestBundle.sut.getInstantiatedApps().stream() //
				.filter(i -> i.appId.equals(appId)) //
				.findFirst();
	}

	/**
	 * Gets a {@link JsonObject} with the full settings for a
	 * {@link FeneconCommercial92}.
	 *
	 * @return the settings object
	 */
	public static JsonObject fullSettings() {
		return JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("GRID_CODE", "VDE_4105") //
				.addProperty("FEED_IN_TYPE", "DYNAMIC_LIMITATION") //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("HAS_ESS_LIMITER_14A", false) //
				.addProperty("BATTERY_TARGET", "AUTO") //
				.build();
	}

}
