package io.openems.edge.ess.core.power.data;

import static io.openems.edge.ess.core.power.data.ConstraintUtil.createSimpleConstraint;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class LinearSolverUtilTest {

	@Test(expected = OpenemsException.class)
	public void testCoefficientOfThrowsException() throws OpenemsException {
		createSimpleConstraint(new Coefficients(), //
				"Dummy#1", "ess0", Phase.ALL, Pwr.ACTIVE, EQUALS, 0);
	}

	@Test
	public void testConvertToLinearConstraints() throws OpenemsException {
		final var coefficients = new Coefficients();
		coefficients.initialize(false, Set.of("ess0"));
		var constraints = List.of(//
				createSimpleConstraint(coefficients, //
						"Dummy EQUALS", "ess0", Phase.ALL, Pwr.ACTIVE, EQUALS, 0), //
				createSimpleConstraint(coefficients, //
						"Dummy GREATER_OR_EQUALS", "ess0", Phase.ALL, Pwr.ACTIVE, GREATER_OR_EQUALS, 0), //
				createSimpleConstraint(coefficients, //
						"Dummy LESS_OR_EQUALS", "ess0", Phase.ALL, Pwr.ACTIVE, LESS_OR_EQUALS, 0), //
				new Constraint("Dummy empty value", new LinearCoefficient[0], Relationship.EQUALS));
		LinearSolverUtil.convertToLinearConstraints(coefficients, constraints);
	}

}
