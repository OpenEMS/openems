package io.openems.edge.ess.core.power.optimizers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.math3.optim.PointValuePair;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.data.TargetDirection;
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

	public static PointValuePair apply(Coefficients coefficients, //
			TargetDirection targetDirection, //
			List<Inverter> allInverters, //
			List<Inverter> targetInverters, //
			List<Constraint> allConstraints, //
			List<Inverter> invs, //
			List<ManagedSymmetricEss> esss //
	) {

		OperationMode Om = null;

//		var PmaxDischarge = 0;
//		var PmaxCharge = 0;
//		var PmaxCluster = 0;
//		int targetSoc = 50;
		List<Constraint> constraints = new ArrayList<>(allConstraints);

		/*
		 * The Power set value
		 */
		double powerSet = getThePowerWhichIsToBeSet(allConstraints);
		/*
		 * All of Inverter and its Soc map
		 */
		LinkedHashMap<Inverter, Integer> InvSocMap = WeightsUtil.getInvAndSocMap(invs, esss);
		/*
		 * All of Inverter and its Ess
		 */
		LinkedHashMap<Inverter, ManagedSymmetricEss> InvEssMap = WeightsUtil.getInvAndEssMap(invs, esss);
		LinkedHashMap<Inverter, Integer> OnlyWantedInvSocMap = new LinkedHashMap<>();
		LinkedHashMap<Inverter, ManagedSymmetricEss> OnlyWantedInvEssMap = new LinkedHashMap<>();
		LinkedHashMap<Inverter, ManagedSymmetricEss> UnWantedInvEssMap = new LinkedHashMap<>();

		System.out.println(InvSocMap);

		TypeOfcluster typeOfCluster = GetTheTypeOfCluster(InvSocMap);

		switch (typeOfCluster) {
		case HETEROGENOUS:

			break;
		case HOMOGENOUS:
			// Get the Target direction
			switch (targetDirection) {
			case CHARGE:
				// get max apparent power, assumption all the inverters are same
				var twetypercentofMaxKVA = InvEssMap.get(0).getMaxApparentPower().get() * 0.2;
				var n = Math.floor(Math.abs(powerSet / twetypercentofMaxKVA));
				List<Inverter> OnlyWantedInv = getListForCharging(InvEssMap, (int) n);

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

			case DISCHARGE:

				// get max apparent power, assumption all the inverters are same
				twetypercentofMaxKVA = InvEssMap.get(0).getMaxApparentPower().get() * 0.2;
				n = Math.floor(Math.abs(powerSet / twetypercentofMaxKVA));
				OnlyWantedInv = getListForDisCharging(InvEssMap, (int) n);

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

			case KEEP_ZERO:
				// This of this later , this mainly has the removed inverters
				// Use the unwanted ess

				break;
			}

//			for (Map.Entry<Inverter, ManagedSymmetricEss> entry : InvEssMap.entrySet()) {
//				if (entry.getValue().getSoc().get() > targetSoc) {
//					OnlyWantedInvEssMap.put(entry.getKey(), entry.getValue());
//				}
//			}

//			switch (targetDirection) {
//			case CHARGE:
//
//				if (0 < setPower && //
//						setPower < (0.40 * (Math.abs(PmaxCharge) / OnlyWantedInvEssMap.size()))) { //
//
//					Om = operationMode.one;
//				} else if ((0.40 * (Math.abs(PmaxCharge) / OnlyWantedInvEssMap.size())) < setPower && //
//						setPower < Math.abs(PmaxCharge)) { //
//
//					Om = operationMode.two;
//				} else { //
//
//					Om = operationMode.three;
//				}
//
//				break;
//			case DISCHARGE:
//
//				if (0 < setPower && //
//						setPower < (0.40 * (Math.abs(PmaxDischarge) / OnlyWantedInvEssMap.size()))) {
//					Om = operationMode.one;
//				} else if ((0.40 * ((Math.abs(PmaxDischarge) / OnlyWantedInvEssMap.size())) < setPower
//						&& setPower < Math.abs(PmaxDischarge))) {
//					Om = operationMode.two;
//				} else {
//					Om = operationMode.three;
//				}
//
//				break;
//			case KEEP_ZERO:
//				break;
//
//			}
//
//			System.out.println(OnlyWantedInvSocMap);
//			System.out.println(OnlyWantedInvEssMap);
//
//			break;

		}

		return null;
	}

	private static TypeOfcluster GetTheTypeOfCluster(LinkedHashMap<Inverter, Integer> invSocMap) {
		int differenceSoc = Collections.max(invSocMap.values()) - Collections.min(invSocMap.values());

		if (differenceSoc > 5) {
			return TypeOfcluster.HETEROGENOUS;
		} else {
			return TypeOfcluster.HOMOGENOUS;
		}
	}

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

	private static List<Inverter> getListForDisCharging(LinkedHashMap<Inverter, ManagedSymmetricEss> map, int i) {

		List<Inverter> OnlyWantedInv = new ArrayList<Inverter>();
		int counter = 1;

		for (Map.Entry<Inverter, ManagedSymmetricEss> entry : map.entrySet()) {
			if (counter > i) {
				// System.out.println("Key : " + entry.getKey() + " value : " +
				// entry.getValue());

				OnlyWantedInv.add(entry.getKey());

			}
			counter++;
		}

		return OnlyWantedInv;
	}

	static class WeightsUtil { // WeightsUtil.java

		private static final int DEFAULT_WEIGHT = 50;
		private static final float SORT_FACTOR = 0.3f; // why ??

		/**
		 * Sets the weight of each Inverter according to the SoC of its ESS.
		 *
		 * @param inverters a List of inverters
		 * @param esss      list of {@link ManagedSymmetricEss}s
		 */
		public static void updateWeightsFromSoc(List<Inverter> inverters, List<ManagedSymmetricEss> esss) {
			for (Inverter inv : inverters) {
				var weight = DEFAULT_WEIGHT;
				for (ManagedSymmetricEss ess : esss) {
					if (inv.getEssId().equals(ess.id())) {
						weight = ess.getSoc().orElse(DEFAULT_WEIGHT);
						break;
					}
				}
				inv.setWeight(weight);
			}
		}

		/**
		 * get Inver and its Soc as a map
		 * 
		 * @param invs
		 * @param esss
		 * @return
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

		/**
		 * Sorts the list of Inverters by their weights descending.
		 *
		 * @param inverters a List of inverters
		 */
		public static void sortByWeights(List<Inverter> inverters) {
			Collections.sort(inverters, (e1, e2) -> {
				// first: sort by weight
				var weightCompare = Integer.compare(e2.getWeight(), e1.getWeight());
				if (weightCompare != 0) {
					return weightCompare;
				}
				// second: sort by name
				return e1.toString().compareTo(e2.toString());
			});
		}

		/**
		 * Adjust the sorting of Inverters by weights.
		 *
		 * <p>
		 * This is different to 'invertersSortByWeights()' in that it tries to avoid
		 * resorting the entire list all the time. Instead it only adjusts the list
		 * slightly.
		 *
		 * @param inverters a List of inverters
		 */
		public static void adjustSortingByWeights(List<Inverter> inverters) {
			for (var i = 1; i < inverters.size(); i++) {
				for (var j = 0; j < inverters.size() - i; j++) {
					var weight1 = inverters.get(j).getWeight();
					var weight2 = inverters.get(j + 1).getWeight();
					var threshold = (int) (Math.min(weight1, weight2) * SORT_FACTOR);
					if (weight1 < weight2 - threshold) {
						Collections.swap(inverters, j, j + 1);
					}
				}
			}
		}

	}

	/*
	 * have to verify this sorting with simulators if possible as we need resorting
	 * of the list every cycle, and invertersSortByWeights() what happens ?
	 * difference with this sorting
	 */

	/*
	 * ESS is what should be sorted within a cluster, logically but we do it with
	 * Inverters in OpenEMS
	 */

//	var maxDischargePower = entry.getValue() //
//			.getPower() //
//			.getMaxPower(entry.getValue(), Phase.ALL, Pwr.ACTIVE);
//
//	System.out.println(" Discharge power of " + entry.getValue().id() + " is : " + maxDischargePower);
//
//	PmaxDischarge += maxDischargePower;
//
//	var maxChargePower = entry.getValue() //
//			.getPower() //
//			.getMinPower(entry.getValue(), Phase.ALL, Pwr.ACTIVE);
//
//	System.out.println(" Charge power of " + entry.getValue().id() + " is : " + maxChargePower);
//
//	PmaxCharge -= maxChargePower;

}
