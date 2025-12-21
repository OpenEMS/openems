package io.openems.edge.core.appmanager;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance.Request;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppConfig;

public class ComponentDefConfigTest {
	private OpenemsApp app;

	@Test
	public void testConfig() throws Exception {
		final var appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(this.app = Apps.testComponentDefConfig(t));
		}, null, new PseudoComponentManagerFactory());
		appManagerTestBundle.addComponentAggregateTask();

		assertTrue(appManagerTestBundle.sut.findAppById(this.app.getAppId()).isPresent());

		assertEquals(4, appManagerTestBundle.sut.componentManager.getAllComponents().size());

		// install apps
		appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new Request(this.app.getAppId(), "key", "alias", buildJsonObject() //
						.build()));

		assertEquals(1, appManagerTestBundle.sut.getInstantiatedApps().size());

		assertEquals(5, appManagerTestBundle.sut.componentManager.getAllComponents().size());

		assertNotNull(appManagerTestBundle.sut.componentManager.getComponent("test0"));

		appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new Request(this.app.getAppId(), "key", "alias", buildJsonObject() //
						.build()));

		assertNotNull(appManagerTestBundle.sut.componentManager.getComponent("test0"));

		assertNotNull(appManagerTestBundle.sut.componentManager.getComponent("test1"));

		assertEquals(6, appManagerTestBundle.sut.componentManager.getAllComponents().size());

		appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_ADMIN,
				new UpdateAppConfig.Request("test0", JsonUtils.buildJsonObject()//
						.addProperty("minPowerSinglePhase", 1381)//
						.build()));

		assertEquals(6, appManagerTestBundle.sut.componentManager.getAllComponents().size());

		appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_ADMIN,
				new UpdateAppConfig.Request("test2", JsonUtils.buildJsonObject()//
						.addProperty("minPowerSinglePhase", 1381)//
						.build()));

		assertEquals(6, appManagerTestBundle.sut.componentManager.getAllComponents().size());

		appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_ADMIN,
				new UpdateAppConfig.Request("test0", JsonUtils.buildJsonObject()//
						.addProperty("minPowerSinglePhase", 1382)//
						.build()));

		assertEquals(6, appManagerTestBundle.sut.componentManager.getAllComponents().size());

		appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_ADMIN,
				new UpdateAppConfig.Request("test2", JsonUtils.buildJsonObject()//
						.addProperty("minPowerSinglePhase", 1382)//
						.build()));

		assertEquals(6, appManagerTestBundle.sut.componentManager.getAllComponents().size());

		assertNotNull(appManagerTestBundle.sut.componentManager.getComponent("test0"));

		assertNotNull(appManagerTestBundle.sut.componentManager.getComponent("test1"));
	}

}
