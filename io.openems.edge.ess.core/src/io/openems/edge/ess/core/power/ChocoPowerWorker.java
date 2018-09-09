package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Goal;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
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
		int avgPdelta = (pEqualsTarget.get() - lastP) / this.parent.esss.size();
		int avgQdelta = (qEqualsTarget.get() - lastQ) / this.parent.esss.size();
		for (EssWrapper ess : this.parent.esss.values()) {
			if (ess.getEss() instanceof ManagedAsymmetricEss) {
				int avgPLDelta = avgPdelta / 3;
				targets.put(ess.getP_L1(), ess.getLastP_L1() + avgPLDelta);
				targets.put(ess.getP_L2(), ess.getLastP_L2() + avgPLDelta);
				targets.put(ess.getP_L3(), ess.getLastP_L3() + avgPLDelta);
				int avgQLDelta = avgQdelta / 3;
				targets.put(ess.getQ_L1(), ess.getLastQ_L1() + avgQLDelta);
				targets.put(ess.getQ_L2(), ess.getLastQ_L2() + avgQLDelta);
				targets.put(ess.getQ_L3(), ess.getLastQ_L3() + avgQLDelta);
			} else {
				targets.put(ess.getP(), ess.getLastP() + avgPdelta);
				targets.put(ess.getQ(), ess.getLastQ() + avgQdelta);
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
		solver.limitTime(TIMELIMIT);
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

	private Stream<ReExpression> getConstraintsForChocoSolver(Model model) {
		return this.parent.getAllConstraints() //
				.filter(c -> c.getValue().isPresent()) //
				.map(c -> {
					try {
						ArExpression allExp = null;
						for (Coefficient co : c.getCoefficients()) {
							SymmetricEss ess = co.getEss();
							ArExpression var = this.getVar(ess, co.getPwr(), co.getPhase());
							int value = co.getValue();
							ArExpression exp = value == 1 ? var : var.mul(value);
							allExp = allExp == null ? exp : allExp.add(exp);
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

	private List<IntVar> getImportantVars() {
		return Stream.concat(this.getImportantPVars().stream(), this.getImportantQVars().stream())
				.collect(Collectors.toList());
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

	private List<IntVar> getImportantQVars() {
		// Get IntVars from Ess
		List<IntVar> list = new ArrayList<>();
		for (EssWrapper ess : this.parent.esss.values()) {
			if (ess.getEss() instanceof ManagedAsymmetricEss) {
				list.add(ess.getQ_L1());
				list.add(ess.getQ_L2());
				list.add(ess.getQ_L3());
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
