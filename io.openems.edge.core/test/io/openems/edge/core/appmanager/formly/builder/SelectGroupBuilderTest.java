package io.openems.edge.core.appmanager.formly.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.Exp;

class SelectGroupBuilderTest {

	@Test
	void hideExpressionIsAppliedToOuterField() {
		final var property = Nameable.of("property");
		final var json = new SelectGroupBuilder(property)
				.onlyShowIfWithoutRequired(Exp.currentModelValue(Nameable.of("show")).notNull()).build();
		final var hideExpression = json.getAsJsonObject("expressionProperties").get("hide");
		final var innerHideExpression = json.getAsJsonObject("templateOptions").getAsJsonArray("fields").get(0)
				.getAsJsonObject().getAsJsonObject("expressionProperties").get("hide");

		assertEquals(innerHideExpression, hideExpression);
	}

}
