package io.openems.edge.app.prediction;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.openemshardware.BeagleBoneBlack;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

class AppWeatherPredictionTest {

	private AppManagerTestBundle appManagerTestBundle;
	private AppWeatherPrediction weatherPrediction;
	private BeagleBoneBlack beagleBoneBlack;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, //
				t -> List.of(//
						this.weatherPrediction = Apps.weatherPrediction(t), //
						this.beagleBoneBlack = Apps.beagleBoneBlack(t)),
				null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addPredictorManagerByCentralOrderAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
		this.appManagerTestBundle.addPersistencePredictorAggregateTask();
	}

	@Test
	void testModelComplexity_shouldBeHigh_withoutHardware() throws Exception {
		final var config = this.weatherPrediction.getAppConfiguration(ConfigurationTarget.ADD, emptyProperties(),
				Language.DEFAULT);
		assertEquals("HIGH", getPredictorModelComplexity(config));
	}

	@Test
	void testModelComplexity_shouldBeLow_withBeagleBoneHardware() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.beagleBoneBlack.getAppId(), "key", "alias", emptyProperties()));

		final var config = this.weatherPrediction.getAppConfiguration(ConfigurationTarget.ADD, emptyProperties(),
				Language.DEFAULT);
		assertEquals("LOW", getPredictorModelComplexity(config));
	}

	private static String getPredictorModelComplexity(io.openems.edge.core.appmanager.AppConfiguration config) {
		final var predictor = config.getComponents().stream() //
				.filter(component -> "Predictor.Production.LinearModel".equals(component.factoryId())) //
				.findFirst().orElse(null);
		assertNotNull(predictor);

		final var modelComplexity = predictor.properties().getOrNull("modelComplexity");
		assertNotNull(modelComplexity);
		return modelComplexity.value().getAsString();
	}

	private static JsonObject emptyProperties() {
		return JsonUtils.buildJsonObject().build();
	}
}
