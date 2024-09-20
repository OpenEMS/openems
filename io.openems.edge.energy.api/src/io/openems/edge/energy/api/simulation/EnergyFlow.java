package io.openems.edge.energy.api.simulation;

import static io.openems.edge.energy.api.simulation.Coefficient.CONS;
import static io.openems.edge.energy.api.simulation.Coefficient.ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.ESS_TO_CONS;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID_TO_CONS;
import static io.openems.edge.energy.api.simulation.Coefficient.GRID_TO_ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_CONS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_ESS;
import static io.openems.edge.energy.api.simulation.Coefficient.PROD_TO_GRID;
import static java.lang.Math.min;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.math3.optim.linear.Relationship.EQ;
import static org.apache.commons.math3.optim.linear.Relationship.GEQ;
import static org.apache.commons.math3.optim.linear.Relationship.LEQ;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MAXIMIZE;
import static org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MINIMIZE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.energy.api.simulation.GlobalSimulationsContext.Period;

/**
 * Holds the {@link Solution} of an {@link EnergyFlow.Model} and provides helper
 * functions to access the individual {@link Coefficient}s.
 */
public class EnergyFlow {

	private static final Logger LOG = LoggerFactory.getLogger(EnergyFlow.class);

	private final double[] point;

	private EnergyFlow(PointValuePair pvp) {
		this.point = pvp.getPointRef();
	}

	/**
	 * Gets {@link Coefficient#PROD}.
	 * 
	 * @return the value
	 */
	public int getProd() {
		return this.getValue(PROD);
	}

	/**
	 * Gets {@link Coefficient#CONS}.
	 * 
	 * @return the value
	 */
	public int getCons() {
		return this.getValue(CONS);
	}

	/**
	 * Gets {@link Coefficient#ESS}.
	 * 
	 * @return the value
	 */
	public int getEss() {
		return this.getValue(ESS);
	}

	/**
	 * Gets {@link Coefficient#GRID}.
	 * 
	 * @return the value
	 */
	public int getGrid() {
		return this.getValue(GRID);
	}

	/**
	 * Gets {@link Coefficient#PROD_TO_CONS}.
	 * 
	 * @return the value
	 */
	public int getProdToCons() {
		return this.getValue(PROD_TO_CONS);
	}

	/**
	 * Gets {@link Coefficient#PROD_TO_ESS}.
	 * 
	 * @return the value
	 */
	public int getProdToEss() {
		return this.getValue(PROD_TO_ESS);
	}

	/**
	 * Gets {@link Coefficient#PROD_TO_GRID}.
	 * 
	 * @return the value
	 */
	public int getProdToGrid() {
		return this.getValue(PROD_TO_GRID);
	}

	/**
	 * Gets {@link Coefficient#GRID_TO_CONS}.
	 * 
	 * @return the value
	 */
	public int getGridToCons() {
		return this.getValue(GRID_TO_CONS);
	}

	/**
	 * Gets {@link Coefficient#GRID_TO_ESS}.
	 * 
	 * @return the value
	 */
	public int getGridToEss() {
		return this.getValue(GRID_TO_ESS);
	}

	/**
	 * Gets {@link Coefficient#ESS_TO_CONS}.
	 * 
	 * @return the value
	 */
	public int getEssToCons() {
		return this.getValue(ESS_TO_CONS);
	}

	private int getValue(Coefficient coefficient) {
		return toInt(this.point[coefficient.ordinal()]);
	}

	/**
	 * Prints all {@link Coefficient}s and their values line by line.
	 */
	public void print() {
		for (var c : Coefficient.values()) {
			LOG.info(c.toCamelCase() + ": " + this.getValue(c));
		}
	}

	@Override
	public String toString() {
		return new StringBuilder("EnergyFlow[") //
				.append(Arrays.stream(Coefficient.values()) //
						.map(c -> new StringBuilder() //
								.append(c.toCamelCase()) //
								.append("=") //
								.append(this.getValue(c)) //
								.toString()) //
						.collect(joining(", "))) //
				.append("]") //
				.toString();
	}

	/**
	 * Models an EnergyFlow as a Linear Equation System with defined
	 * {@link Coefficient}s for GRID, ESS, CONS, etc.
	 */
	public static class Model {

