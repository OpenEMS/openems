package io.openems.edge.ess.core.power;
//package io.openems.edge.ess.core.power;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.apache.commons.math3.optim.linear.LinearConstraint;
//import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
//import org.apache.commons.math3.optim.linear.Relationship;
//import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
//
//import io.openems.common.utils.IntUtils;
//import io.openems.common.utils.IntUtils.Round;
//import io.openems.edge.ess.api.ManagedAsymmetricEss;
//import io.openems.edge.ess.api.ManagedSymmetricEss;
//import io.openems.edge.ess.api.MetaEss;
//import io.openems.edge.ess.power.api.Constraint;
//import io.openems.edge.ess.power.api.ConstraintType;
//import io.openems.edge.ess.power.api.Phase;
//import io.openems.edge.ess.power.api.Pwr;
//
//public class Utils {
//
//	private static String coefficientsToString(double[] coefficients) {
//		StringBuilder b = new StringBuilder();
//		for (int i = 0; i < coefficients.length; i++) {
//			double x = coefficients[i];
//			b.append(String.format("%+.1f*", x));
//			switch (i % 2) {
//			case 0:
//				b.append("p");
//				break;
//			case 1:
//				b.append("q");
//				break;
//			}
//			b.append((i / 2) % 3 + 1);
//			if (i < coefficients.length - 1) {
//				b.append(" ");
//			}
//		}
//		return b.toString();
//	}
//
//	/**
//	 * Helper: Copies the LinearConstraints list to an array
//	 * 
//	 * @param constraints
//	 * @return
//	 */
//	public static LinearConstraint[] copyToArray(List<LinearConstraint> constraints) {
//		LinearConstraint[] c = new LinearConstraint[constraints.size()];
//		for (int i = 0; i < constraints.size(); i++) {
//			c[i] = constraints.get(i);
//		}
//		return c;
//	}
//
//	public static String linearConstraintToString(LinearConstraint constraint, String note) {
//		StringBuilder b = new StringBuilder();
//		b.append(String.format("%-30s ", note));
//		double[] coefficients = constraint.getCoefficients().toArray();
//		b.append(coefficientsToString(coefficients));
//		b.append(" " + constraint.getRelationship().toString() + " " + String.format("%.1f", constraint.getValue()));
//		return b.toString();
//	}
//
//	public static String objectiveFunctionToString(LinearObjectiveFunction function, GoalType goalType) {
//		StringBuilder b = new StringBuilder();
//		b.append(String.format("%-30s ", "Objective Function"));
//		double[] coefficients = function.getCoefficients().toArray();
//		b.append(coefficientsToString(coefficients));
//		b.append(" -> " + goalType.toString());
//		return b.toString();
//	}
//
//	/**
//	 * Round values to accuracy of inverter; following this logic:
//	 *
//	 * On Discharge (Power > 0)
//	 *
//	 * <ul>
//	 * <li>if SoC > 50 %: round up (more discharge)
//	 * <li>if SoC < 50 %: round down (less discharge)
//	 * </ul>
//	 *
//	 * On Charge (Power < 0)
//	 *
//	 * <ul>
//	 * <li>if SoC > 50 %: round down (less charge)
//	 * <li>if SoC < 50 %: round up (more discharge)
//	 * </ul>
//	 */
//	public static int roundToInverterPrecision(ManagedSymmetricEss ess, double value) {
//		Round round = Round.DOWN;
//		int precision = ess.getPowerPrecision();
//		int soc = ess.getSoc().value().orElse(0);
//
//		if (value > 0 && soc > 50 || value < 0 && soc < 50) {
//			round = Round.UP;
//		}
//
//		return IntUtils.roundToPrecision((float) value, round, precision);
//	}
//
//	public static String solutionToString(double[] solution) {
//		StringBuilder b = new StringBuilder();
//		for (int i = 0; i < solution.length; i++) {
//			double x = solution[i];
//			switch (i % 2) {
//			case 0:
//				b.append("p");
//				break;
//			case 1:
//				b.append("q");
//				break;
//			}
//			b.append((i / 2) % 3 + 1 + String.format(" = % .1f", x));
//			if (i < solution.length - 1) {
//				b.append(String.format("%n"));
//			}
//		}
//		return b.toString();
//	}
//
//	public static void sumSolutions(ManagedSymmetricEss ess, Map<ManagedSymmetricEss, SymmetricSolution> solutions,
//			AtomicInteger activePower, AtomicInteger reactivePower, AtomicInteger activePowerL1,
//			AtomicInteger reactivePowerL1, AtomicInteger activePowerL2, AtomicInteger reactivePowerL2,
//			AtomicInteger activePowerL3, AtomicInteger reactivePowerL3) {
//		if (ess instanceof MetaEss) {
//			for (ManagedSymmetricEss subEss : ((MetaEss) ess).getEsss()) {
//				Utils.sumSolutions(subEss, solutions, activePower, reactivePower, activePowerL1, reactivePowerL1,
//						activePowerL2, reactivePowerL2, activePowerL3, reactivePowerL3);
//			}
//		} else {
//			SymmetricSolution ss = solutions.get(ess);
//			if (ss != null) {
//				activePower.addAndGet(ss.getActivePower());
//
//				if (ess instanceof ManagedAsymmetricEss && ss instanceof AsymmetricSolution) {
//					AsymmetricSolution as = (AsymmetricSolution) ss;
//					activePowerL1.addAndGet(as.getActivePowerL1());
//					reactivePowerL1.addAndGet(as.getReactivePowerL1());
//					activePowerL2.addAndGet(as.getActivePowerL2());
//					reactivePowerL2.addAndGet(as.getReactivePowerL2());
//					activePowerL3.addAndGet(as.getActivePowerL3());
//					reactivePowerL3.addAndGet(as.getReactivePowerL3());
//				}
//			}
//		}
//	}
//
//	/**
//	 * Create SymmetricSolution objects for easier handling of a Solution
//	 * 
//	 * @param data
//	 * @param solution
//	 * @return
//	 */
//	public static Map<ManagedSymmetricEss, SymmetricSolution> toSolutions(Data data, double[] solution) {
//		Map<ManagedSymmetricEss, SymmetricSolution> solutions = new HashMap<>();
//		data.realEsss.forEach(ess -> {
//			int i = data.getEssIndex(ess);
//
//			if (ess instanceof ManagedAsymmetricEss) {
//				/*
//				 * ManagedAsymmetricEss
//				 */
//				solutions.put(ess, //
//						new AsymmetricSolution((ManagedAsymmetricEss) ess, //
//								Utils.roundToInverterPrecision(ess,
//										solution[i + Phase.L1.getOffset() + Pwr.ACTIVE.getOffset()]), //
//								Utils.roundToInverterPrecision(ess,
//										solution[i + Phase.L1.getOffset() + Pwr.REACTIVE.getOffset()]), //
//								Utils.roundToInverterPrecision(ess,
//										solution[i + Phase.L2.getOffset() + Pwr.ACTIVE.getOffset()]), //
//								Utils.roundToInverterPrecision(ess,
//										solution[i + Phase.L2.getOffset() + Pwr.REACTIVE.getOffset()]), //
//								Utils.roundToInverterPrecision(ess,
//										solution[i + Phase.L3.getOffset() + Pwr.ACTIVE.getOffset()]), //
//								Utils.roundToInverterPrecision(ess,
//										solution[i + Phase.L3.getOffset() + Pwr.REACTIVE.getOffset()])));
//			} else {
//				/*
//				 * ManagedSymmetricEss
//				 */
//				solutions.put(ess, //
//						new SymmetricSolution(ess, //
//								Utils.roundToInverterPrecision(ess, //
//										solution[i + Phase.L1.getOffset() + Pwr.ACTIVE.getOffset()] //
//												+ solution[i + Phase.L2.getOffset() + Pwr.ACTIVE.getOffset()] //
//												+ solution[i + Phase.L3.getOffset() + Pwr.ACTIVE.getOffset()]), //
//								Utils.roundToInverterPrecision(ess, //
//										solution[i + Phase.L1.getOffset() + Pwr.REACTIVE.getOffset()] //
//												+ solution[i + Phase.L2.getOffset() + Pwr.REACTIVE.getOffset()] //
//												+ solution[i + Phase.L3.getOffset() + Pwr.REACTIVE.getOffset()])));
//			}
//		});
//		return solutions;
//	}
//
//	/**
//	 * Create a Simple Constraint
//	 * 
//	 * @param ess
//	 * @param type
//	 * @param phase
//	 * @param pwr
//	 * @param relationship
//	 * @param value
//	 * @return
//	 */
//	public static Constraint createSimpleConstraint(ManagedSymmetricEss ess, ConstraintType type, Phase phase, Pwr pwr,
//			Relationship relationship, int value) {
//		return new Constraint( //
//				type, new Coefficient[] { //
//						new Coefficient(ess, phase, pwr, 1) }, //
//				relationship, //
//				value);
//	}
//
//	public static boolean coefficientIsCoveredBy(Coefficient c, ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
//		if (ess instanceof MetaEss) {
//			for (ManagedSymmetricEss subEss : ((MetaEss) ess).getEsss()) {
//				if (Utils.coefficientIsCoveredBy(c, subEss, phase, pwr)) {
//					return true;
//				}
//			}
//			return false;
//		} else {
//			if (c.getEss() == ess //
//					&& c.getPwr() == pwr //
//					&& (phase == Phase.ALL //
//							|| c.getPhase() == phase //
//					)) {
//				return true;
//			}
//			return false;
//		}
//	}
//
//}
