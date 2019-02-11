package io.openems.edge.ess.test;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class DummyPower implements Power {

	private final int maxApparentPower;

	public DummyPower(int maxApparentPower) {
		this.maxApparentPower = maxApparentPower;
	}

	@Override
	public Constraint addConstraint(Constraint constraint) {
		return null;
	}

	@Override
	public Constraint addConstraintAndValidate(Constraint constraint) throws PowerException {
		return null;
	}

	@Override
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			Relationship relationship, double value) {
		return null;
	}

	@Override
	public void removeConstraint(Constraint constraint) {

	}

	@Override
	public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		return this.maxApparentPower;
	}

	@Override
	public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		return this.maxApparentPower * -1;
	}

	@Override
	public Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		return null;
	}

}