		/**
		 * Generates a {@link EnergyFlow.Model} from a {@link OneSimulationContext} and
		 * a {@link Period}.
		 * 
		 * @param osc    the {@link OneSimulationContext}
		 * @param period the {@link Period}
		 * @return a new {@link EnergyFlow.Model}
		 */
		public static EnergyFlow.Model from(OneSimulationContext osc, Period period) {
			final int factor; // TODO replace with switch in Java 21
			if (period instanceof GlobalSimulationsContext.Period.Hour) {
				factor = 4;
			} else {
				factor = 1;
			}
			final var ess = osc.global.ess();
			final var grid = osc.global.grid();
			return new EnergyFlow.Model(//
					/* production */ period.production(), //
					/* consumption */ period.consumption(), //
					/* essMaxCharge */ min(ess.maxChargeEnergy() * factor, ess.totalEnergy() - osc.getEssInitial()), //
					/* essMaxDischarge */ min(ess.maxDischargeEnergy() * factor, osc.getEssInitial()), //
					/* gridMaxBuy */ grid.maxBuy() * factor, //
					/* gridMaxSell */ grid.maxSell() * factor);
		}

		public final int production;
		public final int consumption;
		public final int essMaxCharge;
		public final int essMaxDischarge;
		public final int gridMaxBuy;
		public final int gridMaxSell;

		private final List<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

		public Model(int production, int consumption, int essMaxCharge, int essMaxDischarge, int gridMaxBuy,
				int gridMaxSell) {
			this.production = production;
			this.consumption = consumption;
			this.essMaxCharge = essMaxCharge;
			this.essMaxDischarge = essMaxDischarge;
			this.gridMaxBuy = gridMaxBuy;
			this.gridMaxSell = gridMaxSell;

			this
					// Internal Relationships
					.addConstraint(c -> c // Sum
							.setCoefficient(PROD, 1) //
							.setCoefficient(ESS, 1) //
							.setCoefficient(GRID, 1) //
							.setCoefficient(CONS, -1) //
							.toLinearConstraint(EQ, 0)) //
					.addConstraint(c -> c // Distribute Production
							.setCoefficient(PROD, -1) //
							.setCoefficient(PROD_TO_CONS, 1) //
							.setCoefficient(PROD_TO_ESS, 1) //
							.setCoefficient(PROD_TO_GRID, 1) //
							.toLinearConstraint(EQ, 0)) //
					.addConstraint(b -> b // Distribute Consumption
							.setCoefficient(CONS, 1) //
							.setCoefficient(ESS_TO_CONS, -1) //
							.setCoefficient(GRID_TO_CONS, -1) //
							.setCoefficient(PROD_TO_CONS, -1) //
							.toLinearConstraint(EQ, 0)) //
					.addConstraint(b -> b // Distribute Grid
							.setCoefficient(GRID, -1) //
							.setCoefficient(PROD_TO_GRID, -1) //
							.setCoefficient(GRID_TO_CONS, 1) //
							.setCoefficient(GRID_TO_ESS, 1) //
							.toLinearConstraint(EQ, 0)) //
					.addConstraint(b -> b // Distribute ESS
							.setCoefficient(ESS, -1) //
							.setCoefficient(PROD_TO_ESS, -1) //
							.setCoefficient(ESS_TO_CONS, 1) //
							.setCoefficient(GRID_TO_ESS, -1) //
							.toLinearConstraint(EQ, 0)) //
					.addConstraint(b -> b // Only Positive PROD_TO_ESS
							.setCoefficient(PROD_TO_ESS, 1) //
							.toLinearConstraint(GEQ, 0)) //
					.addConstraint(b -> b // Only Positive PROD_TO_GRID
							.setCoefficient(PROD_TO_GRID, 1) //
							.toLinearConstraint(GEQ, 0)) //
					.addConstraint(b -> b // Only Positive ESS_TO_CONS
							.setCoefficient(ESS_TO_CONS, 1) //
							.toLinearConstraint(GEQ, 0)) //
					.addConstraint(b -> b // Only Positive GRID_TO_CONS
							.setCoefficient(GRID_TO_CONS, 1) //
							.toLinearConstraint(GEQ, 0))

					// Production & Consumption
					.addConstraint(c -> c //
							.setCoefficient(PROD, 1) //
							.toLinearConstraint(EQ, production)) //
					.addConstraint(c -> c //
							.setCoefficient(CONS, 1) //
							.toLinearConstraint(EQ, consumption))
					.addConstraint(b -> b // PROD_TO_CONS
							.setCoefficient(PROD_TO_CONS, 1) //
							.toLinearConstraint(EQ, min(production, consumption)))

					// ESS Max Charge/Discharge
					.addConstraint(c -> c //
							.setCoefficient(ESS, 1) //
							.toLinearConstraint(GEQ, -essMaxCharge)) //
					.addConstraint(c -> c //
							.setCoefficient(ESS, 1) //
							.toLinearConstraint(LEQ, essMaxDischarge)) //
					// Grid Max Buy/Sell
					.addConstraint(c -> c //
							.setCoefficient(GRID, 1) //
							.toLinearConstraint(LEQ, gridMaxBuy)) //
					.addConstraint(c -> c //
							.setCoefficient(GRID, 1) //
							.toLinearConstraint(GEQ, -gridMaxSell));
		}

