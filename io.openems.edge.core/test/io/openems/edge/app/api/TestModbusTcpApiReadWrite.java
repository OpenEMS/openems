package io.openems.edge.app.api;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;

public class TestModbusTcpApiReadWrite {

	private AppManagerTestBundle appManagerTestBundle;
	private ModbusTcpApiReadOnly modbusTcpApiReadOnly;
	private ModbusTcpApiReadWrite modbusTcpApiReadWrite;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.modbusTcpApiReadOnly = Apps.modbusTcpApiReadOnly(t), //
					this.modbusTcpApiReadWrite = Apps.modbusTcpApiReadWrite(t) //
			);
		});
	}

	@Test
	public void testDeactivateReadOnly() throws Exception {
		// create ReadOnly app
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				this.modbusTcpApiReadOnly.getAppId(), "key", "alias", JsonUtils.buildJsonObject().build()));

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// ACTIVE not set or true
		var readOnlyApp = this.appManagerTestBundle.sut.getInstantiatedApps().get(0);

		if (readOnlyApp.properties.has("ACTIVE")) {
			var isActiv = readOnlyApp.properties.get("ACTIVE").getAsBoolean();
			assertTrue(isActiv);
		}

		// create ReadWrite app
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.modbusTcpApiReadWrite.getAppId(), "key", "alias",
						JsonUtils.buildJsonObject() //
								.addProperty("API_TIMEOUT", 60) //
								.add("COMPONENT_IDS", JsonUtils.buildJsonArray() //
										.add("_sum") //
										.build()) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		final var readWriteApp = this.appManagerTestBundle.sut.getInstantiatedApps().get(0);

		// ACTIVE set and false
		readOnlyApp = this.appManagerTestBundle.sut.getInstantiatedApps().get(0);

		assertTrue(readOnlyApp.properties.has("ACTIVE"));
		var isActiv = readOnlyApp.properties.get("ACTIVE").getAsBoolean();
		assertFalse(isActiv);

		// remove ReadWrite to see if the ReadOnly gets activated
		this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(DUMMY_ADMIN,
				new DeleteAppInstance.Request(readWriteApp.instanceId));

		// ACTIVE not set or true
		readOnlyApp = this.appManagerTestBundle.sut.getInstantiatedApps().get(0);

		if (readOnlyApp.properties.has("ACTIVE")) {
			isActiv = readOnlyApp.properties.get("ACTIVE").getAsBoolean();
			assertTrue(isActiv);
		}
	}

}
