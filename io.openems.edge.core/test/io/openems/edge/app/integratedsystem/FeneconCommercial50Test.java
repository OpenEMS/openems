package io.openems.edge.app.integratedsystem;

import static io.openems.edge.core.appmanager.validator.ExpressionEvaluator.evaluateExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial50Gen3;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

class FeneconCommercial50Test {

	private AppManagerTestBundle appManagerTestBundle;
	private FeneconCommercial50Gen3 feneconCommercial50;
	private static final int DEFAULT_EMERGENCY_RESERVE_SOC = 10;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return List.of(//
					this.feneconCommercial50 = Apps.feneconCommercial50Gen3(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.prepareBatteryExtension(t), //
					Apps.sohCycle(t), //
					Apps.predictionDefault(t), //
					Apps.predictionUnmanagedConsumption(t) //
			);
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
		this.appManagerTestBundle.addPredictorManagerByCentralOrderAggregateTask();
	}

	@Test
	void testFullSettings() throws Exception {
		this.createFullCommercial50();
		assertEquals(7, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	@Test
	void testMaxHeatPowerDefaultValue() {
		final var def = FeneconCommercial50Gen3.Property.EMERGENCY_RESERVE_SOC.def();
		final var parameter = new Type.Parameter.BundleParameter(
				AbstractOpenemsApp.getTranslationBundle(Language.DEFAULT));

		final var defaultValue = def.getDefaultValue().get(this.feneconCommercial50,
				FeneconCommercial50Gen3.Property.EMERGENCY_RESERVE_SOC, Language.DEFAULT, parameter);
		assertEquals(DEFAULT_EMERGENCY_RESERVE_SOC, defaultValue.getAsInt());
	}

	@Test
	void testGensetChargeSocValidationWithInvalidValues() {
		final var expressionString = this.getExpressionStringForSocGroupRange();

		assertFalse(evaluateExpression(expressionString, JsonUtils.buildJsonObject() //
				.addProperty("GENSET_ENABLE_CHARGE", true) //
				.addProperty("GENSET_CHARGE_SOC_START", 20) //
				.addProperty("GENSET_CHARGE_SOC_END", 15) //
				.build() //
		));
	}

	@Test
	void testGensetChargeSocValidationWithValidValues() {
		final var expressionString = this.getExpressionStringForSocGroupRange();

		assertTrue(evaluateExpression(expressionString, JsonUtils.buildJsonObject() //
				.addProperty("GENSET_ENABLE_CHARGE", true) //
				.addProperty("GENSET_CHARGE_SOC_START", 20) //
				.addProperty("GENSET_CHARGE_SOC_END", 50) //
				.build() //
		));
	}

	@Test
	void testEmergencyReserveSocValidationWithInvalidValues() {
		final var expressionString = this.getExpressionStringForEmergencyReserve();

		assertFalse(evaluateExpression(expressionString, JsonUtils.buildJsonObject() //
				.addProperty("EMERGENCY_RESERVE_SOC", 15) //
				.addProperty("IS_GENSET_INSTALLED", true) //
				.addProperty("GENSET_ENABLE_CHARGE", true) //
				.addProperty("GENSET_CHARGE_SOC_START", 20) //
				.build() //
		));
	}

	@Test
	void testEmergencyReserveSocValidationWithValidValues() {
		final var expressionString = this.getExpressionStringForEmergencyReserve();

		assertTrue(evaluateExpression(expressionString, JsonUtils.buildJsonObject() //
				.addProperty("EMERGENCY_RESERVE_SOC", 25) //
				.addProperty("IS_GENSET_INSTALLED", true) //
				.addProperty("GENSET_ENABLE_CHARGE", true) //
				.addProperty("GENSET_CHARGE_SOC_START", 20) //
				.build() //
		));
	}

	@Test
	void testEmergencyReserveSocValidationWithNullGensetSocStart() {
		final var expressionString = this.getExpressionStringForEmergencyReserve();

		assertTrue(evaluateExpression(expressionString, JsonUtils.buildJsonObject() //
				.addProperty("EMERGENCY_RESERVE_SOC", 15) //
				.addProperty("IS_GENSET_INSTALLED", true) //
				.addProperty("GENSET_ENABLE_CHARGE", false) //
				.build() //
		));
	}

	private String getExpressionStringForEmergencyReserve() {
		final var def = FeneconCommercial50Gen3.Property.EMERGENCY_RESERVE_SOC.def();
		final var parameter = new Type.Parameter.BundleParameter(
				AbstractOpenemsApp.getTranslationBundle(Language.DEFAULT));

		final var fieldBuilder = def.getField().get(this.feneconCommercial50,
				FeneconCommercial50Gen3.Property.EMERGENCY_RESERVE_SOC, Language.DEFAULT, parameter);
		final var fieldJson = fieldBuilder.build().getAsJsonObject();

		return this.getExpressionStringFromValidatorName(fieldJson, "reserveEnergyValidation");
	}

	private String getExpressionStringForSocGroupRange() {
		final var groupDef = FeneconCommercial50Gen3.Property.GENSET_CHARGE_SOC_GROUP.def();
		final var parameter = new Type.Parameter.BundleParameter(
				AbstractOpenemsApp.getTranslationBundle(Language.DEFAULT));

		final var fieldBuilder = groupDef.getField().get(this.feneconCommercial50,
				FeneconCommercial50Gen3.Property.GENSET_CHARGE_SOC_GROUP, Language.DEFAULT, parameter);
		final var fieldJson = fieldBuilder.build().getAsJsonObject();

		final var fieldGroup = fieldJson.get("fieldGroup").getAsJsonArray();
		return this.getExpressionStringFromValidatorName(fieldGroup.get(0).getAsJsonObject(),
				"gensetChargeSocValidation");
	}

	private String getExpressionStringFromValidatorName(JsonObject jsonObject, String validatorName) {
		final var validators = jsonObject.get("validators").getAsJsonObject();
		assertNotNull(validators);
		assertTrue(validators.has(validatorName));

		final var validation = validators.getAsJsonObject(validatorName);

		return validation.get("expressionString").getAsString();

	}

	private void createFullCommercial50() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DummyUser.DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Commercial.50.Gen3", "key", "alias", fullSettings()));
	}

	private static JsonObject fullSettings() {
		return JsonUtils.buildJsonObject().addProperty("SAFETY_COUNTRY", "GERMANY") //
				.addProperty("GRID_CODE", "VDE_4105") //
				.addProperty("FEED_IN_TYPE", "DYNAMIC_LIMITATION") //
				.addProperty("MAX_FEED_IN_POWER", 1000) //
				.addProperty("NA_PROTECTION_ENABLED", false) //
				.addProperty("CT_RATIO_FIRST", 500) //
				.addProperty("HAS_ESS_LIMITER_14A", false) //
				.addProperty("HAS_EMERGENCY_RESERVE", true) //
				.addProperty("EMERGENCY_RESERVE_ENABLED", true) //
				.addProperty("EMERGENCY_RESERVE_SOC", 25) //
				.addProperty("IS_GENSET_INSTALLED", true) //
				.addProperty("GENSET_ID", "meter1") //
				.addProperty("GENSET_RATED_POWER", 10000) //
				.addProperty("GENSET_PREHEATING_TIME", 30) //
				.addProperty("GENSET_RUN_TIME", 180) //
				.addProperty("GENSET_ENABLE_CHARGE", true) //
				.addProperty("GENSET_MAX_POWER", 50) //
				.addProperty("GENSET_CHARGE_SOC_START", 20) //
				.addProperty("GENSET_CHARGE_SOC_END", 50) //
				.addProperty("SHADOW_MANAGEMENT_DISABLED", false) //
				.build();
	}
}