		/**
		 * Sets the {@link Coefficient#ESS} Charge/Discharge Energy to the given value,
		 * while making sure the value fits in the active constraints.
		 * 
		 * @param value the value
		 * @return true on success; false otherwise
		 */
		public boolean setEss(int value) {
			return this.setFittingCoefficientValue(ESS, EQ, value);
		}

		/**
		 * Limits the {@link Coefficient#ESS} Charge Energy to the given value, while
		 * making sure the value fits in the active constraints.
		 * 
		 * @param value the value
		 * @return true on success; false otherwise
		 */
		public boolean setEssMaxCharge(int value) {
			return this.setFittingCoefficientValue(ESS, GEQ, -value);
		}

		/**
		 * Limits the {@link Coefficient#ESS} Discharge Energy to the given value, while
		 * making sure the value fits in the active constraints.
		 * 
		 * @param value the value
		 * @return true on success; false otherwise
		 */
		public boolean setEssMaxDischarge(int value) {
			return this.setFittingCoefficientValue(ESS, LEQ, value);
		}

		/**
		 * Limits the {@link Coefficient#GRID} Buy Energy to the given value, while
		 * making sure the value fits in the active constraints.
		 * 
		 * @param value the value
		 * @return true on success; false otherwise
		 */
		public boolean setGridMaxBuy(int value) {
			return this.setFittingCoefficientValue(ESS, LEQ, value);
		}

		/**
		 * Limits the {@link Coefficient#GRID} Sell Energy to the given value, while
		 * making sure the value fits in the active constraints.
		 * 
		 * @param value the value
		 * @return true on success; false otherwise
		 */
		public boolean setGridMaxSell(int value) {
			return this.setFittingCoefficientValue(ESS, GEQ, -value);
		}

		/**
		 * Prints a table with all constraints.
		 */
		public void logConstraints() {
			{
				var b = new StringBuilder();
				for (var coefficient : Coefficient.values()) {
					b.append(String.format("%s ", coefficient.toCamelCase()));
				}
				LOG.info(b.toString());
			}
			for (var constraint : this.constraints) {
				var b = new StringBuilder();
				var equation = constraint.getCoefficients();
				for (var coefficient : Coefficient.values()) {
					b.append(String.format("% " + coefficient.name().length() + ".0f ",
							equation.getEntry(coefficient.ordinal())));
				}
				b.append(String.format("%2s % 10.0f", constraint.getRelationship(), constraint.getValue()));
				LOG.info(b.toString());
			}
		}

		/**
		 * Prints min/max values for a {@link Coefficient}.
		 * 
		 * @param coefficient the {@link Coefficient}
		 */
		public void logMinMaxValues(Coefficient coefficient) {
			var values = this.calculateMinMaxValues(coefficient);
			var min = values[0];
			var max = values[1];
			LOG.info(String.format("%-12s % 5.0f % 5.0f %s", coefficient.toCamelCase(), min, max,
					min == max ? "fixed" : ""));
		}

		/**
		 * Prints a table with all constraints.
		 */
		public void logMinMaxValues() {
			LOG.info(String.format("%-12s %5s %5s", "Coefficient", "Min", "Max"));
			for (var coefficient : Coefficient.values()) {
				this.logMinMaxValues(coefficient);
			}
		}

		@Override
		public String toString() {
			return "EnergyFlow.Model[" //
					+ Arrays.stream(Coefficient.values()) //
							.map(coefficient -> {
								var values = this.calculateMinMaxValues(coefficient);
								var min = values[0];
								var max = values[1];
								var b = new StringBuilder().append(coefficient.toCamelCase()) //
										.append("=") //
										.append(min);
								if (min == max) {
									b //
											.append("|fixed");
								} else {
									b //
											.append("|") //
											.append(max); //
								}
								return b.toString();
							}) //
							.collect(joining(",")) //
					+ "]";
		}

