package io.openems.edge.core.appmanager.validator;

import static io.openems.edge.core.appmanager.validator.ExpressionEvaluator.evaluateExpression;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

class ExpressionEvaluatorTest {

	@Test
	void testParenthesizedEquality() {
		assertTrue(evaluateExpression("(model.INTEGRATION_TYPE == 'TCP')", modelWithIntegrationType("TCP")));
	}

	@Test
	void testNegatedEqualityWhenComparisonMatches() {
		assertFalse(evaluateExpression("!(model.INTEGRATION_TYPE == 'TCP')", modelWithIntegrationType("TCP")));
	}

	@Test
	void testNegatedEqualityWhenComparisonDoesNotMatch() {
		assertTrue(evaluateExpression("!(model.INTEGRATION_TYPE == 'TCP')", modelWithIntegrationType("RTU")));
	}

	@Test
	void testDoubleNegation() {
		final var expression = "!!(model.INTEGRATION_TYPE == 'TCP')";

		assertTrue(evaluateExpression(expression, modelWithIntegrationType("TCP")));
		assertFalse(evaluateExpression(expression, modelWithIntegrationType("RTU")));
	}

	private static JsonObject modelWithIntegrationType(String integrationType) {
		return JsonUtils.buildJsonObject() //
				.addProperty("INTEGRATION_TYPE", integrationType) //
				.build();
	}
}
