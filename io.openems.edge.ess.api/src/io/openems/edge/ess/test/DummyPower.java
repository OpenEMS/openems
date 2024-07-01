package io.openems.edge.ess.test;

import java.util.ArrayList;
import java.util.List;

import io.openems.edge.common.filter.DisabledPidFilter;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class DummyPower implements Power {

	private final PidFilter pidFilter;
	private final List<ManagedSymmetricEss> esss = new ArrayList<>();

	private int maxApparentPower;

	/**
	 * Creates a {@link DummyPower} with unlimited MaxApparentPower and disabled PID
	 * filter.
	 */
	public DummyPower() {
		this(Integer.MAX_VALUE, new DisabledPidFilter());
	}

	/**
	 * Creates a {@link DummyPower} with given MaxApparentPower and disabled PID
	 * filter.
	 *
	 * @param maxApparentPower the MaxApparentPower
	 */
	public DummyPower(int maxApparentPower) {
		this(maxApparentPower, new DisabledPidFilter());
	}

	public DummyPower(int maxApparentPower, PidFilter pidFilter) {
		this.maxApparentPower = maxApparentPower;
		this.pidFilter = pidFilter;
	}

	/**
	 * Creates a {@link DummyPower} with unlimited MaxApparentPower and PID filter
	 * with the given parameters.
	 * 
	 * @param p the proportional gain
	 * @param i the integral gain
	 * @param d the derivative gain
	 */
	public DummyPower(double p, double i, double d) {
		this(Integer.MAX_VALUE, p, i, d);
	}

	/**
	 * Creates a {@link DummyPower} with given MaxApparentPower and PID filter with
	 * the given parameters.
	 * 
	 * @param maxApparentPower the MaxApparentPower
	 * @param p                the proportional gain
	 * @param i                the integral gain
	 * @param d                the derivative gain
	 */
	public DummyPower(int maxApparentPower, double p, double i, double d) {
		this(maxApparentPower, new PidFilter(p, i, d));
	}

	/**
	 * Registers a {@link ManagedSymmetricEss} with this {@link DummyPower}.
	 * 
	 * @param ess the {@link ManagedSymmetricEss}
	 */
	public void addEss(ManagedSymmetricEss ess) {
		this.esss.add(ess);
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

	public void setMaxApparentPower(int maxApparentPower) {
		this.maxApparentPower = maxApparentPower;
	}

	@Override
	public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		var result = this.maxApparentPower;
		for (ManagedSymmetricEss e : this.esss) {
			result = TypeUtils.min(result, e.getMaxApparentPower().get(), e.getAllowedDischargePower().get());
		}
		return result;
	}

	@Override
	public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		var result = this.maxApparentPower;
		for (ManagedSymmetricEss e : this.esss) {
			result = TypeUtils.min(result, e.getMaxApparentPower().get(),
					TypeUtils.multiply(e.getAllowedChargePower().get(), -1));
		}
		return result * -1;
	}

	@Override
	public Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		return null;
	}

	@Override
	public PidFilter getPidFilter() {
		return this.pidFilter;
	}

	@Override
	public boolean isPidEnabled() {
		if (this.pidFilter instanceof DisabledPidFilter) {
			return false;
		}

		return true;
	}

}
