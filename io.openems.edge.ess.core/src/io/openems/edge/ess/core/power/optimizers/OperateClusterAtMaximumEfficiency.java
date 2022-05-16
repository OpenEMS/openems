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
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class OperateClusterAtMaximumEfficiency {

	public static PointValuePair apply(Coefficients coefficients, //
			TargetDirection targetDirection, //
			List<Inverter> allInverters, //
			List<Inverter> targetInverters, //
			List<Constraint> allConstraints, //
			List<Inverter> invs, //
			List<ManagedSymmetricEss> esss //
	) {

		for (var c : allConstraints) {
			if (c.getCoefficients().length == 1 && c.getCoefficients()[0].getCoefficient().getPwr() == Pwr.ACTIVE
					&& c.getRelationship() == Relationship.EQUALS) {
				
				var setPower = c.getValue();
				System.out.println(setPower);
			}
		}

		LinkedHashMap<Inverter, Integer> InvSocMap = WeightsUtil.getInvAndSocMap(invs, esss);
		LinkedHashMap<Inverter, ManagedSymmetricEss> InvEssMap = WeightsUtil.getInvAndEssMap(invs, esss);
		LinkedHashMap<Inverter, Integer> OnlyWantedInvSocMap = new LinkedHashMap<>();
		LinkedHashMap<Inverter, ManagedSymmetricEss> OnlyWantedInvEssMap = new LinkedHashMap<>();
		System.out.println(InvSocMap);

		int targetDifferenceSoc = Collections.max(InvSocMap.values()) - Collections.min(InvSocMap.values());

		// Get the reduced target inverters
		List<Inverter> reducedTargetInverter = new ArrayList<Inverter>();

		// int n = 0;
		for (Map.Entry<Inverter, Integer> entry : InvSocMap.entrySet()) {
			if (entry.getValue() < targetDifferenceSoc) {
				OnlyWantedInvSocMap.put(entry.getKey(), entry.getValue());
				reducedTargetInverter.add(entry.getKey());

			}
		}

		for (Map.Entry<Inverter, ManagedSymmetricEss> entry : InvEssMap.entrySet()) {
			if (entry.getValue().getSoc().get() < targetDifferenceSoc) {
				OnlyWantedInvEssMap.put(entry.getKey(), entry.getValue());
			}
		}

		System.out.println(OnlyWantedInvSocMap);
		System.out.println(OnlyWantedInvEssMap);

		// calculate the Pmax based on the target direction
		int PmaxInCharge = 0;
		int PmaxInDischarge = 0;
		switch (targetDirection) {
		case CHARGE:
			for (Map.Entry<Inverter, ManagedSymmetricEss> entry : OnlyWantedInvEssMap.entrySet()) {

			}
			break;
		case DISCHARGE:
			break;
		case KEEP_ZERO:
			break;

		}

		return null;
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
			LinkedHashMap<Inverter, Integer> InvSocMap = new LinkedHashMap<Inverter, Integer>();

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
			LinkedHashMap<Inverter, ManagedSymmetricEss> InvSocMap = new LinkedHashMap<Inverter, ManagedSymmetricEss>();

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

}
