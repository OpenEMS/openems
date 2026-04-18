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

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;

class AppHeatAskomaTest {

	private AppManagerTestBundle appManagerTestBundle;
	private AppHeatAskoma heatAskoma;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> ImmutableList.of(//
				this.heatAskoma = Apps.heatAskoma(t) //
		), null, new AppManagerTestBundle.PseudoComponentManagerFactory());
	}

	@Test
	void testCreateApp() throws Exception {
		this.createApp(DUMMY_ADMIN);

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		final var instance = this.appManagerTestBundle.findFirst(this.heatAskoma.getAppId());
		assertEquals("App.Heat.Askoma", instance.appId);
	}

	@Test
	void testGetAppPermissions() {
		final var permissions = this.heatAskoma.getAppPermissions();

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

	private OpenemsAppInstance createApp(User user) throws Exception {
		return this.appManagerTestBundle.sut.handleAddAppInstanceRequest(user,
				new AddAppInstance.Request(this.heatAskoma.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
						.addProperty("IP", "192.168.2.118") //
						.addProperty("MAX_HEAT_POWER", 30000) //
						.build()))
				.instance();
	}
}
