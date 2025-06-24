package io.openems.edge.core.appmanager;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsJsonElement;
import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.common.test.DummyUser.DUMMY_INSTALLER;
import static io.openems.edge.common.test.DummyUser.DUMMY_OWNER;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.app.TestPermissions;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppConfig;

public class AppPropertyPermissionsTest {

	private AppManagerTestBundle appManagerTestBundle;

	private TestPermissions testPermissions;

	@Before
	public void setUp() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.testPermissions = Apps.testPermissions(t) //
			);
		});
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.testPermissions.getAppId(), "key", "alias", buildJsonObject() //
						.addProperty(TestPermissions.Property.ID.name(), "id0")
						.addProperty(TestPermissions.Property.ADMIN_ONLY.name(), "val0") //
						.addProperty(TestPermissions.Property.INSTALLER_ONLY.name(), "val0") //
						.addProperty(TestPermissions.Property.EVERYONE.name(), "val0") //
						.build()))
				.instance();
	}

	@Test(expected = OpenemsException.class)
	public void testAdminOnlyAsInstaller() throws OpenemsNamedException {
		final var req = this.request("ADMIN_ONLY");
		this.appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_INSTALLER, req);
	}

	@Test
	public void testAdminOnlyAsAdmin() throws OpenemsNamedException {
		final var req = this.request("ADMIN_ONLY");
		this.appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_ADMIN, req);
	}

	@Test(expected = OpenemsException.class)
	public void testInstallerOnlyAsOwner() throws OpenemsNamedException {
		final var req = this.request("INSTALLER_ONLY");
		this.appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_OWNER, req);
	}

	@Test
	public void testInstallerOnlyAsAdmin() throws OpenemsNamedException {
		final var req = this.request("INSTALLER_ONLY");
		this.appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_ADMIN, req);
	}

	@Test
	public void testEveryoneAsOwner() throws OpenemsNamedException {
		final var req = this.request("EVERYONE");
		this.appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_OWNER, req);
	}

	private UpdateAppConfig.Request request(String val) {
		final var ja = buildJsonArray() //
				.add("val3") //
				.add("val4") //
				.build();
		final var jo = buildJsonObject() //
				.add(val, getAsJsonElement("val1")) //
				.add("UPDATE_ARRAY", ja) //
				.build();

		final var req = new UpdateAppConfig.Request("id0", jo);
		return req;
	}

}
