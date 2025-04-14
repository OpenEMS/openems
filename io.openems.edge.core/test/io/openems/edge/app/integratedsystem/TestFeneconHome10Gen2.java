package io.openems.edge.app.integratedsystem;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestFeneconHome10Gen2 {
	private AppManagerTestBundle appManagerTestBundle;
	private SocomecMeter meterApp;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return List.of(//
					Apps.feneconHome10Gen2(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.prepareBatteryExtension(t), //
					this.meterApp = Apps.socomecMeter(t) //
			);
		}, null, new PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
	}

	@Test
	public void testCreate() throws Exception {
		createFullHome(this.appManagerTestBundle, DummyUser.DUMMY_ADMIN);
	}

	private static OpenemsAppInstance createFullHome(AppManagerTestBundle appManagerTestBundle, User user)
			throws Exception {
		var fullConfig = fullSettings();

		appManagerTestBundle.sut.handleAddAppInstanceRequest(user,
				new AddAppInstance.Request("App.FENECON.Home10.Gen2", "key", "alias", fullConfig));

		assertEquals(4, appManagerTestBundle.sut.getInstantiatedApps().size());

		for (var instance : appManagerTestBundle.sut.getInstantiatedApps()) {
			final var expectedDependencies = switch (instance.appId) {
			case "App.FENECON.Home10.Gen2" -> 3;
			case "App.PvSelfConsumption.GridOptimizedCharge" -> 0;
			case "App.PvSelfConsumption.SelfConsumptionOptimization" -> 0;
			case "App.Ess.PrepareBatteryExtension" -> 0;
			default -> throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			};
			if (expectedDependencies == 0 && instance.dependencies == null) {
				continue;
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}

		var homeInstance = appManagerTestBundle.sut.getInstantiatedApps().stream()
				.filter(t -> t.appId.equals("App.FENECON.Home10.Gen2")).findAny().orElse(null);

		assertNotNull(homeInstance);

		return homeInstance;
	}

	@Test
	public final void testCreateAndUpdateFullHome() throws Exception {
		var fullSettings = fullSettings();
		var homeInstance = createFullHome(this.appManagerTestBundle, DummyUser.DUMMY_ADMIN);
		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(homeInstance.instanceId, "aliasrename", fullSettings));
		// expect the same as before
		// make sure every dependency got installed
		assertEquals(4, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			final var expectedDependencies = switch (instance.appId) {
			case "App.FENECON.Home10.Gen2" -> 3;
			case "App.PvSelfConsumption.GridOptimizedCharge" -> 0;
			case "App.PvSelfConsumption.SelfConsumptionOptimization" -> 0;
			case "App.Ess.PrepareBatteryExtension" -> 0;
			default -> throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			};
			if (expectedDependencies == 0 && instance.dependencies == null) {
				continue;
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}

	}

	@Test
	public void testGetMeterDefaultModbusIdValue() throws Exception {
		createFullHome(this.appManagerTestBundle, DUMMY_ADMIN);

		final var modbusIdProperty = Arrays.stream(this.meterApp.getProperties()) //
				.filter(t -> t.name.equals(SocomecMeter.Property.MODBUS_ID.name())) //
				.findFirst().orElseThrow();

		assertEquals("modbus2", modbusIdProperty.getDefaultValue(Language.DEFAULT).map(JsonElement::getAsString).get());
	}

	/**
	 * Gets a {@link JsonObject} with the full settings for a
	 * {@link FeneconHome10Gen2}.
	 * 
	 * @return the settings object
	 */
	public static final JsonObject fullSettings() {
		return JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_DC_PV1", true) //
				.addProperty("DC_PV1_ALIAS", "alias pv 1") //
				.addProperty("HAS_DC_PV2", true) //
				.addProperty("DC_PV2_ALIAS", "alias pv 2") //
				.addProperty("HAS_DC_PV3", true) //
				.addProperty("DC_PV2_ALIAS", "alias pv 3") //
				.addProperty("HAS_EMERGENCY_RESERVE", true) //
				.addProperty("GRID_METER_CATEGORY", GoodWeGridMeterCategory.INTEGRATED_METER)
				.addProperty("EMERGENCY_RESERVE_ENABLED", true) //
				.addProperty("EMERGENCY_RESERVE_SOC", 15) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();
	}

	/**
	 * Gets a {@link JsonObject} with the minimum settings for a
	 * {@link FeneconHome10}.
	 * 
	 * @return the settings object
	 */
	public static final JsonObject minSettings() {
		return JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_EMERGENCY_RESERVE", false) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();
	}

}
