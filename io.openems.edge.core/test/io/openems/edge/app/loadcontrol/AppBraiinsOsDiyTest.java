package io.openems.edge.app.loadcontrol;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Dictionary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

class AppBraiinsOsDiyTest {

	private static final String APP_ID = "App.LoadControl.BraiinsOs";
	private static final String CONTROLLER_ID = "ctrlBraiinsSingle0";
	private static final String IP = "192.168.1.199";
	private static final String USERNAME = "root";
	private static final int DEFAULT_CONSUMPTION = 3000;
	private static final String PASSWORD = "test123";

	private AppManagerTestBundle appManagerTestBundle;
	private AppBraiinsOsDiy braiinsOsDiy;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> ImmutableList.of(//
				this.braiinsOsDiy = Apps.braiinsOsDiy(t) //
		), null, new AppManagerTestBundle.PseudoComponentManagerFactory());
		this.appManagerTestBundle.addComponentAggregateTask();
	}

	@Test
	void testHasOnlyExpectedProperties() {
		this.appManagerTestBundle //
				.withApp(this.braiinsOsDiy) //
				.hasOnlyProperties(//
						AppBraiinsOsDiy.Property.COMPONENT_ID, //
						AppBraiinsOsDiy.Property.ALIAS, //
						AppBraiinsOsDiy.Property.PHASE, //
						AppBraiinsOsDiy.Property.IP, //
						AppBraiinsOsDiy.Property.READ_ONLY, //
						AppBraiinsOsDiy.Property.DEFAULT_CONSUMPTION, //
						AppBraiinsOsDiy.Property.USERNAME, //
						AppBraiinsOsDiy.Property.PASSWORD, //
						AppBraiinsOsDiy.Property.DISCLAIMER_CONFIRMATION);
	}

	@Test
	void testCreateAppAndComponents() throws Exception {
		final var instance = this.createApp(DUMMY_ADMIN, null);

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertEquals(APP_ID, instance.appId);

		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component(CONTROLLER_ID, null, "Controller.BraiinsOS.Single", JsonUtils.buildJsonObject() //
						.addProperty("phase", "L1") //
						.addProperty("ip", IP) //
						.addProperty("readOnly", true) //
						.addProperty("defaultConsumptionW", DEFAULT_CONSUMPTION) //
						.addProperty("username", USERNAME) //
						.build()));
	}

	@Test
	void testPasswordNotStoredInAppAndNotVisibleInEdgeConfig() throws Exception {
		final var instance = this.createApp(DUMMY_ADMIN, PASSWORD);

		assertEquals(PASSWORD, this.getControllerProperties().get("password"));

		final var passwordDef = Arrays.stream(this.braiinsOsDiy.getProperties()) //
				.filter(t -> AppBraiinsOsDiy.Property.PASSWORD.name().equals(t.name)) //
				.findFirst() //
				.orElseThrow();
		assertFalse(passwordDef.isAllowedToSave);

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN, //
				new UpdateAppInstance.Request(instance.instanceId, instance.alias, //
						properties(CommonProps.MASKED_PASSWORD)));

		final var updatedInstance = this.appManagerTestBundle.findFirst(this.braiinsOsDiy.getAppId());
		assertNotNull(updatedInstance);
		final var storedPassword = JsonUtils.getAsOptionalString(updatedInstance.properties,
				AppBraiinsOsDiy.Property.PASSWORD.name());
		assertTrue(storedPassword.isEmpty() //
				|| CommonProps.MASKED_PASSWORD.equals(storedPassword.get()));

		assertEquals(PASSWORD, this.getControllerProperties().get("password"));
	}

	@Test
	void testPermissions() {
		this.appManagerTestBundle.assertPermissions(this.braiinsOsDiy, properties(null)) //
				.canSeeWithOnlyRoles(Role.ADMIN, Role.INSTALLER, Role.OWNER) //
				.canDeleteWithOnlyRoles(Role.ADMIN, Role.INSTALLER, Role.OWNER) //
				.canInstallWithOnlyRoles(Role.ADMIN, Role.INSTALLER, Role.OWNER);
	}

	private OpenemsAppInstance createApp(User user, String password) throws Exception {
		return this.appManagerTestBundle.sut //
				.handleAddAppInstanceRequest(user, //
						new AddAppInstance.Request(this.braiinsOsDiy.getAppId(), "key", "BraiinsOS", //
								properties(password)))
				.instance();
	}

	private Dictionary<String, Object> getControllerProperties() throws OpenemsNamedException {
		return this.appManagerTestBundle.componentManger //
				.getPossiblyDisabledComponent(CONTROLLER_ID) //
				.getComponentContext() //
				.getProperties();
	}

	private static JsonObject properties(String password) {
		return JsonUtils.buildJsonObject() //
				.addProperty("COMPONENT_ID", CONTROLLER_ID) //
				.addProperty("IP", IP) //
				.addProperty("PHASE", "L1") //
				.addProperty("READ_ONLY", true) //
				.addProperty("DEFAULT_CONSUMPTION", DEFAULT_CONSUMPTION) //
				.addProperty("USERNAME", USERNAME) //
				.addProperty("DISCLAIMER_CONFIRMATION", true) //
				.onlyIf(password != null, b -> b.addProperty("PASSWORD", password)) //
				.build();
	}
}