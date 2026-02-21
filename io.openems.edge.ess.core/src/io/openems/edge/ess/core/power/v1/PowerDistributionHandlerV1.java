package io.openems.edge.ess.core.power.v1;

import static io.openems.edge.ess.core.power.data.LogUtil.debugLogConstraints;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.BooleanConsumer;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.PowerDistributionHandler;
import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.core.power.solver.CalculatePowerExtrema;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.power.api.SolverStrategy;

/**
 * Handles Power-Distribution using a Linear Equation System.
 */
public class PowerDistributionHandlerV1 implements PowerDistributionHandler {

	private final Logger log = LoggerFactory.getLogger(PowerDistributionHandlerV1.class);
	private final SolverStrategy solverStrategy;
	private final boolean debugMode;
	private final Data data;
	private final Solver solver;

	public PowerDistributionHandlerV1(SolverStrategy solverStrategy, boolean symmetricMode, boolean debugMode,
			Supplier<List<ManagedSymmetricEss>> esssSupplier, Consumer<Boolean> onStaticConstraintsFailed,
			BooleanConsumer onSetNotSolved, IntConsumer onSetSolveDuration,
			Consumer<SolverStrategy> onSetSolveStrategy) {
		this.solverStrategy = solverStrategy;
		this.debugMode = debugMode;

		this.data = new Data(esssSupplier);
		this.data.onStaticConstraintsFailed(onStaticConstraintsFailed);
		this.data.setSymmetricMode(symmetricMode);

		this.solver = new Solver(esssSupplier, this.data, debugMode);
		this.solver.onSolved((isSolved, duration, strategy) -> {
			onSetNotSolved.accept(!isSolved);
			onSetSolveDuration.accept(duration);
			onSetSolveStrategy.accept(strategy);
		});
	}

	@Override
	public void onUpdateEsss() {
		this.data.updateInverters();
	}

	@Override
	public void onAfterProcessImage() {
		// ignore
	}

	@Override
	public void onBeforeWriteEvent() {
		this.solver.solve(this.solverStrategy);
	}

	@Override
	public void onAfterWriteEvent() {
		this.data.initializeCycle();
	}

	@Override
	public void addConstraint(Constraint constraint) {
		this.data.addConstraint(constraint);
	}

	@Override
	public void addConstraintAndValidate(Constraint constraint) throws OpenemsException {
		this.data.addConstraint(constraint);
		try {
			this.solver.isSolvableOrError();
		} catch (OpenemsException e) {
			this.data.removeConstraint(constraint);
			if (this.debugMode) {
				var allConstraints = this.data.getConstraintsForAllInverters();
				debugLogConstraints(this.log, "Unable to validate with following constraints:", allConstraints);
				this.log.warn("Failed to add Constraint: " + constraint);
			}
			if (e instanceof PowerException pe) {
				pe.setReason(constraint);
			}
			throw e;
		}
	}

	@Override
	public Coefficient getCoefficient(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr)
			throws OpenemsException {
		return this.data.getCoefficient(ess.id(), phase, pwr);
	}

	@Override
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, SingleOrAllPhase phase,
			Pwr pwr, Relationship relationship, double value) throws OpenemsException {
		return ConstraintUtil.createSimpleConstraint(this.data.getCoefficients(), //
				description, ess.id(), phase, pwr, relationship, value);
	}

	@Override
	public void removeConstraint(Constraint constraint) {
		this.data.removeConstraint(constraint);
	}

	@Override
	public int getPowerExtrema(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr, GoalType goal) {
		final List<Constraint> allConstraints;
		try {
			allConstraints = this.data.getConstraintsForAllInverters();
		} catch (OpenemsException e) {
			this.log.error("Unable to get Constraints " + e.getMessage());
			return 0;
		}
		var power = CalculatePowerExtrema.from(this.data.getCoefficients(), allConstraints, ess.id(), phase, pwr, goal);
		if (power <= Integer.MIN_VALUE || power >= Integer.MAX_VALUE) {
			this.log.error(goal.name() + " Power for [" + ess.toString() + "," + phase.toString() + "," + pwr.toString()
					+ "=" + power + "] is out of bounds. Returning '0'");
			return 0;
		}
		if (goal == GoalType.MAXIMIZE) {
			return (int) Math.floor(power);
		}
		return (int) Math.ceil(power);
	}

}
