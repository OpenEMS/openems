package io.openems.edge.app.timeofusetariff;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class TestTibber {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);
	private AppManagerTestBundle appManagerTestBundle;

	private Tibber tibber;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.tibber = Apps.tibber(t));
		});
	}

	@Test
	public void testRemoveAccessToken() throws Exception {
		final var properties = JsonUtils.buildJsonObject() //
				.addProperty("ACCESS_TOKEN", "g78aw9ht2n112nb453") //
				.build();
		var response = (AddAppInstance.Response) this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.tibber.getAppId(), "key", "alias", properties)).get();

		assertFalse(response.instance.properties.has("ACCESS_TOKEN"));

		final var apps = this.appManagerTestBundle.getAppsFromConfig();

		assertNotEquals(apps.size(), 0);

		assertFalse(JsonUtils.stream(apps) //
				.map(JsonElement::getAsJsonObject).anyMatch(a -> {
					final var propertiesOfInstance = a.get("properties").getAsJsonObject();
					return propertiesOfInstance.has("ACCESS_TOKEN");
				}));
	}

}
