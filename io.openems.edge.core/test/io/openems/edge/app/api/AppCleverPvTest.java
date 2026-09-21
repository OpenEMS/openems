package io.openems.edge.app.api;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

class AppCleverPvTest {

	private static final String URL = "https://push.clever-pv.com/api/v1/first-id/electricMeters/fenecon/"
			+ "second-id?code=first-code";
	private static final String UPDATED_URL = "https://push.clever-pv.com/api/v1/new-first-id/electricMeters/fenecon/"
			+ "new-second-id?code=new-code";
	private static final String INCOMPLETE_URL = "https://push.clever-pv.com/api/v1/first-id/electricMeters/fenecon/"
			+ "second-id?code=";

	private AppManagerTestBundle appManagerTestBundle;
	private AppCleverPv cleverPv;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> List.of(//
				this.cleverPv = Apps.cleverPv(t) //
		), null, new PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
	}

	@Test
	void testHasOnlyExpectedProperties() {
		this.appManagerTestBundle //
				.withApp(this.cleverPv) //
				.hasOnlyProperties(//
						AppCleverPv.Property.CONTROLLER_ID, //
						AppCleverPv.Property.ALIAS, //
						AppCleverPv.Property.URL, //
						AppCleverPv.Property.PRIVACY_POLICY);
	}

	@Test
	void testRejectIncompleteUrl() throws Exception {
		final var exception = assertThrows(OpenemsNamedException.class, () -> this.installWithUrl(INCOMPLETE_URL));

		assertTrue(exception.getMessage().contains("Push-API-URL ist unvollständig oder ungültig"));
		this.appManagerTestBundle.assertInstalledApps(0);
	}

	@Test
	void testUpdateUrl() throws Exception {
		final var instance = this.installWithUrl(URL);
		this.assertUrl(URL);

		this.updateUrl(instance.instanceId, "xxx");
		this.assertUrl(URL);

		this.updateUrl(instance.instanceId, UPDATED_URL);
		this.assertUrl(UPDATED_URL);
	}

	private OpenemsAppInstance installWithUrl(String url) throws OpenemsNamedException {
		return this.appManagerTestBundle.sut
				.handleAddAppInstanceRequest(DUMMY_ADMIN,
						new AddAppInstance.Request(this.cleverPv.getAppId(), "key", "alias", properties(url)))
				.instance();
	}

	private void updateUrl(UUID instanceId, String url) throws OpenemsNamedException {
		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(instanceId, "alias", properties(url)));
	}

	private static JsonObject properties(String url) {
		return JsonUtils.buildJsonObject() //
				.addProperty("URL", url) //
				.addProperty("PRIVACY_POLICY", true) //
				.build();
	}

	private void assertUrl(String url) throws OpenemsNamedException {
		this.appManagerTestBundle.assertComponentExist(//
				new EdgeConfig.Component("ctrlCleverPv0", "alias", "Controller.Clever-PV", //
						JsonUtils.buildJsonObject() //
								.addProperty("url", url) //
								.addProperty("readOnly", true) //
								.build()));
	}
}
