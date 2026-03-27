package io.openems.edge.ess.core.power.v2;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.function.Supplier;

import io.openems.common.function.BooleanConsumer;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.PowerDistributionHandler;
import io.openems.edge.ess.core.power.v1.data.ConstraintUtil;
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
	private final BooleanConsumer onSetNotSolved;
	private final Coefficients coefficients = new Coefficients();

	private PowerDistribution pd;

	public PowerDistributionHandlerV2(Supplier<List<ManagedSymmetricEss>> esssSupplier,
			BooleanConsumer onSetNotSolved) {
		this.esssSupplier = esssSupplier;
		this.onSetNotSolved = onSetNotSolved;
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
	}

	@Override
	public void onBeforeWriteEvent() {
		if (this.pd == null) {
			return;
		}

		// Solve
		this.pd.solve();
		this.onSetNotSolved.accept(false);

		// Apply Power
		final var esss = this.esssSupplier.get();
		this.pd.applyToEsss(esss);
	}

	@Override
	public void onAfterWriteEvent() {
		// ignore, this.pd is recreated in onAfterProcessImage() at start of next cycle
	}

	@Override
	public void addConstraint(Constraint rawConstraint) {
		if (!(rawConstraint instanceof Constraint.Simple constraint)) {
			throw new IllegalArgumentException("Only Simple Constraints are allowed here");
		}

		if (this.pd == null) {
			return;
		}
		final var pwr = constraint.coefficient.getCoefficient().getPwr();
		final var essId = constraint.coefficient.getCoefficient().getEssId();
		final var value = constraint.value;

		switch (pwr) {
		case ACTIVE -> {
			switch (constraint.relationship) {
			case EQUALS //
				-> this.pd.setEquals(essId, value);
			case LESS_OR_EQUALS //
				-> this.pd.setLessOrEquals(essId, value);
			case GREATER_OR_EQUALS //
				-> this.pd.setGreaterOrEquals(essId, value);
			}
		}
		case REACTIVE -> {
			switch (constraint.relationship) {
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
			Pwr pwr, Relationship relationship, int value) throws OpenemsException {
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
		if (this.pd == null) {
			return 0;
		}
		if (pwr == Pwr.ACTIVE) {
			return this.pd.getActivePowerExtrema(ess.id(), goal);
		}
		return this.pd.getReactivePowerExtrema(ess.id(), goal);
	}
}