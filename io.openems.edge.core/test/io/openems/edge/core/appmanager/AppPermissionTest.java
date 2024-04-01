package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_INSTALLER;
import static io.openems.edge.common.test.DummyUser.DUMMY_OWNER;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;

public class AppPermissionTest {

	private AppManagerTestBundle test;

	@Before
	public void beforeEach() throws Exception {
		this.test = new AppManagerTestBundle(null, null, t -> {
			return List.of(//
					DummyApp.create() //
							.setAppId("App.Dummy") //
							.setAppPermissions(OpenemsAppPermissions.create() //
									.setCanDelete(Role.INSTALLER) //
									.build()) //
							.build() //
			);
		});
	}

	@Test
	public void testDeleteSuccess() throws Exception {
		final var instance = this.createDummyApp();

		this.test.assertInstalledApps(1);
		this.deleteDummyApp(instance, DUMMY_INSTALLER);
		this.test.assertInstalledApps(0);
	}

	@Test
	public void testDeleteAccessDenied() throws Exception {
		final var instance = this.createDummyApp();

		this.test.assertInstalledApps(1);
		try {
			this.deleteDummyApp(instance, DUMMY_OWNER);
			fail();
		} catch (Exception e) {
			assertNotNull(e);
		}
		this.test.assertInstalledApps(1);
	}

	private OpenemsAppInstance createDummyApp() throws Exception {
		return this.test.sut.handleAddAppInstanceRequest(DUMMY_INSTALLER,
				new AddAppInstance.Request("App.Dummy", "key", "alias", JsonUtils.buildJsonObject() //
						.build()))
				.get().instance;
	}

	private void deleteDummyApp(OpenemsAppInstance instance, User user) throws Exception {
		this.test.sut.handleDeleteAppInstanceRequest(user, new DeleteAppInstance.Request(instance.instanceId)).get();
	}

}
