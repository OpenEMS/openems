package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Goal;
import io.openems.edge.ess.power.api.Relationship;

public class ChocoPowerWorker {

	private final static int TIMELIMIT = 500;

	private final Logger log = LoggerFactory.getLogger(ChocoPowerWorker.class);

	private final ChocoPower parent;

	public ChocoPowerWorker(ChocoPower parent) {
		this.parent = parent;
	}

	private Model initializeModel() {
		Model model = new Model();

		// initialize every EssWrapper
		for (EssWrapper ess : this.parent.esss.values()) {
			ess.initialize(model);
		}

		return model;
	}

	public synchronized int getActivePowerExtrema(Goal goal) {
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
		solver.limitTime(TIMELIMIT);
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

		// initialize solver
		final Model model = this.initializeModel();
		final Solver solver = model.getSolver();
		if (ChocoPower.DEBUG) {
			solver.showDecisions();
		}

		// Post all constraints
		this.getConstraintsForChocoSolver(model) //
				.forEach(c -> c.post());

		// initial distribution
		// TODO make this smarter, e.g. taking AllowedCharge/Discharge in
		// consideration when distributing. This is crucial for fast results.
		Map<IntVar, Integer> targets = new HashMap<>();
		int pEqualsTarget = 0;
		Optional<io.openems.edge.ess.power.api.Constraint> eqConstraint = this.parent.getAllConstraints() //
				.filter(c -> c.getRelationship() == Relationship.EQUALS) //
				.findFirst(); //
		if (eqConstraint.isPresent()) {
			pEqualsTarget = eqConstraint.get().getValue().orElse(0);
		}
		int lastP = 0;
		for (EssWrapper ess : this.parent.esss.values()) {
			lastP += ess.getLastP();
		}
		int avgPdelta = (pEqualsTarget - lastP) / this.parent.esss.size();
		for (EssWrapper ess : this.parent.esss.values()) {
			if (ess.getEss() instanceof ManagedAsymmetricEss) {
				int avgPLDelta = avgPdelta / 3;
				targets.put(ess.getP_L1(), ess.getLastP_L1() + avgPLDelta);
				targets.put(ess.getP_L2(), ess.getLastP_L2() + avgPLDelta);
				targets.put(ess.getP_L3(), ess.getLastP_L3() + avgPLDelta);
			} else {
				targets.put(ess.getP(), ess.getLastP() + avgPdelta);
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
		List<IntVar> pVars = getImportantPVars();
		solver.limitTime(TIMELIMIT);
		solver.setSearch(Search.intVarSearch(
				// variable selector
				new MaxRegret(),
				// value selector
				new IntDomainTarget(targets),
				// remove value on branch, no split
				DecisionOperatorFactory.makeIntEq(),
				// variables to branch on
				pVars.toArray(new IntVar[pVars.size()])));

		Solution solution = solver.findOptimalSolution(objective, Model.MINIMIZE);
		return solution;
	}

	private Stream<ReExpression> getConstraintsForChocoSolver(Model model) {
		return this.parent.getAllConstraints() //
				.filter(c -> c.getValue().isPresent()) //
				.map(c -> {
					try {
						ArExpression allExp = null;
						for (Coefficient co : c.getCoefficients()) {
							SymmetricEss ess = co.getEss();
							EssWrapper wrapper = this.parent.esss.get(ess);
							IntVar var = null;
							switch (co.getPwr()) {
							case ACTIVE:
								switch (co.getPhase()) {
								case ALL:
									var = wrapper.getP();
									break;
								case L1:
									var = wrapper.getP_L1();
									break;
								case L2:
									var = wrapper.getP_L2();
									break;
								case L3:
									var = wrapper.getP_L3();
									break;
								}
								break;
							case REACTIVE:
								break;
							}
							if (var == null) {
								throw new IllegalArgumentException("IntVar is null. This should never happen!");
							}
							int value = co.getValue();
							ArExpression exp;
							if (value == 1) {
								exp = var;
							} else {
								exp = var.mul(value);
							}
							if (allExp == null) {
								allExp = exp;
							} else {
								allExp = allExp.add(exp);
							}
						}

						// initialize error from minPrecision
						int minPrecision = Integer.MAX_VALUE;
						for (EssWrapper ess : this.parent.esss.values()) {
							minPrecision = Math.min(minPrecision, ess.getPrecision());
						}
						allExp.add(model.intVar("pError", 0, minPrecision - 1, false));
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

	private List<IntVar> getImportantPVars() {
		// Get IntVars from Ess
		List<IntVar> list = new ArrayList<>();
		for (EssWrapper ess : this.parent.esss.values()) {
			if (ess.getEss() instanceof ManagedAsymmetricEss) {
				list.add(ess.getP_L1());
				list.add(ess.getP_L2());
				list.add(ess.getP_L3());
			} else {
				list.add(ess.getP());
			}
		}
		return list;
	}

	private List<ArExpression> createDiffToLastOptimizer() {
		List<ArExpression> result = new ArrayList<>();
		for (EssWrapper ess : this.parent.esss.values()) {
			result.add(ess.createDiffToLastOptimizer());
		}
		return result;
	}

}
