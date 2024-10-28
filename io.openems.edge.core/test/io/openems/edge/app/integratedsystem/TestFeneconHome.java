package io.openems.edge.app.integratedsystem;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestFeneconHome {

	private AppManagerTestBundle appManagerTestBundle;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return Apps.of(t, //
					Apps::feneconHome, //
					Apps::gridOptimizedCharge, //
					Apps::selfConsumptionOptimization, //
					Apps::socomecMeter, //
					Apps::prepareBatteryExtension, //
					Apps::limiter14a //
			);
		}, null, new PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
	}

	@Test
	public void testCreateHomeFullSettings() throws Exception {
		this.createFullHome();
	}

	@Test
	public void testCreateAndUpdateHomeFullSettings() throws Exception {

		var homeInstance = this.createFullHome();

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(homeInstance.instanceId, "aliasrename", fullSettings()));
		// expect the same as before
		// make sure every dependency got installed
		assertEquals(5, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// check properties of created apps
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			final var expectedDependencies = switch (instance.appId) {
			case "App.FENECON.Home" -> 4;
			case "App.PvSelfConsumption.GridOptimizedCharge" -> 0;
			case "App.PvSelfConsumption.SelfConsumptionOptimization" -> 0;
			case "App.Meter.Socomec" -> 0;
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
	public void testRemoveAcMeter() throws Exception {
		var homeInstance = this.createFullHome();

		var configNoMeter = JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("RIPPLE_CONTROL_RECEIVER_ACTIV", false) //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_ESS_LIMITER_14A", false) //
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

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(homeInstance.instanceId, "aliasrename", configNoMeter));
		// expect the same as before
		// make sure every dependency got installed
		assertEquals(4, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// check properties of created apps
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			final var expectedDependencies = switch (instance.appId) {
			case "App.FENECON.Home" -> 3;
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
	public void testFeedInTypeRippleControlReceiver() throws Exception {
		final var properties = fullSettings();
		properties.addProperty("RIPPLE_CONTROL_RECEIVER_ACTIV", true);
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home", "key", "alias", properties));

		final var batteryInverterProps = this.appManagerTestBundle.componentManger.getComponent("batteryInverter0")
				.getComponentContext().getProperties();

		assertEquals("DISABLE", batteryInverterProps.get("feedPowerEnable"));
		assertEquals("ENABLE", batteryInverterProps.get("rcrEnable"));
	}

	@Test
	public void testFeedInTypeDynamicLimitation() throws Exception {
		final var properties = fullSettings();
		properties.addProperty("FEED_IN_TYPE", FeedInType.DYNAMIC_LIMITATION.name());
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home", "key", "alias", properties));

		final var batteryInverterProps = this.appManagerTestBundle.componentManger.getComponent("batteryInverter0")
				.getComponentContext().getProperties();

		assertEquals("ENABLE", batteryInverterProps.get("feedPowerEnable"));
		assertEquals("DISABLE", batteryInverterProps.get("rcrEnable"));
	}

	@Test
	public void testFeedInTypeNoLimitation() throws Exception {
		final var properties = fullSettings();
		properties.addProperty("FEED_IN_TYPE", FeedInType.NO_LIMITATION.name());
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home", "key", "alias", properties));

		final var batteryInverterProps = this.appManagerTestBundle.componentManger.getComponent("batteryInverter0")
				.getComponentContext().getProperties();

		assertEquals("DISABLE", batteryInverterProps.get("feedPowerEnable"));
		assertEquals("DISABLE", batteryInverterProps.get("rcrEnable"));
	}

	@Test
	@Ignore
	public void testEnableLimiter14a() throws Exception {
		final var createSettings = fullSettings();
		createSettings.addProperty(FeneconHome.Property.HAS_ESS_LIMITER_14A.name(), false);

		final var createResponse = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home", "key", "alias", createSettings));

		assertEquals(4, createResponse.instance().dependencies.size());
		assertFalse(this.appManagerTestBundle.sut.getInstantiatedApps().stream()
				.anyMatch(a -> a.appId.equals("App.Ess.Limiter14a")));

		final var updateSettings = fullSettings();
		createSettings.addProperty(FeneconHome.Property.HAS_ESS_LIMITER_14A.name(), true);
		final var updateResponse = this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(createResponse.instance().instanceId, "alias", updateSettings));

		assertEquals(5, updateResponse.instance().dependencies.size());
		assertTrue(this.appManagerTestBundle.sut.getInstantiatedApps().stream()
				.anyMatch(a -> a.appId.equals("App.Ess.Limiter14a")));

	}

	private final OpenemsAppInstance createFullHome() throws Exception {
		return createFullHome(this.appManagerTestBundle, DUMMY_ADMIN);
	}

	/**
	 * Creates a full home and checks if everything got created correctly.
	 * 
	 * @param appManagerTestBundle the {@link AppManagerTestBundle}
	 * @param user                 the {@link User}
	 * @return the created {@link OpenemsAppInstance}
	 * @throws Exception on error
	 */
	public static final OpenemsAppInstance createFullHome(AppManagerTestBundle appManagerTestBundle, User user)
			throws Exception {
		var fullConfig = fullSettings();

		final var response = appManagerTestBundle.sut.handleAddAppInstanceRequest(user,
				new AddAppInstance.Request("App.FENECON.Home", "key", "alias", fullConfig));

		assertEquals(4, response.instance().dependencies.size());

		// make sure every dependency got installed
		assertEquals(5, appManagerTestBundle.sut.getInstantiatedApps().size());

		// check properties of created apps
		for (var instance : appManagerTestBundle.sut.getInstantiatedApps()) {
			final var expectedDependencies = switch (instance.appId) {
			case "App.FENECON.Home" -> 4;
			case "App.PvSelfConsumption.GridOptimizedCharge" -> 0;
			case "App.PvSelfConsumption.SelfConsumptionOptimization" -> 0;
			case "App.Meter.Socomec" -> 0;
			case "App.Ess.PrepareBatteryExtension" -> 0;
			default -> throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			};
			if (expectedDependencies == 0 && instance.dependencies == null) {
				continue;
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}

		var homeInstance = appManagerTestBundle.sut.getInstantiatedApps().stream()
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
				.addProperty("HAS_ESS_LIMITER_14A", false) //
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

	/**
	 * Gets a {@link JsonObject} with the minimum settings for a
	 * {@link FeneconHome}.
	 * 
	 * @return the settings object
	 */
	public static final JsonObject minSettings() {
		return JsonUtils.buildJsonObject() //
				.addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("RIPPLE_CONTROL_RECEIVER_ACTIV", false) //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("FEED_IN_SETTING", "LAGGING_0_95") //
				.addProperty("HAS_AC_METER", false) //
				.addProperty("HAS_DC_PV1", false) //
				.addProperty("HAS_DC_PV2", false) //
				.addProperty("HAS_EMERGENCY_RESERVE", false) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();
	}

}