		/**
		 * Calculates the current Min and Max values for a given {@link Coefficient}.
		 * 
		 * @param coefficient the {@link Coefficient}
		 * @return result[0] is the Min value; result[1] is the Max value
		 */
		private double[] calculateMinMaxValues(Coefficient coefficient) {
			final double[] result = new double[2];
			try {
				result[0] = this.getExtremeCoefficientValue(coefficient, MINIMIZE);
			} catch (MathIllegalStateException e) {
				result[0] = Double.NaN;
			}
			try {
				result[1] = this.getExtremeCoefficientValue(coefficient, MAXIMIZE);
			} catch (MathIllegalStateException e) {
				result[1] = Double.NaN;
			}
			return result;
		}

		private EnergyFlow.Model addConstraint(Function<Coefficients, LinearConstraint> coefficients) {
			this.constraints.add(coefficients.apply(new Coefficients()));
			return this;
		}

		/**
		 * Gets the minimum or maximum allowed value for the given {@link Coefficient}.
		 * 
		 * @param coefficient the {@link Coefficient}
		 * @param goalType    the {@link GoalType}
		 * @return the value
		 * @throws MathIllegalStateException if this {@link EnergyFlow.Model} is
		 *                                   unsolvable
		 */
		public double getExtremeCoefficientValue(Coefficient coefficient, GoalType goalType)
				throws MathIllegalStateException {
			return solve(goalType, this.constraints, Coefficients.create() //
					.setCoefficient(coefficient, 1) //
					.toLinearObjectiveFunction(0)) //
					.getPointRef()[coefficient.ordinal()];
		}

		/**
		 * Adds a {@link LinearConstraint} that sets the given {@link Coefficient} to
		 * the minimum or maximum allowed value.
		 * 
		 * @param coefficient the {@link Coefficient}
		 * @param goalType    the {@link GoalType}
		 */
		public void setExtremeCoefficientValue(Coefficient coefficient, GoalType goalType) {
			try {
				var value = this.getExtremeCoefficientValue(coefficient, goalType);
				this.setCoefficientValue(coefficient, value);
			} catch (MathIllegalStateException e) {
				LOG.warn("[setExtremeCoefficientValue] " //
						+ "Unable to " + goalType + " " + coefficient + ": " + e.getMessage() + " " //
						+ this.toString());
			}
		}

		/**
		 * Adds a {@link LinearConstraint} that sets the given {@link Coefficient} to
		 * the given value, while making sure the value fits in the active constraints.
		 * 
		 * @param coefficient  the {@link Coefficient}
		 * @param relationship the {@link Relationship}l
		 * @param value        the value
		 * @return true on success; false otherwise
		 */
		public boolean setFittingCoefficientValue(Coefficient coefficient, Relationship relationship, double value) {
			// Fit to MIN value
			try {
				var min = this.getExtremeCoefficientValue(coefficient, MINIMIZE);
				if (value <= min) {
					this.setCoefficientValue(coefficient, relationship, min);
					return true;
				}
			} catch (MathIllegalStateException e) {
				LOG.warn("[setFittingCoefficientValue] " //
						+ "Unable to MINIMIZE " + coefficient + ": " + e.getMessage() + " " //
						+ this.toString());
				return false;
			}

			// Fit to MAX value
			try {
				var max = this.getExtremeCoefficientValue(coefficient, MAXIMIZE);
				if (value > max) {
					this.setCoefficientValue(coefficient, relationship, max);
					return true;
				}
			} catch (MathIllegalStateException e) {
				LOG.warn("[setFittingCoefficientValue] " //
						+ "Unable to MAXIMIZE " + coefficient + ": " + e.getMessage() + " " //
						+ this.toString());
				return false;
			}

			// Apply coefficient value
			this.setCoefficientValue(coefficient, relationship, value);
			return true;
		}

		/**
		 * Adds a {@link LinearConstraint} that sets the given {@link Coefficient} to
		 * the given value.
		 * 
		 * @param coefficient the {@link Coefficient}
		 * @param value       the value
		 */
		private void setCoefficientValue(Coefficient coefficient, double value) {
			this.setCoefficientValue(coefficient, Relationship.EQ, value);
		}

