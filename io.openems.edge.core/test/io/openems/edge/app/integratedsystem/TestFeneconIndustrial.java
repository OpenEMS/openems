package io.openems.edge.app.integratedsystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.fenecon.industrial.s.Isk110;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class TestFeneconIndustrial {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	private AppManagerTestBundle appManagerTestBundle;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return Apps.of(t, //
					Apps::feneconIsk110, //
					Apps::feneconIsk010, //
					Apps::feneconIsk011 //
			);
		});
	}

	@Test
	public void testCreateIndustrial110FullSettings() throws Exception {
		this.createFullIndustrial("App.FENECON.Industrial.S.ISK110");
	}

	@Test
	public void testCreateIndustrial010FullSettings() throws Exception {
		this.createFullIndustrial("App.FENECON.Industrial.S.ISK010");
	}

	@Test
	public void testCreateIndustrial011FullSettings() throws Exception {
		this.createFullIndustrial("App.FENECON.Industrial.S.ISK011");
	}

	private final OpenemsAppInstance createFullIndustrial(final String appId) throws Exception {
		var fullConfig = fullSettings();

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(appId, "key", "alias", fullConfig));

		// make sure every dependency got installed
		assertEquals(this.appManagerTestBundle.sut.getInstantiatedApps().size(), 1);

		// check properties of created apps
		for (var instance : this.appManagerTestBundle.sut.getInstantiatedApps()) {
			final var expectedDependencies = switch (instance.appId) {
			default -> {
				if (instance.appId.equals(appId)) {
					yield 0;
				}
				throw new Exception("App with ID[" + instance.appId + "] should not have been created!");
			}
			};
			if (expectedDependencies == 0 && instance.dependencies == null) {
				continue;
			}
			assertEquals(expectedDependencies, instance.dependencies.size());
		}

		final var industrialApp = this.appManagerTestBundle.sut.getInstantiatedApps().stream()
				.filter(t -> t.appId.equals(appId)).findAny().orElse(null);

		assertNotNull(industrialApp);

		return industrialApp;
	}

	/**
	 * Gets a {@link JsonObject} with the full settings for a {@link Isk110}.
	 * 
	 * @return the settings object
	 */
	public static final JsonObject fullSettings() {
		return JsonUtils.buildJsonObject() //
				.build();
	}

}
