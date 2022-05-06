package io.openems.edge.ess.core.power.optimizers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.optim.PointValuePair;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.Data;
import io.openems.edge.ess.core.power.data.TargetDirection;
import io.openems.edge.ess.power.api.Inverter;

import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Inverter;
import io.openems.edge.ess.power.api.LinearCoefficient;
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
		
		
		Map<Inverter, Integer> InvSocMap = WeightsUtil.getSocList(invs, esss);
		//{"key" : "value"}
		
		// {inv1, soc1}
		// {inv2, soc2}
		
		var targetDifferenceSoc = Collections.max(InvSocMap.values()) -  Collections.min(InvSocMap.values());
		
		int n = 0;
		for (Map.Entry<Inverter, Integer> entry : InvSocMap.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
			
			if (entry.getValue() <  targetDifferenceSoc) {
				n = n+1;
			}
		}
		
		

		// find maxLastActive + maxWeight
		var maxLastActivePower = 0;
		var sumWeights = 0;

		//sortByWeights(allInverters);

		// Get the targetDifferenceSoc 
		// targetDifferenceSoc = Max soc - min Soc
		// Make the List of Soc's

		//InvSocMap = WeightsUtil.getSocList(allInverters, esss);

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
		 * Return the List of inverter and its Soc
		 * @param inverters
		 * @param esss
		 * @return
		 */
		public static Map<Inverter, Integer> getSocList(List<Inverter> inverters, List<ManagedSymmetricEss> esss) {

			Map<Inverter, Integer> InvSocMap = new HashMap<>();
			for (Inverter inv : inverters) {
				for (ManagedSymmetricEss ess : esss) {
					InvSocMap.put(inv, ess.getSoc().get());
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
