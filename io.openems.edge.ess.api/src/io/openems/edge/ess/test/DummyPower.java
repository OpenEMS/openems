package io.openems.edge.ess.test;

import io.openems.edge.common.filter.PidFilter;
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
	private final double p;
	private final double i;
	private final double d;

	public DummyPower(int maxApparentPower) {
		this(maxApparentPower, PidFilter.DEFAULT_P, PidFilter.DEFAULT_I, PidFilter.DEFAULT_D);
	}

	public DummyPower(double p, double i, double d) {
		this(Integer.MAX_VALUE, p, i, d);
	}

	public DummyPower(int maxApparentPower, double p, double i, double d) {
		this.maxApparentPower = maxApparentPower;
		this.p = p;
		this.i = i;
		this.d = d;
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

	@Override
	public PidFilter buildPidFilter() {
		return new PidFilter(this.p, this.i, this.d);
	}

}
