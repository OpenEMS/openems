package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.expression.discrete.arithmetic.ArExpression;
import org.chocosolver.solver.expression.discrete.relational.ReExpression;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMax;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.selectors.variables.MaxRegret;
import org.chocosolver.solver.variables.IntVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Goal;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class ChocoPowerWorker {

	private final Logger log = LoggerFactory.getLogger(ChocoPowerWorker.class);

	private final ChocoPower parent;

	public ChocoPowerWorker(ChocoPower parent) {
		this.parent = parent;
	}

	private Model initializeModel() {
		Model model = new Model();

		// initialize every EssWrapper
		for (EssWrapper ess : this.parent.esss.values()) {
			ess.initialize(this.parent, model);
		}

		return model;
	}

	public synchronized int getActivePowerExtrema(Goal goal) {
		// No Ess, no solution!
		if (this.parent.esss.isEmpty()) {
			return 0;
		}

		// initialize solver
		final Model model = this.initializeModel();
		final Solver solver = model.getSolver();

		// Post all constraints
		this.getConstraintsForChocoSolver(model) //
				.forEach(c -> c.post());

		// create objective
		List<IntVar> pVars = getImportantPVars();
		ArExpression objectiveExp = null;
		for (IntVar pVar : pVars) {
			if (objectiveExp == null) {
				objectiveExp = pVar;
			} else {
				objectiveExp = objectiveExp.add(pVar);
			}
		}
		IntVar objective = objectiveExp.intVar();

		// get appropriate selector
		IntValueSelector selector;
		if (goal == Goal.MAXIMIZE) {
			selector = new IntDomainMax();
		} else {
			selector = new IntDomainMin();
		}

		// initialize solver
		solver.limitTime(this.parent.parent.getSolveDurationLimit());
		// TODO add smart stop Criterion that stops the search early if the solutions
		// are not anymore improving significantly
		solver.setSearch(Search.intVarSearch(
				// variable selector
				new MaxRegret(),
				// value selector
				selector,
				// remove value on branch, no split
				DecisionOperatorFactory.makeIntEq(),
				// variables to branch on
				pVars.toArray(new IntVar[pVars.size()])));

		Solution solution = solver.findOptimalSolution(objective, goal == Goal.MAXIMIZE);
		if (solution == null) {
			log.warn("Unable to " + goal.name() + " Active Power. Setting it to zero.");
			return 0;
		} else {
			return solution.getIntVal(objective);
		}
	}

	public Solution solve() {
		// No Ess, no solution!
		if (this.parent.esss.isEmpty()) {
			return null;
		}

		// measure duration
		long startTime = System.nanoTime();

		// initialize solver
		final Model model = this.initializeModel();
		final Solver solver = model.getSolver();

		// add Zero-Constraints
		this.addZeroConstraints();

		// Post all constraints
		this.getConstraintsForChocoSolver(model) //
				.forEach(c -> c.post());

		// initial distribution
		// TODO make this smarter, e.g. taking AllowedCharge/Discharge in
		// consideration when distributing. This is crucial for fast results.
		Map<IntVar, Integer> targets = new HashMap<>();
		final AtomicInteger pEqualsTarget = new AtomicInteger(0);
		final AtomicInteger qEqualsTarget = new AtomicInteger(0);
		this.parent.getAllConstraints() //
				.filter(c -> c.getRelationship() == Relationship.EQUALS && c.getValue().isPresent()) //
				.forEach(c -> {
					for (Coefficient co : c.getCoefficients()) {
						switch (co.getPwr()) {
						case ACTIVE:
							pEqualsTarget.set(c.getValue().get());
							break;
						case REACTIVE:
							qEqualsTarget.set(c.getValue().get());
							break;
						}
					}
				});
		int lastP = 0;
		int lastQ = 0;
		for (EssWrapper ess : this.parent.esss.values()) {
			lastP += ess.getLastP();
			lastQ += ess.getLastQ();
		}
		int avgPDelta = (pEqualsTarget.get() - lastP) / this.parent.esss.size();
		int avgQDelta = (qEqualsTarget.get() - lastQ) / this.parent.esss.size();
		for (EssWrapper ess : this.parent.esss.values()) {
			if (ess.getEss() instanceof ManagedAsymmetricEss && !this.parent.parent.isSymmetricMode()) {
				int avgPLDelta = avgPDelta / 3;
				targets.put(ess.getP_L1(), ess.getLastP_L1() + avgPLDelta);
				targets.put(ess.getP_L2(), ess.getLastP_L2() + avgPLDelta);
				targets.put(ess.getP_L3(), ess.getLastP_L3() + avgPLDelta);
				targets.put(ess.getP(), ess.getLastP() + avgPDelta);
				int avgQLDelta = avgQDelta / 3;
				targets.put(ess.getQ_L1(), ess.getLastQ_L1() + avgQLDelta);
				targets.put(ess.getQ_L2(), ess.getLastQ_L2() + avgQLDelta);
				targets.put(ess.getQ_L3(), ess.getLastQ_L3() + avgQLDelta);
				targets.put(ess.getQ(), ess.getLastQ() + avgQDelta);
			} else {
				targets.put(ess.getP(), ess.getLastP() + avgPDelta);
				targets.put(ess.getQ(), ess.getLastQ() + avgQDelta);
			}
		}

		// Create optimization objective
		// TODO allow definition of Cycle/Static Optimizers
		ArExpression objectiveExp = null;
		for (ArExpression e : this.createDiffToLastOptimizer()) {
			if (objectiveExp == null) {
				objectiveExp = e;
			} else {
				objectiveExp = objectiveExp.add(e);
			}
		}
		IntVar objective = objectiveExp.intVar();

		// initialize solver
		List<IntVar> vars = getImportantVars();
		solver.limitTime(this.parent.parent.getSolveDurationLimit());
		solver.setSearch(Search.intVarSearch(
				// variable selector
				new MaxRegret(),
				// value selector
				new IntDomainTarget(targets),
				// remove value on branch, no split
				DecisionOperatorFactory.makeIntEq(),
				// variables to branch on
				vars.toArray(new IntVar[vars.size()])));

		Solution solution = solver.findOptimalSolution(objective, Model.MINIMIZE);

		// store debug channels
		this.parent.parent.getSolvedChannel().setNextValue(solution != null);
		this.parent.parent.getSolveDurationChannel().setNextValue((System.nanoTime() - startTime) / 1_000_000);

		return solution;
	}

	private ArExpression getVar(SymmetricEss ess, Pwr pwr, Phase phase) {
		if (ess instanceof MetaEss) {
			// for MetaEss: call getVar recursively
			ArExpression sum = null;
			for (SymmetricEss e : ((MetaEss) ess).getEsss()) {
				ArExpression var = this.getVar(e, pwr, phase);
				sum = sum == null ? var : sum.add(var);
			}
			return sum;

		} else {
			EssWrapper wrapper = this.parent.esss.get(ess);
			switch (pwr) {
			case ACTIVE:
				switch (phase) {
				case ALL:
					return wrapper.getP();
				case L1:
					return wrapper.getP_L1();
				case L2:
					return wrapper.getP_L2();
				case L3:
					return wrapper.getP_L3();
				}
			case REACTIVE:
				switch (phase) {
				case ALL:
					return wrapper.getQ();
				case L1:
					return wrapper.getQ_L1();
				case L2:
					return wrapper.getQ_L2();
				case L3:
					return wrapper.getQ_L3();
				}
			}
		}
		throw new IllegalArgumentException("IntVar is null. This should never happen!");
	}

	private static boolean coefficientIsCoveredBy(Coefficient co, SymmetricEss ess, Pwr pwr, Phase phase) {
		if (ess instanceof MetaEss) {
			// for MetaEss: call coefficientIsCoveredBy recursively
			for (SymmetricEss subEss : ((MetaEss) ess).getEsss()) {
				if (ChocoPowerWorker.coefficientIsCoveredBy(co, subEss, pwr, phase)) {
					return true;
				}
			}
			return false;

		} else {
			if (Objects.equals(co.getEss(), ess) //
					&& co.getPwr() == pwr //
					&& (phase == Phase.ALL //
							|| co.getPhase() == phase //
					)) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Adds Zero-Constraint for every Coefficient that does not have an explicit
	 * EQUALS constraint.
	 * 
	 * i.e. if there is no existing Constraint with ess0/L1/ACTIVE a constraint is
	 * added so that ess0/L1/ACTIVE is required to be zero.
	 */
	private void addZeroConstraints() {
		// Get Constraints for each Var to EQUALS zero
		List<Coefficient> allVarsEqualZero = new ArrayList<>();
		for (ManagedSymmetricEss ess : this.parent.esss.keySet()) {
			for (Pwr pwr : Pwr.values()) {
				for (Phase phase : Phase.values()) {
					allVarsEqualZero.add(//
							new Coefficient(ess, phase, pwr, 1));
				}
			}
		}

		// remove coefficient from 'allVarsEqualZero' that matches this coefficient
		// this makes sure, that allVarsEqualZero sets all Coefficients to EQUALS ZERO
		// that do not have a specific Constraint set. Specific means that the
		// coefficient has a value (i.e. it is not Optional.empty) and one of the
		// following is true:
		// <ul>
		// <li>Any constraint with Relationship EQUALS
		// <li>Cycle constraint with any relationship
		// </ul>
		this.parent.getAllConstraints() //
				.filter(c -> c.getValue().isPresent() && (//
				c.getRelationship() == Relationship.EQUALS //
						|| c.getType() == ConstraintType.CYCLE //
				)) //
				.forEach(c -> {
					for (Coefficient coSet : c.getCoefficients()) {
						Iterator<Coefficient> coZeroIter = allVarsEqualZero.iterator();
						while (coZeroIter.hasNext()) {
							Coefficient coZero = coZeroIter.next();
							if (ChocoPowerWorker.coefficientIsCoveredBy(coZero, coSet.getEss(), coSet.getPwr(),
									coSet.getPhase())) {
								coZeroIter.remove();
							}
						}
					}
				});

		// Remove specific L1/L2/L3 coefficients if 'ALL' is existing
		Iterator<Coefficient> coL123Iter = allVarsEqualZero.iterator();
		while (coL123Iter.hasNext()) {
			Coefficient coL123 = coL123Iter.next();
			Phase phase = coL123.getPhase();
			if (phase == Phase.L1 || phase == Phase.L2 || phase == Phase.L3) {
				Iterator<Coefficient> coLPIter = allVarsEqualZero.iterator();
				while (coLPIter.hasNext()) {
					Coefficient coLP = coLPIter.next();
					if (Objects.equals(coLP.getEss(), coL123.getEss()) //
							&& coLP.getPhase() == Phase.ALL //
							&& coLP.getPwr() == coL123.getPwr()) { //
						coL123Iter.remove();
						break;
					}
				}
			}
		}

		// Add 'allVarsEqualZero' Constraints
		for (Coefficient co : allVarsEqualZero) {
			this.parent.addConstraint(new Constraint( //
					ConstraintType.CYCLE, //
					new Coefficient[] { co }, //
					Relationship.EQUALS, //
					0));
		}
	}

	private Stream<ReExpression> getConstraintsForChocoSolver(Model model) {
		// Get all Constraints that were applied (using addConstraint)
		return this.parent.getAllConstraints().filter(c -> c.getValue().isPresent()) //
				.map(c -> {
					try {
						// create 'allExp' from all coefficients
						ArExpression allExp = null;
						for (Coefficient co : c.getCoefficients()) {
							SymmetricEss ess = co.getEss();
							ArExpression var = this.getVar(ess, co.getPwr(), co.getPhase());
							int value = co.getValue();
							ArExpression exp = value == 1 ? var : var.mul(value);
							allExp = allExp == null ? exp : allExp.add(exp);
						}

						// add a rounding error for cases where the target value is not dividable by
						// precision
						// e.g. Ess has precision of 100 but target value is 2534.
						allExp = allExp.add(this.getErrorVar(model));

						int value = c.getValue().get(); // the stream was filtered before, so this call is save
						switch (c.getRelationship()) {
						case EQUALS:
							return allExp.eq(value);
						case GREATER_OR_EQUALS:
							return allExp.ge(value);
						case LESS_OR_EQUALS:
							return allExp.le(value);
						}
					} catch (NullPointerException e) {
						log.error("Unable to convert Constraint to Choco-Solver: " + e.getMessage());
						e.printStackTrace();
					}
					return null;
				}).filter(c -> c != null);
	}

	/**
	 * Creates an IntVar that represents a rounding error for cases where the target
	 * value is not dividable by precision e.g. Ess has precision of 100 but target
	 * value is 2534.
	 * 
	 * The error variable is positive or negative according to the current average
	 * state-of-charge, following this logic
	 * 
	 * <ul>
	 * <li>if SoC > 50 %: error is negative (more discharge/less charge)
	 * <li>if SoC < 50 %: error is positive (less discharge/more charge)
	 * </ul>
	 * 
	 * @param model
	 * @return
	 */
	private ArExpression getErrorVar(Model model) {
		// get minPrecision and average state-of-charge of all Ess
		int minPrecision = Integer.MAX_VALUE;
		int socSum = 0;
		for (EssWrapper wrapper : this.parent.esss.values()) {
			minPrecision = Math.min(minPrecision, wrapper.getPrecision());
			socSum += wrapper.getEss().getSoc().value().orElse(50);
		}
		if (socSum / this.parent.esss.values().size() > 49) {
			return model.intVar("error", (minPrecision - 1) * -1, 0, false);
		} else {
			return model.intVar("error", 0, minPrecision - 1, false);
		}
	}

	private List<IntVar> getImportantVars() {
		return Stream.concat(this.getImportantPVars().stream(), this.getImportantQVars().stream())
				.collect(Collectors.toList());
	}

	private List<IntVar> getImportantPVars() {
		// Get IntVars from Ess
		List<IntVar> list = new ArrayList<>();
		for (EssWrapper ess : this.parent.esss.values()) {
			if (ess.getEss() instanceof ManagedAsymmetricEss && !this.parent.parent.isSymmetricMode()) {
				list.add(ess.getP_L1());
				list.add(ess.getP_L2());
				list.add(ess.getP_L3());
				list.add(ess.getP());
			} else {
				list.add(ess.getP());
			}
		}
		return list;
	}

	private List<IntVar> getImportantQVars() {
		// Get IntVars from Ess
		List<IntVar> list = new ArrayList<>();
		for (EssWrapper ess : this.parent.esss.values()) {
			if (ess.getEss() instanceof ManagedAsymmetricEss && !this.parent.parent.isSymmetricMode()) {
				list.add(ess.getQ_L1());
				list.add(ess.getQ_L2());
				list.add(ess.getQ_L3());
				list.add(ess.getQ());
			} else {
				list.add(ess.getQ());
			}
		}
		return list;
	}

	private List<ArExpression> createDiffToLastOptimizer() {
		List<ArExpression> result = new ArrayList<>();
		for (EssWrapper ess : this.parent.esss.values()) {
			result.add(ess.optimizePDiffToLast());
			result.add(ess.optimizeQDiffToLast());
		}
		return result;
	}
}
