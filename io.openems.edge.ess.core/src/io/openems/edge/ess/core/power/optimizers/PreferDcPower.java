package io.openems.edge.ess.core.power.optimizers;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.core.power.data.ConstraintUtil.createSimpleConstraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
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

	private static final PointValuePair NO_RESULT = null;
	private static final Logger log = LoggerFactory.getLogger(PreferDcPower.class);

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
			List<Inverter> allInverters, List<Constraint> allConstraints, TargetDirection direction,
			boolean debugMode) throws OpenemsException {

		if (esss.isEmpty()) {
			return null;
		}

		/*
		 *
		 * TODO:
		 *
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

		if(esss.stream().filter(MetaEss.class::isInstance).count()==0) {
			log.info("no ess cluster");
			return NO_RESULT; // use next solver
		}

		var setActivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.ACTIVE);
		var setReactivePower = getPowerSetPoint(esss, allConstraints, direction, Pwr.REACTIVE);

		var essList = getGenericEssList(esss);

		return solvePower(setActivePower, setReactivePower, essList, direction, coefficients, allConstraints, sortedInverters, debugMode);
	}

	/**
	 * Solves the power optimization problem for a given set of coefficients, energy
	 * storage systems (ESSs), inverters, constraints and target direction.
	 *
	 * @param activePower     the active power to be distributed
	 * @param reactivePower   the reactive power to be distributed
	 * @param essList         the {@link ManagedSymmetricEss}s
	 * @param direction       the {@link TargetDirection}.
	 * @param coefficients    the {@link Coefficients}
	 * @param allConstraints  all active {@link Constraint}s
	 * @param sortedInverters all {@link Inverter}s
	 * @param debugMode       the {@link EssPower} debug mode
	 * @return The optimized solution The {@link PointValuePair}.
	 */
	private static PointValuePair solvePower(double activePower, double reactivePower, List<ManagedSymmetricEss> essList, TargetDirection direction,
			Coefficients coefficients, List<Constraint> allConstraints, List<Inverter> sortedInverters, boolean debugMode) throws OpenemsException {
		List<Constraint> constraints = new ArrayList<>(allConstraints);
		var result = ConstraintSolver.solve(coefficients, constraints);

		var essState = sortedInverters.stream()//
				.map(inv -> getEss(essList,inv.getEssId()).getState())//
				.toArray();

		if (!Double.isNaN(activePower)) {

			/*
			 * Solve Active Power
			 *
			 */

			var essUpperLimit = sortedInverters.stream()//
					.mapToDouble(inv -> getMaxPowerFromEss(getEss(essList, inv.getEssId()), Pwr.ACTIVE))//
					.toArray();

			var essLowerLimit = sortedInverters.stream()//
					.mapToDouble(inv -> getMinPowerFromEss(getEss(essList,inv.getEssId()), Pwr.ACTIVE))//
					.toArray();

			var essPvProduction = sortedInverters.stream()//
					.mapToDouble(inv -> getPvProductionFromEss(getEss(essList,inv.getEssId())))//
					.toArray();

			if(debugMode) log.debug("[ACTIVE] PowerSetPoint: "+activePower+ ", Direction: "+direction);

			var essPowerRequired = new double[sortedInverters.size()];
			double remainingPowerRequired = activePower;

			// Step 1: Distribute the active power

			// Step 1.1: Distribute PV production power first
			for (int i=0; i<sortedInverters.size(); i++) {
				var inv = sortedInverters.get(i);
				var logMessage = new String();
				if(debugMode) logMessage += "[ACTIVE]   " + inv.toString() + ": PVProduction: "+essPvProduction[i]+", min: "+essLowerLimit[i]+", max: "+essUpperLimit[i];

				if(essState[i] == Level.FAULT) {
					if(debugMode) logMessage += "  -> ESS state: FAULT";
					continue;
				}

				if(direction == TargetDirection.DISCHARGE) {
					if(essLowerLimit[i] > remainingPowerRequired) {
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

					if(debugMode) logMessage += "  -> EQUALS "+essPowerRequired[i];
				}

				if(debugMode) log.debug(logMessage);
			}


			if(debugMode) log.debug("[ACTIVE]   -> remaining power required after pv production solved: "+remainingPowerRequired);


			// Step 1.2: Distribute remaining power using all ESS currently producing power (discharging due to PV production); distribute using order
			if(remainingPowerRequired != 0) {
				for (int i=0; i<sortedInverters.size(); i++) {
					if(remainingPowerRequired>0 && essPowerRequired[i]>0) {
						var inv = sortedInverters.get(i);
						var remainingUpperLimit = essUpperLimit[i]-essPowerRequired[i];
						var logMessage = new String();
						if(debugMode) logMessage += "[ACTIVE]   " + inv.toString() + ": PVProduction: "+essPvProduction[i]+", min: "+essLowerLimit[i]+", max: "+essUpperLimit[i];

						if(essState[i] == Level.FAULT) {
							if(debugMode) logMessage += "  -> ESS state: FAULT";
							continue;
						}

						if(direction == TargetDirection.DISCHARGE) {
							if(remainingPowerRequired > remainingUpperLimit && remainingUpperLimit >= 0) {
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

							if(debugMode) logMessage += "  -> EQUALS "+essPowerRequired[i];
						}

						if(debugMode) log.debug(logMessage);
					}
				}

				if(debugMode) log.debug("[ACTIVE]   -> remaining power required after ess already discharging solved: "+remainingPowerRequired);
			}


			// Step 1.3: Distribute remaining power using all ESS if we still require more power; distribute using order
			if(remainingPowerRequired != 0) {
				for (int i=0; i<sortedInverters.size(); i++) {
					var inv = sortedInverters.get(i);
					var remainingLowerLimit = essLowerLimit[i]-essPowerRequired[i];
					var remainingUpperLimit = essUpperLimit[i]-essPowerRequired[i];
					var logMessage = new String();
					if(debugMode) logMessage += "[ACTIVE]   " + inv.toString() + ": PVProduction: "+essPvProduction[i]+", min: "+essLowerLimit[i]+", max: "+essUpperLimit[i];

					if(essState[i] == Level.FAULT) {
						if(debugMode) logMessage += "  -> ESS state: FAULT";
						continue;
					}

					if(direction == TargetDirection.CHARGE) {
						if(remainingLowerLimit > remainingPowerRequired && remainingLowerLimit <= 0) {
							// lowerLimit > remainingPowerRequired, charge with lowerLimit
							essPowerRequired[i] += remainingLowerLimit;
							remainingPowerRequired -= remainingLowerLimit;
						}
						else
						{
							// charge with partial ess charge power
							essPowerRequired[i] += remainingPowerRequired;
							remainingPowerRequired = 0;
						}
					} else if(direction == TargetDirection.DISCHARGE) {
						if(remainingPowerRequired > remainingUpperLimit && remainingUpperLimit >= 0) {
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

					if(debugMode) log.debug(logMessage + "\t-> "+inv.toString()+" EQUALS "+essPowerRequired[i]);
				}

				if(debugMode) log.debug("[ACTIVE]   -> remaining power required after solving using all ess: "+remainingPowerRequired);
			}

			// Step 2: Solve the active power system

			var relationship = switch (direction) {
			case CHARGE -> Relationship.LESS_OR_EQUALS;
			case DISCHARGE -> Relationship.GREATER_OR_EQUALS;
			case KEEP_ZERO -> Relationship.EQUALS;
			};

			for (int i=0; i<sortedInverters.size(); i++) {
				var inv = sortedInverters.get(i);

				if(essState[i] == Level.FAULT) {
					// Create Constraint to force faulty Ess on ZERO
					if(debugMode) log.debug("[ACTIVE] Add Constraint for "+inv.toString()+": EQUALS "+essPowerRequired[i]);
					result = addContraintIfProblemStillSolves(result, constraints, coefficients,
							createSimpleConstraint(coefficients, //
									inv.toString() + ": Force ActivePower KEEP_ZERO", //
									inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, 0));
				} else {
					// Create Constraint to force Ess positive/negative/zero according to
					// targetDirection
					result = addContraintIfProblemStillSolves(result, constraints, coefficients,
							createSimpleConstraint(coefficients, //
									inv.toString() + ": Force ActivePower " + direction.name(), //
									inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, relationship, 0));
				}
			}

			for (int i=0; i<sortedInverters.size(); i++) {
				var inv = sortedInverters.get(i);

				if(essState[i] != Level.FAULT) {
					if(debugMode) log.debug("[ACTIVE] Add Constraint for "+inv.toString()+": "+Relationship.EQUALS+" "+essPowerRequired[i]);
					result = addContraintIfProblemStillSolves(result, constraints, coefficients,
							createSimpleConstraint(coefficients, //
									inv.toString() + ": Set ActivePower " + direction.name() + " value", //
									inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, essPowerRequired[i]));
				}
			}

		}

		// Exit, if reactivePower is NaN
		if(Double.isNaN(reactivePower)) return result;

		/*
		 * Solve Reactive Power
		 *
		 */

		var reactiveDirection = getReactiveDirection(reactivePower);
		var essActivePowerSetpoint = new double[sortedInverters.size()];
		var essReactiveUpperLimit = new double[sortedInverters.size()];
		var essReactiveLowerLimit = new double[sortedInverters.size()];

		var dischargeEssActivePower = new double[sortedInverters.size()];
		var idleEssMaxQ = new double[sortedInverters.size()];
		var allEssMaxQ = new double[sortedInverters.size()];

		double dischargeEssActivePowerTotal = 0;
		double idleEssMaxQTotal = 0;
		double allEssMaxQTotal = 0;

		if(debugMode) log.debug("[REACTIVE] PowerSetPoint: "+reactivePower+ ", Direction: "+reactiveDirection);

		// Step 3: Gets the active power from constraint system and define upper and lower limits
		var point = result.getPoint();
		double weightSum = 0;
		double weightDistributedPowerTotal = 0;
		for (int i=0; i<sortedInverters.size(); i++) {
			var inv = sortedInverters.get(i);
			var essId = inv.getEssId();
			var ess = getEss(essList, essId);
			int soc = ess.getSoc().orElse(0);
			var precision = ess.getPowerPrecision();
			var round = Round.TOWARDS_ZERO;
			var c = coefficients.of(essId, inv.getPhase(), Pwr.ACTIVE);
			var value = Math.round(point[c.getIndex()]);
			if (value > 0 && soc > 50 || value < 0 && soc < 50) {
				round = Round.AWAY_FROM_ZERO;
			}

			// skip ESS with FAULT state
			if(essState[i] == Level.FAULT) {
				continue;
			}

			// fill active power setpoint to array
			essActivePowerSetpoint[i] = IntUtils.roundToPrecision((float) value, round, precision);
			if (essActivePowerSetpoint[i] == -1 || essActivePowerSetpoint[i] == 1) {
				essActivePowerSetpoint[i] = 0; // avoid unnecessary power settings on rounding 0.xxx to 1
			}

			// get maxQ using formula sMax = sqrt(P²+Q²)
			double sMax = ess.getMaxApparentPower().getOrError();
			double maxQ = Math.sqrt( (sMax*sMax) - (essActivePowerSetpoint[i]*essActivePowerSetpoint[i]) );
			double minQ = maxQ == 0 ? 0 : maxQ*(-1);

			// set maxQ to zero, if max allowed Q is < 50
			if(sMax-Math.abs(essActivePowerSetpoint[i]) < 50) {
				maxQ = 0;
				minQ = 0;
			}

			maxQ = Math.floor(maxQ);
			minQ = Math.ceil(minQ);

			// get upper and lower limits from ESS
			essReactiveUpperLimit[i] = getMaxPowerFromEss(getEss(essList, inv.getEssId()), Pwr.REACTIVE);
			essReactiveLowerLimit[i] = getMinPowerFromEss(getEss(essList, inv.getEssId()), Pwr.REACTIVE);

			// reduce upper and lower limit to maxQ/minQ if we have an active power setpoint
			if(essActivePowerSetpoint[i] != 0 && maxQ<essReactiveUpperLimit[i]) essReactiveUpperLimit[i] = maxQ;
			if(essActivePowerSetpoint[i] != 0 && minQ>essReactiveLowerLimit[i]) essReactiveLowerLimit[i] = minQ;

			// calculate weights of discharging inverters with remaining Q using essActivePower/essActivePowerTotal
			if(essActivePowerSetpoint[i] > 0 && maxQ > 0)
			{
				dischargeEssActivePower[i] = essActivePowerSetpoint[i];
				dischargeEssActivePowerTotal += essActivePowerSetpoint[i];
			}

			// calculate weights of idle inverters with remaining Q using essRemainingQ/essRemainingQTotal
			if(essActivePowerSetpoint[i] == 0 && maxQ > 0) {
				idleEssMaxQ[i] = essReactiveUpperLimit[i];
				idleEssMaxQTotal += essReactiveUpperLimit[i];
			}

			// calculate weights of all inverters using essRemainingQ/essRemainingQTotal
			allEssMaxQ[i] = essReactiveUpperLimit[i];
			allEssMaxQTotal += essReactiveUpperLimit[i];
		}


		// Step 4: Distribute the reactive power
		var essReactivePowerRequired = new double[sortedInverters.size()];
		double remainingReactivePowerRequired = reactivePower;

		// Step 4.1: Distribute using calculated weights first
		do {
			final double dischargeEssActivePowerTotal2 = dischargeEssActivePowerTotal;
			final double idleEssMaxQTotal2 = idleEssMaxQTotal;
			final double allEssMaxQTotal2 = allEssMaxQTotal;

			enum WeightStrategy {
			    USE_DISCHARGING_ESS, // prefer discharging ESS
			    USE_IDLE_ESS, // use idle ESS if discharging ESS are not sufficient
			    USE_ALL_ESS; // use all ESS if discharging and idle ESS are not sufficient
			}

			WeightStrategy weightStrategy;

			// define weight strategy
			if(dischargeEssActivePowerTotal != 0) weightStrategy = WeightStrategy.USE_DISCHARGING_ESS;
			else if(idleEssMaxQTotal != 0) weightStrategy = WeightStrategy.USE_IDLE_ESS;
			else weightStrategy = WeightStrategy.USE_ALL_ESS;

			// calculate weights
			var weights = switch (weightStrategy) {
		    case USE_DISCHARGING_ESS:
		        yield Arrays.stream(dischargeEssActivePower).map(setpoint -> Math.round((setpoint/dischargeEssActivePowerTotal2)*100.0)/100.0 ).toArray();
		    case USE_IDLE_ESS:
		        yield Arrays.stream(idleEssMaxQ).map(maxQ -> Math.round((maxQ/idleEssMaxQTotal2)*100.0)/100.0 ).toArray();
		    case USE_ALL_ESS:
		        yield Arrays.stream(allEssMaxQ).map(maxQ -> Math.round((maxQ/allEssMaxQTotal2)*100.0)/100.0 ).toArray();
			};

			weightSum = Arrays.stream(weights).sum();

			// reset counters
			dischargeEssActivePower = new double[sortedInverters.size()];
			dischargeEssActivePowerTotal = 0;
			idleEssMaxQ = new double[sortedInverters.size()];
			idleEssMaxQTotal = 0;
			allEssMaxQ = new double[sortedInverters.size()];
			allEssMaxQTotal = 0;

			double remainingReactivePowerForWeightsRun = remainingReactivePowerRequired;
			weightDistributedPowerTotal = 0;
			for (int i=0; i<sortedInverters.size(); i++) {
				var q = Math.round(remainingReactivePowerForWeightsRun*weights[i]);
				weightDistributedPowerTotal += q;
			}

			if(remainingReactivePowerRequired != 0 && weightSum > 0 && weightDistributedPowerTotal != 0) {
				for (int i=0; i<sortedInverters.size(); i++) {
					if(remainingReactivePowerRequired!=0) {
						var inv = sortedInverters.get(i);
						var q = Math.round(remainingReactivePowerForWeightsRun*weights[i]);
						var remainingReactiveLowerLimit = essReactiveLowerLimit[i]-essReactivePowerRequired[i];
						var remainingReactiveUpperLimit = essReactiveUpperLimit[i]-essReactivePowerRequired[i];
						var logMessage = new String();
						if(debugMode) logMessage += "[REACTIVE]   " + inv.toString() + ": weight: "+weights[i]+", min: "+essReactiveLowerLimit[i]+", max: "+essReactiveUpperLimit[i];

						if(weights[i]==0) {
							if(debugMode) log.debug(logMessage + "  -> EQUALS 0.0"); // weight zero
							continue;
						}

						if(reactiveDirection == TargetDirection.CHARGE) {
							if(remainingReactivePowerRequired < remainingReactiveLowerLimit && remainingReactiveLowerLimit <= 0) {
								// lowerLimit > remainingPowerRequired, charge with lowerLimit
								essReactivePowerRequired[i] += remainingReactiveLowerLimit;
								remainingReactivePowerRequired -= remainingReactiveLowerLimit;
							}
							else if(q < remainingReactivePowerRequired) {
								// q < remainingReactivePowerRequired, use remainigReactivePowerRequired
								essReactivePowerRequired[i] += remainingReactivePowerRequired;
								remainingReactivePowerRequired -= remainingReactivePowerRequired;
							}
							else
							{
								// provide required (negative) reactive power
								essReactivePowerRequired[i] += q;
								remainingReactivePowerRequired -= q;
							}
						} else if(reactiveDirection == TargetDirection.DISCHARGE) {
							if(remainingReactivePowerRequired > remainingReactiveUpperLimit && remainingReactiveUpperLimit >= 0) {
								// remainingPowerRequired > upperLimit, discharge with upperLimit
								essReactivePowerRequired[i] += remainingReactiveUpperLimit;
								remainingReactivePowerRequired -= remainingReactiveUpperLimit;
							}
							else if(q > remainingReactivePowerRequired) {
								// q > remainingReactivePowerRequired, use remainigReactivePowerRequired
								essReactivePowerRequired[i] += remainingReactivePowerRequired;
								remainingReactivePowerRequired -= remainingReactivePowerRequired;
							}
							else
							{
								// provide required (positive) reactive power
								essReactivePowerRequired[i] += q;
								remainingReactivePowerRequired -= q;
							}
						}

						if(debugMode) log.debug(logMessage + "\t-> "+inv.toString()+" EQUALS "+essReactivePowerRequired[i]);
					}
				}

				// update counters for weight calculation
				for (int i=0; i<sortedInverters.size(); i++) {
					var remainingReactiveLowerLimit = essReactiveLowerLimit[i]-essReactivePowerRequired[i];
					var remainingReactiveUpperLimit = essReactiveUpperLimit[i]-essReactivePowerRequired[i];

					// skip ESS with FAULT state
					if(essState[i] == Level.FAULT) {
						continue;
					}

					if(reactiveDirection == TargetDirection.CHARGE) {

						// calculate weights of discharging inverters with remaining Q using essActivePower/essActivePowerTotal
						if(essActivePowerSetpoint[i] > 0 && remainingReactiveLowerLimit < 0) {
							dischargeEssActivePower[i] = essActivePowerSetpoint[i];
							dischargeEssActivePowerTotal += essActivePowerSetpoint[i];
						}

						// calculate weights of idle inverters with remaining Q using essRemainingQ/essRemainingQTotal
						if(essActivePowerSetpoint[i] == 0 && remainingReactiveLowerLimit < 0) {
							idleEssMaxQ[i] = Math.abs(remainingReactiveLowerLimit);
							idleEssMaxQTotal += idleEssMaxQ[i];
						}

						// calculate weights of all inverters using essRemainingQ/essRemainingQTotal
						allEssMaxQ[i] = Math.abs(remainingReactiveLowerLimit);
						allEssMaxQTotal += allEssMaxQ[i];
					} else if (reactiveDirection == TargetDirection.DISCHARGE) {

						// calculate weights of discharging inverters with remaining Q using essActivePower/essActivePowerTotal
						if(essActivePowerSetpoint[i] > 0 && remainingReactiveUpperLimit > 0) {
							dischargeEssActivePower[i] = essActivePowerSetpoint[i];
							dischargeEssActivePowerTotal += essActivePowerSetpoint[i];
						}

						// calculate weights of idle inverters with remaining Q using essRemainingQ/essRemainingQTotal
						if(essActivePowerSetpoint[i] == 0 && remainingReactiveUpperLimit > 0) {
							idleEssMaxQ[i] = remainingReactiveUpperLimit;
							idleEssMaxQTotal += remainingReactiveUpperLimit;
						}

						// calculate weights of all inverters using essRemainingQ/essRemainingQTotal
						allEssMaxQ[i] = remainingReactiveUpperLimit;
						allEssMaxQTotal += remainingReactiveUpperLimit;
					}


				}

				if(debugMode) log.debug("[REACTIVE]   -> remaining power required after solving using calculated weights (strategy "+weightStrategy+"): "+remainingReactivePowerRequired);
			}
		}while(remainingReactivePowerRequired != 0 && weightSum > 0 && weightDistributedPowerTotal != 0);


		// Step 4.2: Distribute remaining reactive power using discharging ESS; distribute using order
		if(remainingReactivePowerRequired != 0) {
			for (int i=0; i<sortedInverters.size(); i++) {
				if(remainingReactivePowerRequired != 0 && essActivePowerSetpoint[i] > 0) {
					var inv = sortedInverters.get(i);
					var remainingReactiveLowerLimit = essReactiveLowerLimit[i]-essReactivePowerRequired[i];
					var remainingReactiveUpperLimit = essReactiveUpperLimit[i]-essReactivePowerRequired[i];
					var logMessage = new String();
					if(debugMode) logMessage += "[REACTIVE]   " + inv.toString() + ": min: "+essReactiveLowerLimit[i]+", max: "+essReactiveUpperLimit[i];

					if(reactiveDirection == TargetDirection.CHARGE) {
						if(remainingReactiveLowerLimit > remainingReactivePowerRequired && remainingReactiveLowerLimit <= 0) {
							// lowerLimit > remainingPowerRequired, charge with lowerLimit
							essReactivePowerRequired[i] += remainingReactiveLowerLimit;
							remainingReactivePowerRequired -= remainingReactiveLowerLimit;
						}
						else
						{
							// provide required (negative) reactive power
							essReactivePowerRequired[i] += remainingReactivePowerRequired;
							remainingReactivePowerRequired = 0;
						}
					} else if(reactiveDirection == TargetDirection.DISCHARGE) {
						if(remainingReactivePowerRequired > remainingReactiveUpperLimit && remainingReactiveUpperLimit >= 0) {
							// remainingPowerRequired > upperLimit, discharge with upperLimit
							essReactivePowerRequired[i] += remainingReactiveUpperLimit;
							remainingReactivePowerRequired -= remainingReactiveUpperLimit;
						}
						else
						{
							// provide required (positive) reactive power
							essReactivePowerRequired[i] += remainingReactivePowerRequired;
							remainingReactivePowerRequired = 0;
						}
					}

					if(debugMode) log.debug(logMessage + "\t-> "+inv.toString()+" EQUALS "+essReactivePowerRequired[i]);
				}
			}

			if(debugMode) log.debug("[REACTIVE]   -> remaining power required after solving using discharging ess (distributed using order): "+remainingReactivePowerRequired);
		}


		// Step 4.3: Distribute remaining reactive power using idle ESS; distribute using order
		if(remainingReactivePowerRequired != 0) {
			for (int i=0; i<sortedInverters.size(); i++) {
				if(remainingReactivePowerRequired != 0 && essActivePowerSetpoint[i] == 0) {
					var inv = sortedInverters.get(i);
					var remainingReactiveLowerLimit = essReactiveLowerLimit[i]-essReactivePowerRequired[i];
					var remainingReactiveUpperLimit = essReactiveUpperLimit[i]-essReactivePowerRequired[i];
					var logMessage = new String();
					if(debugMode) logMessage += "[REACTIVE]   " + inv.toString() + ": min: "+essReactiveLowerLimit[i]+", max: "+essReactiveUpperLimit[i];

					if(essState[i] == Level.FAULT) {
						if(debugMode) log.debug(logMessage + "  -> ESS state: FAULT");
						continue;
					}

					if(reactiveDirection == TargetDirection.CHARGE) {
						if(remainingReactiveLowerLimit > remainingReactivePowerRequired && remainingReactiveLowerLimit <= 0) {
							// lowerLimit > remainingPowerRequired, charge with lowerLimit
							essReactivePowerRequired[i] += remainingReactiveLowerLimit;
							remainingReactivePowerRequired -= remainingReactiveLowerLimit;
						}
						else
						{
							// provide required (negative) reactive power
							essReactivePowerRequired[i] += remainingReactivePowerRequired;
							remainingReactivePowerRequired = 0;
						}
					} else if(reactiveDirection == TargetDirection.DISCHARGE) {
						if(remainingReactivePowerRequired > remainingReactiveUpperLimit && remainingReactiveUpperLimit >= 0) {
							// remainingPowerRequired > upperLimit, discharge with upperLimit
							essReactivePowerRequired[i] += remainingReactiveUpperLimit;
							remainingReactivePowerRequired -= remainingReactiveUpperLimit;
						}
						else
						{
							// provide required (positive) reactive power
							essReactivePowerRequired[i] += remainingReactivePowerRequired;
							remainingReactivePowerRequired = 0;
						}
					}

					if(debugMode) log.debug(logMessage + "\t-> "+inv.toString()+" EQUALS "+essReactivePowerRequired[i]);
				}
			}

			if(debugMode) log.debug("[REACTIVE]   -> remaining power required after solving using idle ess (distributed using order): "+remainingReactivePowerRequired);
		}


		// Step 4.4: Distribute remaining reactive power using all ESS; distribute using order
		if(remainingReactivePowerRequired != 0) {
			for (int i=0; i<sortedInverters.size(); i++) {
				var inv = sortedInverters.get(i);
				var remainingReactiveLowerLimit = essReactiveLowerLimit[i]-essReactivePowerRequired[i];
				var remainingReactiveUpperLimit = essReactiveUpperLimit[i]-essReactivePowerRequired[i];
				var logMessage = new String();
				if(debugMode) logMessage += "[REACTIVE]   " + inv.toString() + ": min: "+essReactiveLowerLimit[i]+", max: "+essReactiveUpperLimit[i];

				if(essState[i] == Level.FAULT) {
					if(debugMode) log.debug(logMessage + "  -> ESS state: FAULT");
					continue;
				}

				if(reactiveDirection == TargetDirection.CHARGE) {
					if(remainingReactiveLowerLimit > remainingReactivePowerRequired && remainingReactiveLowerLimit <= 0) {
						// lowerLimit > remainingPowerRequired, charge with lowerLimit
						essReactivePowerRequired[i] += remainingReactiveLowerLimit;
						remainingReactivePowerRequired -= remainingReactiveLowerLimit;
					}
					else
					{
						// provide required (negative) reactive power
						essReactivePowerRequired[i] += remainingReactivePowerRequired;
						remainingReactivePowerRequired = 0;
					}
				} else if(reactiveDirection == TargetDirection.DISCHARGE) {
					if(remainingReactivePowerRequired > remainingReactiveUpperLimit && remainingReactiveUpperLimit >= 0) {
						// remainingPowerRequired > upperLimit, discharge with upperLimit
						essReactivePowerRequired[i] += remainingReactiveUpperLimit;
						remainingReactivePowerRequired -= remainingReactiveUpperLimit;
					}
					else
					{
						// provide required (positive) reactive power
						essReactivePowerRequired[i] += remainingReactivePowerRequired;
						remainingReactivePowerRequired = 0;
					}
				}

				if(debugMode) log.debug(logMessage + "\t-> "+inv.toString()+" EQUALS "+essReactivePowerRequired[i]);
			}

			if(debugMode) log.debug("[REACTIVE]   -> remaining power required after solving using all ess (distributed using order): "+remainingReactivePowerRequired);
		}


		// Step 5: Solve the reactive power system

		var reactiveRelationship = switch (reactiveDirection) {
		case CHARGE -> Relationship.LESS_OR_EQUALS;
		case DISCHARGE -> Relationship.GREATER_OR_EQUALS;
		case KEEP_ZERO -> Relationship.EQUALS;
		};

		for (int i=0; i<sortedInverters.size(); i++) {
			var inv = sortedInverters.get(i);

			if(essState[i] == Level.FAULT) {
				// Create Constraint to force faulty Ess on ZERO
				if(debugMode) log.debug("[REACTIVE] Add Constraint for "+inv.toString()+": EQUALS "+essReactivePowerRequired[i]);
				result = addContraintIfProblemStillSolves(result, constraints, coefficients,
						createSimpleConstraint(coefficients, //
								inv.toString() + ": Force ReactivePower KEEP_ZERO", //
								inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, Relationship.EQUALS, 0));
			} else {
				// Create Constraint to force Ess positive/negative/zero according to
				// reactiveDirection
				result = addContraintIfProblemStillSolves(result, constraints, coefficients,
						createSimpleConstraint(coefficients, //
								inv.toString() + ": Force ReactivePower " + reactiveDirection.name(), //
								inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, reactiveRelationship, 0));
			}
		}

		for (int i=0; i<sortedInverters.size(); i++) {
			var inv = sortedInverters.get(i);

			if(essState[i] != Level.FAULT) {
				if(debugMode) log.debug("[REACTIVE] Add Constraint for "+inv.toString()+": "+Relationship.EQUALS+" "+essReactivePowerRequired[i]);
				result = addContraintIfProblemStillSolves(result, constraints, coefficients,
						createSimpleConstraint(coefficients, //
								inv.toString() + ": Set ReactivePower " + reactiveDirection.name() + " value", //
								inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, Relationship.EQUALS, essReactivePowerRequired[i]));
			}
		}

		return result;
	}

	private static TargetDirection getReactiveDirection(double reactivePower) {
		if(reactivePower > 0) return TargetDirection.DISCHARGE;
		if(reactivePower < 0) return TargetDirection.CHARGE;
		return TargetDirection.KEEP_ZERO;
	}

	private static ManagedSymmetricEss getEss(List<ManagedSymmetricEss> esss, String essId) {
		for (ManagedSymmetricEss ess : esss) {
			if (essId.equals(ess.id())) {
				return ess;
			}
		}
		return null;
	}

	private static Integer getPvProductionFromEss(ManagedSymmetricEss ess) {
		Integer pvProduction = ess.getPvProduction();
		if(pvProduction==null) {
			log.info(ess.id()+" does not report PVProduction | "+ess.getClass().toString());
			return 0;
		}

		return pvProduction;
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
		var maxPower = ess.getPower().getMaxPower(ess, ALL, pwr);
		var minPower =  ess.getPower().getMinPower(ess, ALL, pwr);

		// Verify that maxPower is not lower than minPower (on force charge); tolerate rounding difference
		if(maxPower<0 && maxPower<minPower && minPower-maxPower>1) maxPower = minPower;

		// When constraint system gives negative/zero, use raw ESS discharge limit
		try {
			var allowedDischargeValue = ess.getAllowedDischargePower().getOrError();
			var allowedChargeValue = ess.getAllowedChargePower().getOrError();
			if (maxPower > allowedDischargeValue) {
				return allowedDischargeValue; // limit maxPower to allowedDischargeValue
			} else if(maxPower < allowedChargeValue) {
				return allowedChargeValue; // limit maxPower to allowedChargeValue (on force charge)
			} else {
				// Tolerate rounding difference (e.g. maxPower 9999, allowedDischargePower 10000)
				if(allowedDischargeValue-maxPower == 1) {
					return allowedDischargeValue;
				}
				return maxPower;
			}
		} catch (Exception e) {
			// Value not available, use fallback
		}
		return 1; // Fallback if null, invalid, or not available
	}

	/**
	 * Get the minimum power from a {@link ManagedSymmetricEss}.
	 *
	 * @param ess the {@link ManagedSymmetricEss} *
	 * @param pwr the {@link Pwr} of power
	 * @return the maximum available power in watts for the given parameters
	 */
	private static int getMinPowerFromEss(ManagedSymmetricEss ess, Pwr pwr) {
		var maxPower = ess.getPower().getMaxPower(ess, ALL, pwr);
		var minPower =  ess.getPower().getMinPower(ess, ALL, pwr);

		// Verify that minPower is not higher than maxPower (on force discharge)
		if(minPower>0 && minPower>maxPower) minPower = maxPower;

		// When constraint system gives negative/zero, use raw ESS discharge limit
		try {
			var allowedDischargeValue = ess.getAllowedDischargePower().getOrError();
			var allowedChargeValue = ess.getAllowedChargePower().getOrError();
			if (minPower < allowedChargeValue) {
				return allowedChargeValue; // limit minPower to allowedChargeValue
			} else if(minPower > allowedDischargeValue) {
				return allowedDischargeValue; // limit minPower to allowedDischargeValue (on force discharge)
			} else {
				// Tolerate rounding difference (e.g. minPower -9999, allowedChargePower -10000)
				if(minPower-allowedChargeValue == 1) {
					return allowedChargeValue;
				}
				return minPower;
			}
		} catch (Exception e) {
			// Value not available, use fallback
		}
		return -1; // Fallback if null, invalid, or not available
	}
}