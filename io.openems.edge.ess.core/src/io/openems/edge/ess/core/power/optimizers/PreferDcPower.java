package io.openems.edge.ess.core.power.optimizers;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.core.power.data.ConstraintUtil.createSimpleConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class PreferDcPower {

	/**
	 * Tries to distribute power by preferring inverter DC power.
	 *
	 * @param coefficients   the {@link Coefficients}
	 * @param esss           the {@link ManagedSymmetricEss}
	 * @param allInverters   all {@link Inverter}s
	 * @param allConstraints all active {@link Constraint}s
	 * @param direction      the {@link TargetDirection}
	 * @return a solution or null
	 */
	public static PointValuePair apply(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			List<Inverter> allInverters, List<Constraint> allConstraints, TargetDirection direction) throws OpenemsException {
		if (esss.isEmpty()) {
			return null;
		}

		/*
		 *
		 * TODO:
		 *
		 * 	- Can we use the same logic for ACTIVE and REACTIVE power?
		 * 	- Documentation/Description with expected behavior of this implementation
		 *
		 *
		 * COMMENT:
		 *
		 * 	- Inverters are by default sorted by weight descending.
		 *    For DISCHARGE take list as it is; for CHARGE reverse it.
		 * 	  This prefers high-weight inverters (e.g. high state-of-charge) on DISCHARGE and low-weight inverters (e.g. low state-of-charge) on CHARGE.
		 * 		-> Note: The list will be adjusted slightly (when weight changes), see https://github.com/OpenEMS/openems/blob/develop/io.openems.edge.ess.core/src/io/openems/edge/ess/core/power/data/WeightsUtil.java#L60-L71
		 *
		 *  	-> The foreach for "use pv production power first" is in the correct order then -> We want to use all PV (=no DC charge) for the first ESSs in the list (during discharge)
		 * 		The ESSs last in list (descending order) will not use all PV power (=DC charge), if not all PV power is required. Therefore more charge for ESSs last in order, which are low-weight inverters (e.g. low state-of-charge)
		 * 		If we have additional AC power for charge (we are in charge mode). This prefers low-weight inverters (e.g. low state-of-charge) for the additional ac-charge.
		 *
		 */

		// Inverters are by default sorted by weight descending. For DISCHARGE take list
		// as it is; for CHARGE reverse it. This prefers high-weight inverters (e.g.
		// high state-of-charge) on DISCHARGE and low-weight
		// inverters (e.g. low state-of-charge) on CHARGE.
		List<Inverter> sortedInverters;
		if (direction == TargetDirection.DISCHARGE) {
			sortedInverters = allInverters;
		} else {
			sortedInverters = Lists.reverse(allInverters);
		}

		var debug=false;
		if(debug) {
			System.out.println(direction);

			//for (Inverter inv : allInverters) {
			//	System.out.println(inv.toString() + ": "+inv.getEssId()+" "+inv.getWeight());
			//}

			for (Inverter inv : sortedInverters) {
				System.out.println(inv.toString() + ": "+inv.getEssId()+" "+inv.getWeight());
			}

			for (ManagedSymmetricEss ess : esss) {
				System.out.println(ess.toString() + ": "+ess.id()+ ", min "+getMinPowerFromEss(ess, Pwr.ACTIVE)+", max "+getMaxPowerFromEss(ess, Pwr.ACTIVE));
			}
			System.out.println(allConstraints);
		}

		if(esss.stream().filter(MetaEss.class::isInstance).count()==0) {
			System.out.println("no ess cluster");
			return null;
		}

		var setActivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.ACTIVE);
		var setReactivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.REACTIVE);

		if (Double.isNaN(setActivePower) && Double.isNaN(setReactivePower)) {
			return null;
		}

		var essList = getGenericEssList(esss);

		var activePowerSolved = solvePowerIfNotNaN(setActivePower, essList, Pwr.ACTIVE, direction, coefficients, allConstraints, sortedInverters);
		var reactivePowerSolved = solvePowerIfNotNaN(setReactivePower, essList, Pwr.REACTIVE, direction, coefficients, allConstraints, sortedInverters);

		var mergedResult = mergeResults(coefficients, esss, activePowerSolved, reactivePowerSolved);

		var result = Arrays.stream(mergedResult)//
				.toArray();


		if(debug) {
			//var point = result;
			var point = activePowerSolved.getPoint();
			for (Inverter inv : allInverters) {
				var essId = inv.getEssId();
				for (Pwr pwr : Pwr.values()) {
					var c = coefficients.of(essId, inv.getPhase(), pwr);
					var value = point[c.getIndex()];

					System.out.println("Solution -> "+essId+" "+pwr+" "+value);
				}
			}
		}

		return activePowerSolved;
		//return new PointValuePair(result, 0);
	}

	private static Integer getPvProductionFromEss(ManagedSymmetricEss ess) {
		Integer pvProduction = ess.getPvProduction();
		if(pvProduction==null) {
			System.out.println(ess.id()+" does not report PVProduction | "+ess.getClass().toString());
			return 0;
		}

		return pvProduction;
	}


	/**
	 * Solve it only if the setpower is present.
	 *
	 * @param setPower        the Setpower
	 * @param essList         the {@link ManagedSymmetricEss}
	 * @param powerType       the powerType {@link Pwr}
	 * @param direction       the {@link TargetDirection}
	 * @param coefficients    the {@link Coefficients}
	 * @param allConstraints  all active {@link Constraint}s
	 * @param sortedInverters all {@link Inverter}s
	 * @return a solution
	 */
	private static PointValuePair solvePowerIfNotNaN(double setPower, List<ManagedSymmetricEss> essList, Pwr powerType,
			TargetDirection direction, Coefficients coefficients, List<Constraint> allConstraints, List<Inverter> sortedInverters) throws OpenemsException {
		if (Double.isNaN(setPower)) {
			var defaultResult = new double[essList.size()];
			return new PointValuePair(defaultResult, 0);
		} else {
			return solvePower(setPower, essList, powerType, direction, coefficients, allConstraints, sortedInverters);
		}
	}

	/**
	 * Merges results of Active-Power and Reactive-Power to a {@link PointValuePair}
	 * compatible with default Solver.
	 *
	 * @param coefficients  the {@link Coefficients}
	 * @param esss          the {@link ManagedSymmetricEss}s
	 * @param activePower   the {@link PointValuePair} for Active-Power
	 * @param reactivePower the {@link PointValuePair} for Reactive-Power
	 * @return new {@link PointValuePair}
	 */
	public static double[] mergeResults(Coefficients coefficients, List<ManagedSymmetricEss> esss,
			PointValuePair activePower, PointValuePair reactivePower) {
		try {
			var values = coefficients.getAll().stream() //
					.mapToDouble(c -> {
						var nonMetaEss = esss.stream() //
								.filter(e -> !(e instanceof MetaEss)) //
								.map(ManagedSymmetricEss::id) //
								.toList();

						var index = nonMetaEss.indexOf(c.getEssId());
						if (index == -1) {
							return 0;
						}

						final var pointValuePair = switch (c.getPwr()) {
						case ACTIVE -> activePower;
						case REACTIVE -> reactivePower;
						};
						return pointValuePair.getPoint()[index];
					}).toArray();
			return values;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Solves the power optimization problem for a given set of coefficients, energy
	 * storage systems (ESSs), inverters, constraints, description, power type, and
	 * target direction.
	 *
	 * @param power           the power to be distributed
	 * @param essList         the {@link ManagedSymmetricEss}s
	 * @param pwr             the {@link Pwr}.
	 * @param direction       the {@link TargetDirection}.
	 * @param coefficients    the {@link Coefficients}
	 * @param allConstraints  all active {@link Constraint}s
	 * @param sortedInverters all {@link Inverter}s
	 * @return The optimized solution The {@link PointValuePair}.
	 */
	private static PointValuePair solvePower(double power, List<ManagedSymmetricEss> essList, Pwr pwr,
			TargetDirection direction, Coefficients coefficients, List<Constraint> allConstraints, List<Inverter> sortedInverters) throws OpenemsException {
		List<Constraint> constraints = new ArrayList<>(allConstraints);

		var debug=false;
		if(debug) System.out.println("["+pwr+"] PowerSetPoint: "+power+ ", Direction: "+direction);

		if (direction == TargetDirection.KEEP_ZERO) {
			// Solve the system
			var defaultResult = ConstraintSolver.solve(coefficients, constraints);

			for (var inv : sortedInverters) {
				// Create Constraint to force Ess on ZERO
				defaultResult = addContraintIfProblemStillSolves(defaultResult, constraints, coefficients,
						createSimpleConstraint(coefficients, //
								inv.toString() + ": Force ActivePower " + direction.name(), //
								inv.getEssId(), inv.getPhase(), pwr, Relationship.EQUALS, 0));
			}
			return defaultResult;
		}

		var essUpperLimit = sortedInverters.stream()//
				.mapToDouble(inv -> getMaxPowerFromEss(getEss(essList, inv.getEssId()), pwr))//
				.toArray();

		var essLowerLimit = sortedInverters.stream()//
				.mapToDouble(inv -> getMinPowerFromEss(getEss(essList,inv.getEssId()), pwr))//
				.toArray();

		var essPvProduction = sortedInverters.stream()//
				.mapToDouble(inv -> getPvProductionFromEss(getEss(essList,inv.getEssId())))//
				.toArray();

		// Handle discharge case (limit to maxmimum power if exceedsTotalUpperBound)
		if (direction == TargetDirection.DISCHARGE && power > Arrays.stream(essUpperLimit).sum()) {
			System.out.print("!! Limit discharge power "+power+" to "+Arrays.stream(essUpperLimit).sum());
			power = Arrays.stream(essUpperLimit).sum();
		}

		// Handle charge case (limit to minimum power if exceedsTotalLowerBound)
		if (direction == TargetDirection.CHARGE && power < Arrays.stream(essLowerLimit).sum()) {
			System.out.print("!! Limit charge power "+power+" to "+Arrays.stream(essLowerLimit).sum());
			power = Arrays.stream(essLowerLimit).sum();
		}

		var essPowerRequired = new double[sortedInverters.size()];
		double remainingPowerRequired = power;


		// use pv production power first
		for (int i=0; i<sortedInverters.size(); i++) {
			var inv = sortedInverters.get(i);
			if(debug) System.out.print("["+pwr+"]   " + inv.toString() + ": PVProduction: "+essPvProduction[i]+", min: "+essLowerLimit[i]+", max: "+essUpperLimit[i]);

			switch (direction) {
			case TargetDirection.DISCHARGE -> {
				if(essLowerLimit[i]>remainingPowerRequired) {
					// lowerLimit > remainingPowerRequired (e.g. ess requires minimum discharge while battery is full)
					essPowerRequired[i] = essLowerLimit[i];
					remainingPowerRequired -= essLowerLimit[i];
				}
				else if(essPvProduction[i] >= 100 && remainingPowerRequired >= essPvProduction[i]) {
					// use all pvProduction power (minimum 100 W required)
					essPowerRequired[i] = essPvProduction[i];
					remainingPowerRequired -= essPvProduction[i];
				}
				else if(essPvProduction[i] >= 100)
				{
					// use partial pvProduction power (minimum 100 W required)
					essPowerRequired[i] = remainingPowerRequired;
					remainingPowerRequired = 0;
				}

				if(debug) System.out.print("  -> EQUALS "+essPowerRequired[i]);
			}
			}

			if(debug) System.out.println();
		}


		if(debug) System.out.println("["+pwr+"]   -> remaining power required after pv production solved: "+remainingPowerRequired);


		// use all ess which are already producing power (discharging)
		for (int i=0; i<sortedInverters.size(); i++) {
			if(remainingPowerRequired>0 && essPowerRequired[i]>0) {
				var inv = sortedInverters.get(i);
				var remainingUpperLimit = essUpperLimit[i]-essPowerRequired[i];
				if(debug) System.out.print("["+pwr+"]   " + inv.toString() + ": PVProduction: "+essPvProduction[i]+", min: "+essLowerLimit[i]+", max: "+essUpperLimit[i]);

				switch (direction) {
				case TargetDirection.DISCHARGE -> {
					if(remainingPowerRequired > remainingUpperLimit) {
						// remainingPowerRequired > upperLimit, discharge with upperLimit
						essPowerRequired[i] += remainingUpperLimit;
						remainingPowerRequired -= remainingUpperLimit;
					}
					else
					{
						// provide required discharge power
						essPowerRequired[i] += remainingPowerRequired;
						remainingPowerRequired = 0;
					}

					if(debug) System.out.print("  -> EQUALS "+essPowerRequired[i]);
				}
				}

				if(debug) System.out.println();
			}
		}

		if(debug) System.out.println("["+pwr+"]   -> remaining power required after ess already discharging solved: "+remainingPowerRequired);


		// use all ess if we still require more power (positive or negative)
		if(remainingPowerRequired != 0) {
			for (int i=0; i<sortedInverters.size(); i++) {
				var inv = sortedInverters.get(i);
				var remainingUpperLimit = essUpperLimit[i]-essPowerRequired[i];
				if(debug) System.out.print("["+pwr+"]   " + inv.toString() + ": PVProduction: "+essPvProduction[i]+", min: "+essLowerLimit[i]+", max: "+essUpperLimit[i]);

				switch (direction) {
				case TargetDirection.CHARGE -> {
					if(essLowerLimit[i] > remainingPowerRequired) {
						// lowerLimit > remainingPowerRequired, charge with lowerLimit
						essPowerRequired[i] = essLowerLimit[i];
						remainingPowerRequired += essLowerLimit[i]*(-1);
					}
					else
					{
						// charge with partial ess charge power
						essPowerRequired[i] = remainingPowerRequired;
						remainingPowerRequired = 0;
					}
				}
				case TargetDirection.DISCHARGE -> {
					if(remainingPowerRequired > remainingUpperLimit) {
						// remainingPowerRequired > upperLimit, discharge with upperLimit
						essPowerRequired[i] += remainingUpperLimit;
						remainingPowerRequired -= remainingUpperLimit;
					}
					else
					{
						// provide required discharge power
						essPowerRequired[i] += remainingPowerRequired;
						remainingPowerRequired = 0;
					}
				}
				}

				if(debug) System.out.println("\t-> "+inv.toString()+" EQUALS "+essPowerRequired[i]);
			}

			if(debug) System.out.println("["+pwr+"]   -> remaining power required after solving using all ess: "+remainingPowerRequired);
		}


		// Solve the system
		var result = ConstraintSolver.solve(coefficients, constraints);

		var relationship = switch (direction) {
		case CHARGE -> Relationship.LESS_OR_EQUALS;
		case DISCHARGE -> Relationship.GREATER_OR_EQUALS;
		case KEEP_ZERO -> Relationship.EQUALS;
		};

		for (var inv : sortedInverters) {
			// Create Constraint to force Ess positive/negative/zero according to
			// targetDirection
			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
					createSimpleConstraint(coefficients, //
							inv.toString() + ": Force ActivePower " + direction.name(), //
							inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, relationship, 0));
			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
					createSimpleConstraint(coefficients, //
							inv.toString() + ": Force ReactivePower " + direction.name(), //
							inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, relationship, 0));
		}

		for (int i=0; i<sortedInverters.size(); i++) {
			var inv = sortedInverters.get(i);

			if(debug) System.out.println("["+pwr+"] Add Constraint for "+inv.toString()+": "+relationship+" "+essPowerRequired[i]);
			result = addContraintIfProblemStillSolves(result, constraints, coefficients,
					createSimpleConstraint(coefficients, //
							inv.toString() + ": Set "+pwr.toString()+" Power " + direction.name() + " value", //
							inv.getEssId(), inv.getPhase(), pwr, relationship, essPowerRequired[i]));
		}

		return result;
	}

	private static ManagedSymmetricEss getEss(List<ManagedSymmetricEss> esss, String essId) {
		for (ManagedSymmetricEss ess : esss) {
			if (essId.equals(ess.id())) {
				return ess;
			}
		}
		return null;
	}

	/**
	 * Add Constraint only if the problem still solves with the Constraint.
	 *
	 * @param lastResult   the last result
	 * @param constraints  the list of {@link Constraint}s
	 * @param coefficients the {@link Coefficients}
	 * @param c            the {@link Constraint} to be added
	 * @return new solution on success; last result on error
	 */
	private static PointValuePair addContraintIfProblemStillSolves(PointValuePair lastResult,
			List<Constraint> constraints, Coefficients coefficients, Constraint c) {
		constraints.add(c);
		// Try to solve with Constraint
		try {
			return ConstraintSolver.solve(coefficients, constraints); // only if solving was successful
		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			// solving failed
			constraints.remove(c);
			return lastResult;
		}
	}

	private static double getPowerSetPoint(List<ManagedSymmetricEss> esss, List<Constraint> allConstraints,
			TargetDirection direction, Pwr pwr) {

		var clusterEssId = esss.stream()//
				.filter(MetaEss.class::isInstance)//
				.findFirst().get().id();

		var noPowerSetPoint = Double.NaN;

		return allConstraints.stream()//
				.filter(constraint -> constraint.getRelationship() == Relationship.EQUALS)
				.filter(constraint -> constraint.getCoefficients().length == 1)
				.filter(constraint -> clusterEssId.equals(constraint.getCoefficients()[0].getCoefficient().getEssId()))
				.filter(constraint -> constraint.getCoefficients()[0].getCoefficient().getPwr() == pwr)
				.mapToDouble(constraint -> constraint.getValue().get())//
				.findFirst()//
				.orElse(noPowerSetPoint);
	}

	private static List<ManagedSymmetricEss> getGenericEssList(List<ManagedSymmetricEss> esss) {
		return esss.stream()//
				.filter(e -> !(e instanceof MetaEss))//
				.toList();
	}

	/**
	 * Get the maximum power from a {@link ManagedSymmetricEss}.
	 *
	 * @param ess the {@link ManagedSymmetricEss}
	 * @param pwr the {@link Pwr} of power
	 * @return the maximum available power in watts for the given parameters
	 */
	private static int getMaxPowerFromEss(ManagedSymmetricEss ess, Pwr pwr) {
		return ess.getPower().getMaxPower(ess, ALL, pwr);
	}

	/**
	 * Get the minimum power from a {@link ManagedSymmetricEss}.
	 *
	 * @param ess the {@link ManagedSymmetricEss} *
	 * @param pwr the {@link Pwr} of power
	 * @return the maximum available power in watts for the given parameters
	 */
	private static int getMinPowerFromEss(ManagedSymmetricEss ess, Pwr pwr) {
		return ess.getPower().getMinPower(ess, ALL, pwr);
	}
}