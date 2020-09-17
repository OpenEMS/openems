package io.openems.edge.ess.core.power.data;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class ConstraintUtil {

	private ConstraintUtil() {
	}

	/**
	 * Creates a simple Constraint with only one Coefficient.
	 * 
	 * @param description  a description for the Constraint
	 * @param essId        the component ID of Ess
	 * @param phase        the Phase
	 * @param pwr          the Pwr
	 * @param relationship the Relationshipt
	 * @param value        the value
	 * @return Constraints
	 * @throws OpenemsException
	 */
	public static Constraint createSimpleConstraint(Coefficients coefficients, String description, String essId,
			Phase phase, Pwr pwr, Relationship relationship, double value) throws OpenemsException {
		return new Constraint(description, //
				new LinearCoefficient[] { //
						new LinearCoefficient(coefficients.of(essId, phase, pwr), 1) //
				}, relationship, //
				value);
	}

}
