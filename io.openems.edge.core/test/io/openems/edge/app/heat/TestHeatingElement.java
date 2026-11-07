package io.openems.edge.app.heat;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.common.test.DummyUser.DUMMY_INSTALLER;
import static io.openems.edge.common.test.DummyUser.DUMMY_OWNER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.FeneconHome20;
import io.openems.edge.app.integratedsystem.TestFeneconHome20;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.DummyApp;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.enums.Wrappers;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.core.appmanager.validator.ExpressionEvaluator;
import io.openems.edge.io.test.DummyInputOutput;

class TestHeatingElement {

	private static final List<HeatingElement.Property> OUTPUT_CHANNEL_PROPERTIES = List.of(//
			HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L1, //
			HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L2, //
			HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L3, //
			HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L1_HEATING_ELEMENT_B, //
			HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L2_HEATING_ELEMENT_B, //
			HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L3_HEATING_ELEMENT_B //
	);
	private static final int HEATING_ELEMENT_B_FIRST_PROPERTY_INDEX = 3;

	private record RelayAssistantCase(int numberOfChannels, String[] expectedDefaults) {
	}

	private AppManagerTestBundle appManagerTestBundle;
	private HeatingElement heatingElement;
	private FeneconHome20 homeApp;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					Apps.eastronMeter(t), //
					this.heatingElement = Apps.heatingElement(t), //
					this.homeApp = Apps.feneconHome20(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.socomecMeter(t) //
			);
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		this.appManagerTestBundle.addComponentAggregateTask();
	}

	@Test
	void testInternMeterDependencies() throws Exception {
		// install home
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.homeApp.getAppId(), "key", "alias", TestFeneconHome20.fullSettings()));

		assertEquals(4, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		final var result = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.heatingElement.getAppId(), "key", "alias",
						this.getHeatingWithInternMeterJson()));

		assertEquals(6, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN, new UpdateAppInstance.Request(
				result.instance().instanceId, "alias", this.getHeatingWithoutMeterJson()));

		assertEquals(5, this.appManagerTestBundle.sut.getInstantiatedApps().size());

	}

	@ParameterizedTest
	@MethodSource("allUsers")
	void testOneHeatingElementDefaultsAreBackwardsCompatible(User user) throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(user, new AddAppInstance.Request(
				this.heatingElement.getAppId(), "key", "alias", this.getHeatingWithoutMeterJson()));

		this.appManagerTestBundle.assertComponentExist(new EdgeConfig.Component("ctrlIoHeatingElement0", "alias",
				"Controller.IO.HeatingElement", JsonUtils.buildJsonObject() //
						.addProperty("outputChannelPhaseL1", "io0/InputOutput0") //
						.addProperty("outputChannelPhaseL2", "io0/InputOutput1") //
						.addProperty("outputChannelPhaseL3", "io0/InputOutput2") //
						.addProperty("powerPerPhase", 2000) //
						.addProperty("minimumSwitchingTime", 60) //
						.addProperty("numberOfHeatingElements", "ONE") //
						.build()));
	}

	@Test
	void testUpdateToOneDeletesConfiguredHeatingElementBRelays() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				this.heatingElement.getAppId(), "key", "alias", this.getHeatingWithTwoHeatingElementsJson()));

		final var config = this.heatingElement.getAppConfiguration(ConfigurationTarget.UPDATE,
				this.getHeatingWithoutMeterJson(), Language.DEFAULT);
		final var properties = config.getComponents().get(0).properties();

		assertEquals(JsonNull.INSTANCE, properties.getOrNull("outputChannelPhaseL1HeatingElementB").value());
		assertEquals(JsonNull.INSTANCE, properties.getOrNull("outputChannelPhaseL2HeatingElementB").value());
		assertEquals(JsonNull.INSTANCE, properties.getOrNull("outputChannelPhaseL3HeatingElementB").value());
	}

	@Test
	void testUpdateExistingOneDoesNotDeleteUnconfiguredHeatingElementBRelays() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				this.heatingElement.getAppId(), "key", "alias", this.getHeatingWithoutMeterJson()));

		final var config = this.heatingElement.getAppConfiguration(ConfigurationTarget.UPDATE,
				this.getHeatingWithoutMeterJson(), Language.DEFAULT);
		final var properties = config.getComponents().get(0).properties();

		assertNull(properties.getOrNull("outputChannelPhaseL1HeatingElementB"));
		assertNull(properties.getOrNull("outputChannelPhaseL2HeatingElementB"));
		assertNull(properties.getOrNull("outputChannelPhaseL3HeatingElementB"));
	}

	@Test
	void testTwoHeatingElements() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				this.heatingElement.getAppId(), "key", "alias", this.getHeatingWithTwoHeatingElementsJson()));

		this.appManagerTestBundle.assertComponentExist(new EdgeConfig.Component("ctrlIoHeatingElement0", "alias",
				"Controller.IO.HeatingElement", JsonUtils.buildJsonObject() //
						.addProperty("outputChannelPhaseL1", "io0/InputOutput0") //
						.addProperty("outputChannelPhaseL2", "io0/InputOutput1") //
						.addProperty("outputChannelPhaseL3", "io0/InputOutput2") //
						.addProperty("powerPerPhase", 2000) //
						.addProperty("minimumSwitchingTime", 60) //
						.addProperty("numberOfHeatingElements", "TWO") //
						.addProperty("outputChannelPhaseL1HeatingElementB", "io0/InputOutput3") //
						.addProperty("outputChannelPhaseL2HeatingElementB", "io0/InputOutput4") //
						.addProperty("outputChannelPhaseL3HeatingElementB", "io0/InputOutput5") //
						.addProperty("powerPerPhaseHeatingElementB", 1000) //
						.addProperty("distribution", "PARALLEL") //
						.build()));
	}

	private static Stream<User> allUsers() {
		return Stream.of(DUMMY_OWNER, DUMMY_INSTALLER, DUMMY_ADMIN);
	}

	@ParameterizedTest
	@CsvSource({ "false,false", "true,false", "false,true", "true,true" })
	void testRelayDependenciesOnlyIncludeAppManagedRelays(boolean relayAAppManaged, boolean relayBAppManaged)
			throws Exception {
		final var relayApps = Stream.of("io0", "relayBoard0") //
				.map(id -> DummyApp.create() //
						.setAppId("App.Test.Relay." + id) //
						.setConfiguration((t, p, l) -> AppConfiguration.create() //
								.addTask(Tasks
										.component(new EdgeConfig.Component(id, id, "IO.KMtronic", new JsonObject()))) //
								.build()) //
						.build()) //
				.toList();
		final var apps = new HeatingElement[1];
		final var bundle = new AppManagerTestBundle(null, null,
				t -> ImmutableList.of(apps[0] = Apps.heatingElement(t), relayApps.get(0), relayApps.get(1)), null,
				new AppManagerTestBundle.PseudoComponentManagerFactory());
		bundle.addComponentAggregateTask();

		final var expectedDependencies = new HashMap<String, UUID>();
		if (relayAAppManaged) {
			final var result = bundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
					new AddAppInstance.Request(relayApps.get(0).getAppId(), "key", "relay A", new JsonObject()));
			expectedDependencies.put("RELAY", result.instance().instanceId);
		}
		if (relayBAppManaged) {
			final var result = bundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
					new AddAppInstance.Request(relayApps.get(1).getAppId(), "key", "relay B", new JsonObject()));
			expectedDependencies.put("RELAY2", result.instance().instanceId);
		}

		final var properties = this.getHeatingWithTwoHeatingElementsJson();
		properties.addProperty("OUTPUT_CHANNEL_PHASE_L1_HEATING_ELEMENT_B", "relayBoard0/Relay1");
		properties.addProperty("OUTPUT_CHANNEL_PHASE_L2_HEATING_ELEMENT_B", "relayBoard0/Relay2");
		properties.addProperty("OUTPUT_CHANNEL_PHASE_L3_HEATING_ELEMENT_B", "relayBoard0/Relay3");
		final var config = apps[0].getAppConfiguration(ConfigurationTarget.ADD, properties, Language.DEFAULT);

		assertEquals(expectedDependencies.size(), config.dependencies().size());
		for (final var dependency : config.dependencies()) {
			assertTrue(expectedDependencies.containsKey(dependency.key));
			assertEquals(expectedDependencies.get(dependency.key), dependency.appConfigs.get(0).specificInstanceId);
		}
	}

	@Test
	void testRelayGroupValidationShowsErrorOnPhaseL3() throws Exception {
		final var relayFields = this.getRelayGroup().getAsJsonArray("fieldGroup");
		final var errorField = relayFields.asList().stream() //
				.map(JsonElement::getAsJsonObject) //
				.filter(field -> HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L3.name()
						.equals(field.get("key").getAsString())) //
				.findFirst().orElseThrow();
		assertTrue(errorField.getAsJsonObject("validation").get("show").getAsBoolean());
		final var validators = this.getRelayGroupValidators();
		for (final var property : OUTPUT_CHANNEL_PROPERTIES) {
			final var validator = validators.getAsJsonObject(property.name() + "_VALIDATION");
			assertEquals(HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L3.name(),
					validator.get("errorPath").getAsString());
		}
	}

	@Test
	void testNullChecksTreatNullMissingAndEmptyValuesAsUnset() {
		final var property = HeatingElement.Property.ALIAS;
		final var isNullExpression = Exp.currentModelValue(property).isNull().expression();
		final var notNullExpression = Exp.currentModelValue(property).notNull().expression();
		final var nullModel = new JsonObject();
		nullModel.add(property.name(), JsonNull.INSTANCE);
		final var missingModel = new JsonObject();
		final var emptyModel = JsonUtils.buildJsonObject() //
				.addProperty(property.name(), "") //
				.build();

		for (final var model : List.of(nullModel, missingModel, emptyModel)) {
			assertTrue(ExpressionEvaluator.evaluateExpression(isNullExpression, model));
			assertFalse(ExpressionEvaluator.evaluateExpression(notNullExpression, model));
		}
	}

	private JsonObject getRelayGroupValidators() throws Exception {
		return this.getRelayGroup().getAsJsonObject("validators");
	}

	private JsonObject getRelayGroup() {
		return this.getRelayGroup(DUMMY_ADMIN);
	}

	private JsonObject getRelayGroup(User user) {
		final var appAssistant = this.heatingElement.getAppAssistant(user);
		return appAssistant.fields.asList().stream() //
				.map(JsonElement::getAsJsonObject) //
				.filter(field -> field.has("fieldGroup") && field.getAsJsonArray("fieldGroup").size() > 0) //
				.map(field -> field.getAsJsonArray("fieldGroup").get(0).getAsJsonObject()) //
				.filter(field -> field.has("validators")) //
				.filter(field -> field.getAsJsonObject("validators")
						.has(OUTPUT_CHANNEL_PROPERTIES.get(0).name() + "_VALIDATION")) //
				.findFirst().orElseThrow();
	}

	private JsonObject getHeatingWithInternMeterJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("OUTPUT_CHANNEL_PHASE_L1", "io0/InputOutput0") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L2", "io0/InputOutput1") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L3", "io0/InputOutput2") //
				.addProperty("POWER_PER_PHASE", 2000) //
				.addProperty("HYSTERESIS", 60) //
				.addProperty("IS_ELEMENT_MEASURED", true) //
				.addProperty("HOW_MEASURED", "INTERN") //
				.build();
	}

	private JsonObject getHeatingWithoutMeterJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("OUTPUT_CHANNEL_PHASE_L1", "io0/InputOutput0") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L2", "io0/InputOutput1") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L3", "io0/InputOutput2") //
				.addProperty("POWER_PER_PHASE", 2000) //
				.addProperty("HYSTERESIS", 60) //
				.addProperty("IS_ELEMENT_MEASURED", false) //
				.build();
	}

	private JsonObject getHeatingWithTwoHeatingElementsJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("OUTPUT_CHANNEL_PHASE_L1", "io0/InputOutput0") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L2", "io0/InputOutput1") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L3", "io0/InputOutput2") //
				.addProperty("POWER_PER_PHASE", 2000) //
				.addProperty("HYSTERESIS", 60) //
				.addProperty("IS_ELEMENT_MEASURED", false) //
				.addProperty("NUMBER_OF_HEATING_ELEMENTS", "TWO") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L1_HEATING_ELEMENT_B", "io0/InputOutput3") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L2_HEATING_ELEMENT_B", "io0/InputOutput4") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L3_HEATING_ELEMENT_B", "io0/InputOutput5") //
				.addProperty("POWER_PER_PHASE_HEATING_ELEMENT_B", 1000) //
				.addProperty("DISTRIBUTION", "PARALLEL") //
				.build();
	}

	@ParameterizedTest
	@MethodSource("relayAssistantCases")
	void testGetAppAssistantWithRelayDefaults(RelayAssistantCase testCase) throws Exception {
		final var componentManagerFactory = new AppManagerTestBundle.DefaultComponentManagerFactory();
		final var apps = new HeatingElement[1];
		final var appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(apps[0] = Apps.heatingElement(t));
		}, null, componentManagerFactory);
		componentManagerFactory.getComponentManager()
				.addComponent(new LimitedChannelRelay("io0", testCase.numberOfChannels()));

		final var appAssistant = assertDoesNotThrow(() -> apps[0].getAppAssistant(DUMMY_ADMIN));

		final var fieldsByKey = new HashMap<String, JsonObject>();
		collectFieldsByKey(appAssistant.fields, fieldsByKey);

		for (var i = 0; i < OUTPUT_CHANNEL_PROPERTIES.size(); i++) {
			final var property = OUTPUT_CHANNEL_PROPERTIES.get(i);
			final var field = fieldsByKey.get(property.name());
			assertNotNull(field, "missing field " + property.name());
			appManagerTestBundle.withApp(apps[0]) //
					.assertThatProperty(property) //
					.hasDefaultValue(testCase.expectedDefaults()[i]);
		}

		for (var i = HEATING_ELEMENT_B_FIRST_PROPERTY_INDEX; i < OUTPUT_CHANNEL_PROPERTIES.size(); i++) {
			final var property = OUTPUT_CHANNEL_PROPERTIES.get(i);
			final var hideExpression = getHideExpression(fieldsByKey.get(property.name()));
			assertNotNull(hideExpression, property.name() + " should be conditionally hidden");
			assertVisibility(hideExpression, NumberOfHeatingElements.ONE, false);
			assertVisibility(hideExpression, NumberOfHeatingElements.TWO, true);

			final var outerWrapper = findOuterWrapper(appAssistant.fields, property.name());
			assertNotNull(outerWrapper, property.name() + " should have an outer safe-input wrapper");
			final var outerHideExpression = getHideExpression(outerWrapper);
			assertNotNull(outerHideExpression, property.name() + " outer wrapper should be conditionally hidden");
			assertVisibility(outerHideExpression, NumberOfHeatingElements.ONE, false);
			assertVisibility(outerHideExpression, NumberOfHeatingElements.TWO, true);
		}
	}

	private static Stream<RelayAssistantCase> relayAssistantCases() {
		return Stream.of(//
				new RelayAssistantCase(4,
						new String[] { "io0/InputOutput0", "io0/InputOutput1", "io0/InputOutput2", null, null, null }),
				new RelayAssistantCase(8, new String[] { "io0/InputOutput3", "io0/InputOutput4", "io0/InputOutput5",
						"io0/InputOutput0", "io0/InputOutput1", "io0/InputOutput2" }) //
		);
	}

	private static String getHideExpression(JsonObject field) {
		final var expressionProperties = field.getAsJsonObject("expressionProperties");
		if (expressionProperties == null || !expressionProperties.has("hide")) {
			return null;
		}
		return expressionProperties.get("hide").getAsString();
	}

	private static JsonObject findOuterWrapper(JsonElement element, String key) {
		if (element.isJsonObject()) {
			final var object = element.getAsJsonObject();
			if (object.has("key") && key.equals(object.get("key").getAsString()) && object.has("wrappers")) {
				for (var wrapper : object.getAsJsonArray("wrappers")) {
					if (Wrappers.SAFE_INPUT.getWrapperClass().equals(wrapper.getAsString())) {
						return object;
					}
				}
			}
			for (var entry : object.entrySet()) {
				final var result = findOuterWrapper(entry.getValue(), key);
				if (result != null) {
					return result;
				}
			}
		} else if (element.isJsonArray()) {
			for (var child : element.getAsJsonArray()) {
				final var result = findOuterWrapper(child, key);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	private static void assertVisibility(String hideExpression, NumberOfHeatingElements numberOfHeatingElements,
			boolean expectedVisible) {
		final var model = JsonUtils.buildJsonObject() //
				.addProperty(HeatingElement.Property.NUMBER_OF_HEATING_ELEMENTS.name(), numberOfHeatingElements.name()) //
				.build();
		final var hidden = ExpressionEvaluator.evaluateExpression(hideExpression, model);
		if (expectedVisible) {
			assertFalse(hidden, "field should be visible for " + numberOfHeatingElements);
		} else {
			assertTrue(hidden, "field should be hidden for " + numberOfHeatingElements);
		}
	}

	/**
	 * Recursively collects every {@link JsonObject} with a "key" property from the
	 * given {@link JsonElement}, keyed by that property name.
	 *
	 * @param element the {@link JsonElement} to search
	 * @param result  the map to collect the found fields into
	 */
	private static void collectFieldsByKey(JsonElement element, Map<String, JsonObject> result) {
		if (element.isJsonObject()) {
			final var object = element.getAsJsonObject();
			if (object.has("key") && object.get("key").isJsonPrimitive()) {
				result.put(object.get("key").getAsString(), object);
			}
			for (var entry : object.entrySet()) {
				collectFieldsByKey(entry.getValue(), result);
			}
		} else if (element.isJsonArray()) {
			for (var child : element.getAsJsonArray()) {
				collectFieldsByKey(child, result);
			}
		}
	}

	/**
	 * A {@link DummyInputOutput} with a configurable, reduced number of relay
	 * output channels, used to simulate realistic 4- or 8-channel relay boards.
	 */
	private static final class LimitedChannelRelay extends DummyInputOutput {

		private final BooleanWriteChannel[] limitedOutputChannels;

		LimitedChannelRelay(String id, int numberOfChannels) {
			super(id);
			this.limitedOutputChannels = Arrays.copyOf(super.digitalOutputChannels(), numberOfChannels);
		}

		@Override
		public BooleanWriteChannel[] digitalOutputChannels() {
			return this.limitedOutputChannels;
		}
	}
}
