package io.openems.edge.app.integratedsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestFeneconHome {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	private AppManagerTestBundle appManagerTestBundle;

	private FeneconHome homeApp;
	private GridOptimizedCharge gridOptimizedCharge;
	private SelfConsumptionOptimization selfConsumptionOptimization;
	private SocomecMeter socomecMeter;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			this.homeApp = Apps.feneconHome(t);
			this.gridOptimizedCharge = Apps.gridOptimizedCharge(t);
			this.selfConsumptionOptimization = Apps.selfConsumptionOptimization(t);
			this.socomecMeter = Apps.socomecMeter(t);
			return ImmutableList.of(this.homeApp, this.gridOptimizedCharge, this.selfConsumptionOptimization,
					this.socomecMeter);
		});
	}

	@Test
	public void testCreateHomeFullSettings() throws Exception {
		this.createFullHome();
	}

	@Test
	public void testCreateAndUpdateHomeFullSettings() throws Exception {
		var fullConfig = JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("RIPPLE_CONTROL_RECEIVER_ACTIV", false) //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_AC_METER", true) //
				.addProperty("HAS_DC_PV1", true) //
				.addProperty("DC_PV1_ALIAS", "alias pv 1") //
				.addProperty("HAS_DC_PV2", true) //
				.addProperty("DC_PV2_ALIAS", "alias pv 2") //
				.addProperty("HAS_EMERGENCY_RESERVE", true) //
				.addProperty("EMERGENCY_RESERVE_ENABLED", true) //
				.addProperty("EMERGENCY_RESERVE_SOC", 15) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();

		var homeInstance = this.createFullHome();

		this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(homeInstance.instanceId, "aliasrename", fullConfig));
		// expect the same as before
		// make sure every dependency got installed
		assertEquals(this.appManagerTestBundle.sut.getInstantiatedApps().size(), 4);

		// check properties of created apps
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			int expectedDependencies;
			switch (instance.appId) {
			case "App.FENECON.Home":
				expectedDependencies = 3;
				break;
			case "App.PvSelfConsumption.GridOptimizedCharge":
				expectedDependencies = 0;
				break;
			case "App.PvSelfConsumption.SelfConsumptionOptimization":
				expectedDependencies = 0;
				break;
			case "App.Meter.Socomec":
				expectedDependencies = 0;
				break;
			default:
				throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}
	}

	@Test
	public void testRemoveAcMeter() throws Exception {
		var homeInstance = this.createFullHome();

		var configNoMeter = JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("RIPPLE_CONTROL_RECEIVER_ACTIV", false) //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_AC_METER", false) //
				.addProperty("HAS_DC_PV1", true) //
				.addProperty("DC_PV1_ALIAS", "alias pv 1") //
				.addProperty("HAS_DC_PV2", true) //
				.addProperty("DC_PV2_ALIAS", "alias pv 2") //
				.addProperty("HAS_EMERGENCY_RESERVE", true) //
				.addProperty("EMERGENCY_RESERVE_ENABLED", true) //
				.addProperty("EMERGENCY_RESERVE_SOC", 15) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();

		this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(homeInstance.instanceId, "aliasrename", configNoMeter));
		// expect the same as before
		// make sure every dependency got installed
		assertEquals(this.appManagerTestBundle.sut.getInstantiatedApps().size(), 3);

		// check properties of created apps
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			int expectedDependencies;
			switch (instance.appId) {
			case "App.FENECON.Home":
				expectedDependencies = 2;
				break;
			case "App.PvSelfConsumption.GridOptimizedCharge":
				expectedDependencies = 0;
				break;
			case "App.PvSelfConsumption.SelfConsumptionOptimization":
				expectedDependencies = 0;
				break;
			default:
				throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}

	}

	private final OpenemsAppInstance createFullHome() throws Exception {
		var fullConfig = fullSettings();

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request("App.FENECON.Home", "alias", fullConfig));

		// make sure every dependency got installed
		assertEquals(this.appManagerTestBundle.sut.getInstantiatedApps().size(), 4);

		// check properties of created apps
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			int expectedDependencies;
			switch (instance.appId) {
			case "App.FENECON.Home":
				expectedDependencies = 3;
				break;
			case "App.PvSelfConsumption.GridOptimizedCharge":
				expectedDependencies = 0;
				break;
			case "App.PvSelfConsumption.SelfConsumptionOptimization":
				expectedDependencies = 0;
				break;
			case "App.Meter.Socomec":
				expectedDependencies = 0;
				break;
			default:
				throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}

		var homeInstance = this.appManagerTestBundle.sut.getInstantiatedApps().stream()
				.filter(t -> t.appId.equals("App.FENECON.Home")).findAny().orElse(null);

		assertNotNull(homeInstance);
		return homeInstance;
	}

	/**
	 * Gets a {@link JsonObject} with the full settings for a {@link FeneconHome}.
	 * 
	 * @return the settings object
	 */
	public static final JsonObject fullSettings() {
		return JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("RIPPLE_CONTROL_RECEIVER_ACTIV", false) //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_AC_METER", true) //
				.addProperty("HAS_DC_PV1", true) //
				.addProperty("DC_PV1_ALIAS", "alias pv 1") //
				.addProperty("HAS_DC_PV2", true) //
				.addProperty("DC_PV2_ALIAS", "alias pv 2") //
				.addProperty("HAS_EMERGENCY_RESERVE", true) //
				.addProperty("EMERGENCY_RESERVE_ENABLED", true) //
				.addProperty("EMERGENCY_RESERVE_SOC", 15) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();
	}

}
