package io.openems.edge.ess.core.power.optimizers;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.core.power.solver.ConstraintSolver;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class OperateClusterAtMaximumEfficiency {

	static enum TypeOfcluster {
		HOMOGENOUS, //
		HETEROGENOUS
	}

	static enum OperationMode {
		ONE, //
		TWO, //
		THREE
	}

	public static PointValuePair apply(Coefficients coefficients, TargetDirection targetDirection, //
			List<Inverter> allInverters, List<Inverter> targetInverters, //
			List<Constraint> allConstraints, List<Inverter> invs, //
			List<ManagedSymmetricEss> esss //
	) throws OpenemsException {

		List<Constraint> constraints = new ArrayList<>(allConstraints);
		OperationMode operationMode = null;

		/*
		 * All of Inverter and its Soc map
		 */
		LinkedHashMap<Inverter, Integer> INV_SOC_MAP = Utils.getInvAndSocMap(invs, esss);

		/*
		 * All of Inverter and its Ess
		 */
		LinkedHashMap<Inverter, ManagedSymmetricEss> INV_ESS_MAP = Utils.getInvAndEssMap(invs, esss);

		/*
		 * The Power set value
		 */
		double POWER_SET = Utils.getThePowerWhichIsToBeSet(allConstraints);

		TypeOfcluster typeOfCluster = Utils.GetTheTypeOfCluster(INV_SOC_MAP);

		switch (typeOfCluster) {
		case HETEROGENOUS:
			switch (targetDirection) {
			case CHARGE:
				double PmaxCharge = Utils.getPMaxChargeOfCluster(INV_ESS_MAP);
				operationMode = Utils.getOperationMode(Math.abs(PmaxCharge), Math.abs(POWER_SET), INV_ESS_MAP.size());

				switch (operationMode) {
				case ONE:
					break;
				case TWO:
					
					
					break;
				case THREE:
					break;
				}
				
				
				
				
				
				break;
			case DISCHARGE:
				break;
			case KEEP_ZERO:
				break;

			}

			break;
		case HOMOGENOUS:
			switch (targetDirection) {
			case CHARGE:
				// Get PmaxCharge of cluster during charging
				double PmaxCharge = Utils.getPMaxChargeOfCluster(INV_ESS_MAP);

				// Get the operation Mode
				operationMode = Utils.getOperationMode(Math.abs(PmaxCharge), Math.abs(POWER_SET), INV_ESS_MAP.size());

				switch (operationMode) {
				case ONE:

					// Charging ess with the lowest SOC
					// The Inverter which has the lowest SOC
					List<Inverter> inverters = new ArrayList<>(allInverters);
					Inverter lowestSocInverter = Utils.getLowestSocInverter(INV_SOC_MAP);

					System.out.println(INV_SOC_MAP);
					// System.out.println(lowestSocInverter.getEssId());

					Map<Inverter, Double> Weights = new HashMap<>();
					for (Inverter inv : allInverters) {
						if (inv == lowestSocInverter) {
							Weights.put(inv, 1.0);
						} else {
							Weights.put(inv, 0.0);
						}

					}

					System.out.println(inverters);
					for (Inverter inv : inverters) {

						if (inv == lowestSocInverter) {
							constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
									lowestSocInverter.toString() + ": ActivePower next weight = 0", //
									lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE,
									Relationship.EQUALS, -1));
						} else {

							constraints.add(ConstraintUtil.createSimpleConstraint(
									
									new Coefficients(),
									
									 //
									inv.toString() + ": ActivePower next weight = 0", //
									inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, 1));
						}
					}

//					constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//							lowestSocInverter.toString() + ": ActivePower next weight = 0", //
//							lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -1 * POWER_SET));
//					
//					constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//							lowestSocInverter.toString() + ": ActivePower next weight = 0", //
//							lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -1 * POWER_SET));
//					
//					constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//							lowestSocInverter.toString() + ": ActivePower next weight = 0", //
//							lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -1 * POWER_SET));
//					
//					constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//							lowestSocInverter.toString() + ": ActivePower next weight = 0", //
//							lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -1 * POWER_SET));
//					
//					constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//							lowestSocInverter.toString() + ": ActivePower next weight = 0", //
//							lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -1 * POWER_SET));
//					
//					constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//							lowestSocInverter.toString() + ": ActivePower next weight = 0", //
//							lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -1 * POWER_SET));
//					
//					constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//							lowestSocInverter.toString() + ": ActivePower next weight = 0", //
//							lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -1 * POWER_SET));
//					
//					constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//							lowestSocInverter.toString() + ": ActivePower next weight = 0", //
//							lowestSocInverter.getEssId(), lowestSocInverter.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, -1 * POWER_SET));

//					for (Entry<Inverter, Double> entry : Weights.entrySet()) {
//						if (entry.getValue() == 0) { // might fail... compare double to zero
//							var inv = entry.getKey();
//							constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//									inv.toString() + ": ActivePower next weight = 0", //
//									inv.getEssId(), inv.getPhase(), Pwr.ACTIVE, Relationship.EQUALS, 0));
//							constraints.add(ConstraintUtil.createSimpleConstraint(coefficients, //
//									inv.toString() + ": ReactivePower next weight = 0", //
//									inv.getEssId(), inv.getPhase(), Pwr.REACTIVE, Relationship.EQUALS, 0));
//							inverters.remove(inv);
//						}
//					}

					break;
					
				case TWO:
					Inverter firstKey = null;
					var twetypercentofMaxKVA = 0.0;
					
					firstKey = INV_ESS_MAP.keySet().stream().findFirst().get();

					twetypercentofMaxKVA = INV_ESS_MAP.get(firstKey).getMaxApparentPower().get() * 0.2;
					double  n ;
					if (POWER_SET <=   (PmaxCharge/2)){
						 n = INV_ESS_MAP.size() / 2;
					}else {
						 n = INV_ESS_MAP.size() ;
					}
					List<Inverter> OnlyWantedInv = new ArrayList<>();
					OnlyWantedInv = Utils.getListForCharging(INV_ESS_MAP, (int) n);
					
					try {
						var invA = OnlyWantedInv.get(0);
						for (var j = 1; j < OnlyWantedInv.size(); j++) {
							var invB = OnlyWantedInv.get(j);
							constraints.add(new Constraint(
									invA.toString() + "|" + invB.toString() + ": distribute ActivePower equally",
									new LinearCoefficient[] {
											new LinearCoefficient(
													coefficients.of(invA.getEssId(), invA.getPhase(), Pwr.ACTIVE), 1),
											new LinearCoefficient(
													coefficients.of(invB.getEssId(), invB.getPhase(), Pwr.ACTIVE), -1) },
									Relationship.EQUALS, 0));
							constraints.add(new Constraint(
									invA.toString() + "|" + invB.toString() + ": distribute ReactivePower equally",
									new LinearCoefficient[] {
											new LinearCoefficient(
													coefficients.of(invA.getEssId(), invA.getPhase(), Pwr.REACTIVE), 1),
											new LinearCoefficient(
													coefficients.of(invB.getEssId(), invB.getPhase(), Pwr.REACTIVE), -1) },
									Relationship.EQUALS, 0));
						}
					} catch (Exception e) {
						return null;
					}
					
					
					break;
				case THREE:
					break;
			

				}

				break;
			case DISCHARGE:
				// Get PmaxDisharge of cluster during Discharging
				var PmaxDischarge = Utils.getPMaxChargeOfCluster(INV_ESS_MAP);
				// Get the operation mode
				operationMode = Utils.getOperationMode(PmaxDischarge, POWER_SET, INV_ESS_MAP.size());

				switch (operationMode) {
				case ONE:
					break;
				case THREE:
					break;
				case TWO:
					break;

				}
				break;
			case KEEP_ZERO:
				break;

			}

			break;
		}

		return null;

	}

	static class Utils {
		
		private static List<Inverter> getListForCharging(LinkedHashMap<Inverter, ManagedSymmetricEss> map, int i) {

			List<Inverter> OnlyWantedInv = new ArrayList<Inverter>();
			int sizeOfMap = map.size();

			int x = sizeOfMap - i;
			System.out.println(x);
			int counter = 1;

			for (Map.Entry<Inverter, ManagedSymmetricEss> entry : map.entrySet()) {
				if (counter > x) {
					// System.out.println("Key : " + entry.getKey() + " value : " +
					// entry.getValue());

					OnlyWantedInv.add(entry.getKey());

				}
				counter++;
			}

			return OnlyWantedInv;
		}

		/**
		 * Get the inverter with the lowest SOC
		 * 
		 * @param iNV_SOC_MAP
		 * @return Inverter
		 */
		private static Inverter getLowestSocInverter(LinkedHashMap<Inverter, Integer> iNV_SOC_MAP) {
			int lowestSoc = Collections.min(iNV_SOC_MAP.values());

			for (Map.Entry<Inverter, Integer> entry : iNV_SOC_MAP.entrySet()) { // Itrate through hashmap
				if (entry.getValue() == lowestSoc) {

					System.out.println(entry.getKey()); // Print the Inverter with min Soc
					return entry.getKey();
				}
			}
			return null;
		}

		/**
		 * return the operation Mode
		 * 
		 * @param pmaxCharge
		 * @param pOWER_SET
		 * @return operationMode {@code OperationMode}
		 */
		private static OperationMode getOperationMode(double pmaxCharge, double pOWER_SET, int numOfInverters) {

			double fortyPercentPower = 0.40 * (pmaxCharge / numOfInverters);

			if (0 < pOWER_SET && pOWER_SET < fortyPercentPower) {
				return OperationMode.ONE;
			} else if (fortyPercentPower < pOWER_SET && pOWER_SET < pmaxCharge) {
				return OperationMode.TWO;
			} else {
				return OperationMode.THREE;
			}

		}

		/**
		 * get Inverter and its Soc as a map.
		 * 
		 * @param invs.
		 * @param esss.
		 * @return Map of Inverter and Soc.
		 */
		public static LinkedHashMap<Inverter, Integer> getInvAndSocMap(List<Inverter> invs,
				List<ManagedSymmetricEss> esss) {

			List<ManagedSymmetricEss> updatesEss = new ArrayList<>();
			LinkedHashMap<Inverter, Integer> InvSocMap = new LinkedHashMap<Inverter, Integer>(16, .75f, false);

			// remove the Cluster
			for (ManagedSymmetricEss ess : esss) {
				if (ess.id() != "ess0") {
					updatesEss.add(ess);
				}
			}

			for (Inverter in : invs) {
				for (ManagedSymmetricEss mE : updatesEss) {
					if (in.getEssId() == mE.id()) {
						InvSocMap.put(in, mE.getSoc().get());
					}
				}
			}
			return InvSocMap;
		}

		/**
		 * Return the Power set value.
		 * 
		 * @param allConstraints
		 * @return PowerSet
		 */
		private static double getThePowerWhichIsToBeSet(List<Constraint> allConstraints) {

			for (var c : allConstraints) {
				if (c.getCoefficients().length == 1 && //
						c.getCoefficients()[0].getCoefficient().getPwr() == Pwr.ACTIVE && //
						c.getRelationship() == Relationship.EQUALS) {

					return c.getValue().get();

				}
			}
			return 0d;
		}

		/**
		 * Get the Max Charge power
		 * 
		 * @param iNV_ESS_MAP
		 * @return PmaxCharge
		 */
		public static double getPMaxChargeOfCluster(LinkedHashMap<Inverter, ManagedSymmetricEss> iNV_ESS_MAP) {

			double PmaxCharge = 0;
			for (Map.Entry<Inverter, ManagedSymmetricEss> entry : iNV_ESS_MAP.entrySet()) {
				var maxChargePower = entry.getValue() //
						.getPower() //
						.getMinPower(entry.getValue(), Phase.ALL, Pwr.ACTIVE);

				PmaxCharge -= maxChargePower;

			}
			return PmaxCharge;
		}

		/**
		 * Gets the the type of cluster
		 * 
		 * @param invSocMap
		 * @return TypeOfcluster {@code TypeOfcluster}
		 */
		public static TypeOfcluster GetTheTypeOfCluster(LinkedHashMap<Inverter, Integer> invSocMap) {

			int differenceSoc = Collections.max(invSocMap.values()) - Collections.min(invSocMap.values());

			/*
			 * 5 is the Soc value in percentage, This is actually set as static variable.
			 * Ask Justin about why 5 is set?
			 */
			if (differenceSoc > 5) {
				return TypeOfcluster.HETEROGENOUS;
			} else {
				return TypeOfcluster.HOMOGENOUS;
			}

		}

		/**
		 * get Inver and its ess as a map
		 * 
		 * @param invs
		 * @param esss
		 * @return
		 */

		public static LinkedHashMap<Inverter, ManagedSymmetricEss> getInvAndEssMap(List<Inverter> invs,
				List<ManagedSymmetricEss> esss) {
			List<ManagedSymmetricEss> updatesEss = new ArrayList<>();
			LinkedHashMap<Inverter, ManagedSymmetricEss> InvSocMap = new LinkedHashMap<Inverter, ManagedSymmetricEss>(
					16, .75f, true);

			// remove the Cluster
			for (ManagedSymmetricEss ess : esss) {
				if (ess.id() != "ess0") {
					updatesEss.add(ess);

				}
			}

			for (Inverter in : invs) {
				for (ManagedSymmetricEss mE : updatesEss) {
					if (in.getEssId() == mE.id()) {
						InvSocMap.put(in, mE);
					}
				}
			}
			return InvSocMap;

		}
	}

}
