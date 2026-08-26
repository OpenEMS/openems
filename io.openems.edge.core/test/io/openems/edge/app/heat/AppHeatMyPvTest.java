package io.openems.edge.app.heat;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.common.test.DummyUser.DUMMY_GUEST;
import static io.openems.edge.common.test.DummyUser.DUMMY_INSTALLER;
import static io.openems.edge.common.test.DummyUser.DUMMY_OWNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;

class AppHeatMyPvTest {

	private static final String IP = "192.168.178.152";
	private static final int MAX_HEAT_POWER = 3000;

	private AppManagerTestBundle appManagerTestBundle;
	private AppHeatMyPv heatMyPv;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> List.of(//
				this.heatMyPv = Apps.heatMyPv(t) //
		), null, new AppManagerTestBundle.PseudoComponentManagerFactory());
		this.appManagerTestBundle.addComponentAggregateTask();
	}

	@Test
	void testCreateApp() throws Exception {
		this.createApp(DUMMY_ADMIN);

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		final var instance = this.appManagerTestBundle.findFirst(this.heatMyPv.getAppId());
		assertEquals("App.Heat.MyPv", instance.appId);
	}

	@Test
	void testGeneratedComponents() throws Exception {
		this.createApp(DUMMY_ADMIN);

		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component("heat0", null, "Heat.MyPv", JsonUtils.buildJsonObject() //
						.addProperty("readOnly", false) //
						.addProperty("modbus.id", "modbus0") //
						.addProperty("maxHeatPower", MAX_HEAT_POWER) //
						.build()), //
				new EdgeConfig.Component("modbus0", null, "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
						.addProperty("ip", IP) //
						.addProperty("port", 502) //
						.build()) //
		);
	}

	@Test
	void testGetAppPermissions() {
		final var permissions = this.heatMyPv.getAppPermissions();

		assertEquals(List.of(Role.ADMIN), permissions.canInstall());
		assertEquals(Role.ADMIN, permissions.canSee());
		assertEquals(Role.ADMIN, permissions.canDelete());
	}

	@Test
	void testInstallPermissions() throws Exception {
		this.createApp(DUMMY_ADMIN);
		this.appManagerTestBundle.assertInstalledApps(1);

		this.beforeEach();
		assertThrows(OpenemsNamedException.class, () -> this.createApp(DUMMY_INSTALLER));
		this.appManagerTestBundle.assertInstalledApps(0);

		this.beforeEach();
		assertThrows(OpenemsNamedException.class, () -> this.createApp(DUMMY_OWNER));
		this.appManagerTestBundle.assertInstalledApps(0);

		this.beforeEach();
		assertThrows(OpenemsNamedException.class, () -> this.createApp(DUMMY_GUEST));
		this.appManagerTestBundle.assertInstalledApps(0);
	}

	@Test
	void testDeletePermissions() throws Exception {
		final var ownerInstalledInstance = this.createApp(DUMMY_ADMIN);
		this.appManagerTestBundle.assertInstalledApps(1);
		this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(DUMMY_ADMIN,
				new DeleteAppInstance.Request(ownerInstalledInstance.instanceId));
		this.appManagerTestBundle.assertInstalledApps(0);

		this.beforeEach();
		final var ownerDeleteInstance = this.createApp(DUMMY_ADMIN);
		assertThrows(OpenemsNamedException.class,
				() -> this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(DUMMY_OWNER,
						new DeleteAppInstance.Request(ownerDeleteInstance.instanceId)));
		this.appManagerTestBundle.assertInstalledApps(1);

		this.beforeEach();
		final var intstallerDeleteInstance = this.createApp(DUMMY_ADMIN);
		assertThrows(OpenemsNamedException.class,
				() -> this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(DUMMY_INSTALLER,
						new DeleteAppInstance.Request(intstallerDeleteInstance.instanceId)));
		this.appManagerTestBundle.assertInstalledApps(1);

		this.beforeEach();
		final var guestDeleteInstance = this.createApp(DUMMY_ADMIN);
		assertThrows(OpenemsNamedException.class,
				() -> this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(DUMMY_GUEST,
						new DeleteAppInstance.Request(guestDeleteInstance.instanceId)));
		this.appManagerTestBundle.assertInstalledApps(1);
	}

	@Test
	void testMaxHeatPowerFieldConstraints() {
		final var def = AppHeatMyPv.Property.MAX_HEAT_POWER.def();
		final var parameter = new BundleParameter(AbstractOpenemsApp.getTranslationBundle(Language.DEFAULT));

		final var field = def.getField().get(this.heatMyPv, AppHeatMyPv.Property.MAX_HEAT_POWER, Language.DEFAULT,
				parameter);
		final var jsonField = field.build();

		final var templateOptions = jsonField.get("templateOptions").getAsJsonObject();
		assertEquals(0, templateOptions.get("min").getAsInt());
		assertEquals(9000, templateOptions.get("max").getAsInt());
	}

	@Test
	void testMaxHeatPowerDefaultValue() {
		final var def = AppHeatMyPv.Property.MAX_HEAT_POWER.def();
		final var parameter = new BundleParameter(AbstractOpenemsApp.getTranslationBundle(Language.DEFAULT));

		final var defaultValue = def.getDefaultValue().get(this.heatMyPv, AppHeatMyPv.Property.MAX_HEAT_POWER,
				Language.DEFAULT, parameter);
		assertEquals(MAX_HEAT_POWER, defaultValue.getAsInt());
	}

	private OpenemsAppInstance createApp(User user) throws Exception {
		return this.appManagerTestBundle.sut.handleAddAppInstanceRequest(user,
				new AddAppInstance.Request(this.heatMyPv.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
						.addProperty("IP", IP) //
						.addProperty("MAX_HEAT_POWER", MAX_HEAT_POWER) //
						.build()))
				.instance();
	}
}