		/**
		 * Adds a {@link LinearConstraint} that constrains the given {@link Coefficient}
		 * to the given value and {@link Relationship}.
		 * 
		 * @param coefficient  the {@link Coefficient}
		 * @param relationship the {@link Relationship}
		 * @param value        the value
		 */
		private void setCoefficientValue(Coefficient coefficient, Relationship relationship, double value) {
			this.addConstraint(c -> c //
					.setCoefficient(coefficient, 1) //
					.toLinearConstraint(relationship, value));
		}

		/**
		 * Solves the {@link EnergyFlow.Model} and returns an {@link EnergyFlow}.
		 * 
		 * @return the {@link EnergyFlow}; null if this {@link EnergyFlow.Model} is
		 *         unsolvable
		 */
		public EnergyFlow solve() {
			final double ess;
			try {
				ess = this.getExtremeCoefficientValue(ESS, MAXIMIZE);
			} catch (MathIllegalStateException e) {
				LOG.warn("[solve] " //
						+ "Unable to MAXIMIZE ESS: " + e.getMessage() + " " //
						+ this.toString());
				return null;
			}
			if (ess <= 0) {
				// ESS Charge or Zero; GRID_TO_ESS must be >= 0
				this.setFittingCoefficientValue(GRID_TO_ESS, GEQ, 0);
				this.setFittingCoefficientValue(ESS_TO_CONS, EQ, 0);
			}
			if (ess >= 0) {
				// ESS Discharge or Zero
				// Maximize ESS_TO_CONS (1st prio: PROD_TO_CONS; 3rd prio: GRID_TO_CONS)
				final double essMax;
				try {
					essMax = this.getExtremeCoefficientValue(ESS_TO_CONS, MAXIMIZE);
				} catch (MathIllegalStateException e) {
					LOG.warn("[solve] " //
							+ "Unable to MAXIMIZE ESS_TO_CONS: " + e.getMessage() + " " //
							+ this.toString());
					return null;
				}
				this.setCoefficientValue(ESS_TO_CONS, min(essMax, ess));
			}

			var coefficients = initializeCoefficients();
			Arrays.fill(coefficients, 1);
			try {
				return new EnergyFlow(solve(MINIMIZE, this.constraints, new LinearObjectiveFunction(coefficients, 0)));
			} catch (MathIllegalStateException e) {
				LOG.warn("[solve] " //
						+ "Unable to solve EnergyFlow.Model: " + e.getMessage() + " " //
						+ this.toString());
				return null;
			}
		}

		/**
		 * Solves the linear equation system.
		 * 
		 * @param goalType          {@link GoalType#MINIMIZE} or
		 *                          {@link GoalType#MAXIMIZE} the objective function
		 * @param constraints       the {@link LinearConstraint}s
		 * @param objectiveFunction the {@link LinearObjectiveFunction}
		 * @return the {@link PointValuePair}
		 * @throws MathIllegalStateException if this {@link EnergyFlow.Model} is
		 *                                   unsolvable
		 */
		private static PointValuePair solve(GoalType goalType, Collection<LinearConstraint> constraints,
				LinearObjectiveFunction objectiveFunction) throws MathIllegalStateException {
			return new SimplexSolver().optimize(//
					objectiveFunction, //
					new LinearConstraintSet(constraints), //
					goalType);
		}
	}

	/**
	 * Helper class to provides a Builder-Pattern like way to create a coefficients
	 * array suitable for a {@link LinearConstraint} or
	 * {@link LinearObjectiveFunction}.
	 */
	private static class Coefficients {

		private static Coefficients create() {
			return new Coefficients();
		}

		private final double[] coefficients;

		private Coefficients() {
			this.coefficients = initializeCoefficients();
		}

		private Coefficients setCoefficient(Coefficient coefficient, int value) {
			this.coefficients[coefficient.ordinal()] = value;
			return this;
		}

		private LinearConstraint toLinearConstraint(Relationship relationship, double value) {
			return new LinearConstraint(this.coefficients, relationship, value);
		}

		private LinearObjectiveFunction toLinearObjectiveFunction(int constantTerm) {
			return new LinearObjectiveFunction(this.coefficients, constantTerm);
		}

	}

	private static double[] initializeCoefficients() {
		return new double[Coefficient.values().length];
	}

	private static int toInt(double value) {
		return (int) Math.round(value);
	}
}
