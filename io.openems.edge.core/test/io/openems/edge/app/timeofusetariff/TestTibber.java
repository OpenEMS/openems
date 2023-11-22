package io.openems.edge.app.timeofusetariff;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
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
		}, null, new PseudoComponentManagerFactory());
	}

	@Test
	public void testRemoveAccessToken() throws Exception {
		final var properties = JsonUtils.buildJsonObject() //
				.addProperty("ACCESS_TOKEN", "g78aw9ht2n112nb453") //
				.build();
		var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
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

	@Test
	public void testAddChannelToPredictor() throws Exception {
		this.createPredictor();

		final var properties = JsonUtils.buildJsonObject() //
				.addProperty("ACCESS_TOKEN", "g78aw9ht2n112nb453") //
				.build();
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.tibber.getAppId(), "key", "alias", properties)).get();

		this.assertChannelsInPredictor("_sum/UnmanagedConsumptionActivePower");
	}

	private void createPredictor() throws Exception {
		this.appManagerTestBundle.componentManger.handleJsonrpcRequest(this.user,
				new CreateComponentConfigRequest("Predictor.PersistenceModel", List.of(//
						new UpdateComponentConfigRequest.Property("id", "predictor0"), //
						new UpdateComponentConfigRequest.Property("channelAddresses", JsonUtils.buildJsonArray()//
								.build()) //
				))).get();
	}

	private void assertChannelsInPredictor(String... channels) throws OpenemsNamedException {
		final var existingAddresses = this.getChannelsInPredictor();
		final var expectedChannels = Stream.of(channels).collect(toSet());
		expectedChannels.removeAll(existingAddresses);
		assertTrue("Missing channels [" + String.join(", ", existingAddresses) + "]", expectedChannels.isEmpty());
	}

	private Set<String> getChannelsInPredictor() throws OpenemsNamedException {
		final var predictor = this.appManagerTestBundle.componentManger.getComponent("predictor0");
		final var existingAddressesArray = (Object[]) predictor.getComponentContext().getProperties()
				.get("channelAddresses");
		return Optional.ofNullable(existingAddressesArray) //
				.map(Stream::of) //
				.map(t -> t.map(String.class::cast)) //
				.orElse(Stream.empty()).collect(toSet());
	}

}
