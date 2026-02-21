package io.openems.edge.ess.core.power.v2;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.PowerDistributionHandler;
import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

/**
 * Handles Power-Distribution using Charge/Discharge Split without a linear
 * equation system.
 */
public class PowerDistributionHandlerV2 implements PowerDistributionHandler {

	private final Supplier<List<ManagedSymmetricEss>> esssSupplier;
	private final Coefficients coefficients = new Coefficients();

	private PowerDistribution pd;

	public PowerDistributionHandlerV2(Supplier<List<ManagedSymmetricEss>> esssSupplier) {
		this.esssSupplier = esssSupplier;
		this.onUpdateEsss(); // Initialize
	}

	@Override
	public void onUpdateEsss() {
		// Re-Initialize Coefficients
		final var esss = this.esssSupplier.get();
		this.coefficients.initialize(false /* symmetricMode */, //
				esss.stream() //
						.map(OpenemsComponent::id) //
						.collect(toSet()));
	}

	@Override
	public void onAfterProcessImage() {
		final var esss = this.esssSupplier.get();
		this.pd = PowerDistribution.from(esss);
		// this.log.info("{}", this.pd);
	}

	@Override
	public void onBeforeWriteEvent() {
		if (this.pd == null) {
			return;
		}

		// Solve
		this.pd.solve();

		// Apply Power
		final var esss = this.esssSupplier.get();
		this.pd.applyToEsss(esss);

	}

	@Override
	public void onAfterWriteEvent() {
		// ignore, this.pd is recreated in onAfterProcessImage() at start of next cycle
	}

	@Override
	public void addConstraint(Constraint constraint) {
		if (constraint.getCoefficients().length != 1) {
			throw new IllegalArgumentException("Constraints with more than one Coefficient are not allowed here");
		}
		final var lc = constraint.getCoefficients()[0];
		if (lc.getValue() != 1.) {
			throw new IllegalArgumentException("Coefficients with value != 1 are not allowed here");
		}
		final var c = lc.getCoefficient();
		if (constraint.getValue().isEmpty()) {
			throw new IllegalArgumentException("Undefined Constraint value is not allowed here");
		}
		final var essId = c.getEssId();
		final var value = constraint.getValue().get().intValue();

		switch (c.getPwr()) {
		case ACTIVE -> {
			switch (constraint.getRelationship()) {
			case EQUALS //
				-> this.pd.setEquals(essId, value);
			case LESS_OR_EQUALS //
				-> this.pd.setLessOrEquals(essId, value);
			case GREATER_OR_EQUALS //
				-> this.pd.setGreaterOrEquals(essId, value);
			}
		}
		case REACTIVE -> {
			switch (constraint.getRelationship()) {
			case EQUALS //
				-> this.pd.setReactiveEquals(essId, value);
			case LESS_OR_EQUALS //
				-> this.pd.setReactiveLessOrEquals(essId, value);
			case GREATER_OR_EQUALS //
				-> this.pd.setReactiveGreaterOrEquals(essId, value);
			}
		}
		}
	}

	@Override
	public void addConstraintAndValidate(Constraint constraint) throws OpenemsException {
		// TODO validate if required
		this.addConstraint(constraint);
	}

	@Override
	public Coefficient getCoefficient(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr)
			throws OpenemsException {
		return this.coefficients.of(ess.id(), phase, pwr);
	}

	@Override
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, SingleOrAllPhase phase,
			Pwr pwr, Relationship relationship, double value) throws OpenemsException {
		return ConstraintUtil.createSimpleConstraint(this.coefficients, //
				description, ess.id(), phase, pwr, relationship, value);
	}

	@Override
	public void removeConstraint(Constraint constraint) {
		// TODO required?
		// this.constraints.remove(constraint);
	}

	@Override
	public int getPowerExtrema(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr, GoalType goal) {
		if (pwr == Pwr.ACTIVE) {
			if (goal == GoalType.MAXIMIZE) {
				return ess.getAllowedDischargePower().orElse(0);
			}
			return ess.getAllowedChargePower().orElse(0);
		}
		// Reactive power range is symmetric: [-S, +S]
		if (goal == GoalType.MAXIMIZE) {
			return ess.getMaxApparentPower().orElse(0);
		}
		return -ess.getMaxApparentPower().orElse(0);
	}
}