package io.openems.edge.app.timeofusetariff;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.validator.CheckAppsNotInstalled;
import io.openems.edge.core.appmanager.validator.CheckCommercial92;
import io.openems.edge.core.appmanager.validator.CheckHome;

public class TestTibber {

	private AppManagerTestBundle appManagerTestBundle;
	private Tibber tibber;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.tibber = Apps.tibber(t) //
			);
		}, null, new PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
		this.appManagerTestBundle.addPersistencePredictorAggregateTask();
	}

	@Test
	public void testRemoveAccessToken() throws Exception {
		final var properties = JsonUtils.buildJsonObject() //
				.addProperty("ACCESS_TOKEN", "g78aw9ht2n112nb453") //
				.build();
		var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.tibber.getAppId(), "key", "alias", properties));

		// in response its set because the access token in the component is not empty
		assertEquals("xxx", response.instance().properties.get("ACCESS_TOKEN").getAsString());

		// in the actual instance there shouldn't be an access token, instead it should
		// only be taken directly from the component
		final var instance = this.appManagerTestBundle.appManagerUtil
				.findInstanceByIdOrError(response.instance().instanceId);
		assertFalse(instance.properties.has("ACCESS_TOKEN"));

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
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.tibber.getAppId(), "key", "alias", properties));

		this.assertChannelsInPredictor("_sum/UnmanagedConsumptionActivePower");
	}

	@Test(expected = OpenemsNamedException.class)
	public void testOnlyCompatibleWithHomeOrCommercial() throws Exception {
		this.appManagerTestBundle.addCheckable(CheckHome.COMPONENT_NAME,
				t -> new CheckHome(t, new CheckAppsNotInstalled(this.appManagerTestBundle.sut,
						AppManagerTestBundle.getComponentContext(CheckAppsNotInstalled.COMPONENT_NAME))));
		this.appManagerTestBundle.addCheckable(CheckCommercial92.COMPONENT_NAME,
				t -> new CheckCommercial92(t, new CheckAppsNotInstalled(this.appManagerTestBundle.sut,
						AppManagerTestBundle.getComponentContext(CheckAppsNotInstalled.COMPONENT_NAME))));

		final var properties = JsonUtils.buildJsonObject() //
				.addProperty("ACCESS_TOKEN", "g78aw9ht2n112nb453") //
				.build();
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.tibber.getAppId(), "key", "alias", properties));
	}

	@Test
	public void testSetTokenValue() throws Exception {
		final var properties = JsonUtils.buildJsonObject() //
				.addProperty("ACCESS_TOKEN", "g78aw9ht2n112nb453") //
				.build();
		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.tibber.getAppId(), "key", "alias", properties));

		final var accessTokenProp = Arrays.stream(this.tibber.getProperties()) //
				.filter(t -> t.name.equals(Tibber.Property.ACCESS_TOKEN.name())) //
				.findAny().orElse(null);
		var value = accessTokenProp.bidirectionalValue.apply(response.instance().properties);

		assertEquals("xxx", value.getAsString());

		this.appManagerTestBundle.componentManger.handleUpdateComponentConfigRequest(DUMMY_ADMIN,
				new UpdateComponentConfigRequest(response.instance().properties
						.get(Tibber.Property.TIME_OF_USE_TARIFF_PROVIDER_ID.name()).getAsString(),
						List.of(new UpdateComponentConfigRequest.Property("accessToken", ""))));

		value = accessTokenProp.bidirectionalValue.apply(response.instance().properties);

		assertEquals(JsonNull.INSTANCE, value);
	}

	@Test
	public void testUnsetFilterValue() throws Exception {
		final var properties = JsonUtils.buildJsonObject() //
				.addProperty(Tibber.Property.ACCESS_TOKEN.name(), "g78aw9ht2n112nb453") //
				.addProperty(Tibber.Property.MULTIPLE_HOMES_CHECK.name(), true) //
				.addProperty(Tibber.Property.FILTER.name(), "randomInitialFilter") //
				.build();
		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.tibber.getAppId(), "key", "alias", properties));

		final var filterProp = Arrays.stream(this.tibber.getProperties()) //
				.filter(t -> t.name.equals(Tibber.Property.FILTER.name())) //
				.findAny().orElseThrow();
		var value = filterProp.bidirectionalValue.apply(response.instance().properties);

		assertEquals("randomInitialFilter", value.getAsString());

		this.appManagerTestBundle.componentManger.handleUpdateComponentConfigRequest(DUMMY_ADMIN,
				new UpdateComponentConfigRequest(
						response.instance().properties.get(Tibber.Property.TIME_OF_USE_TARIFF_PROVIDER_ID.name())
								.getAsString(),
						List.of(new UpdateComponentConfigRequest.Property(Tibber.Property.ACCESS_TOKEN.name(),
								"g78aw9ht2n112nb453"))));

		value = filterProp.bidirectionalValue.apply(response.instance().properties);

		assertTrue(value.isJsonPrimitive());
		assertTrue(value.getAsJsonPrimitive().isString());
		assertEquals("", value.getAsString());
	}

	private void createPredictor() throws Exception {
		this.appManagerTestBundle.componentManger.handleCreateComponentConfigRequest(DUMMY_ADMIN,
				new CreateComponentConfigRequest("Predictor.PersistenceModel", List.of(//
						new UpdateComponentConfigRequest.Property("id", "predictor0"), //
						new UpdateComponentConfigRequest.Property("channelAddresses", JsonUtils.buildJsonArray()//
								.build()) //
				)));
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
