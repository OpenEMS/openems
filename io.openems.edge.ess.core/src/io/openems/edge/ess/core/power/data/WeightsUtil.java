package io.openems.edge.ess.core.power.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Inverter;

public class WeightsUtil {

	private static final int DEFAULT_WEIGHT = 50;
	private static final float SORT_FACTOR = 0.3f;

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

	public static LinkedHashMap <Inverter, Integer> getInvAndSocMap(List<Inverter> invs, List<ManagedSymmetricEss> esss) {
		List<ManagedSymmetricEss> updatesEss = new ArrayList<>();
		LinkedHashMap <Inverter, Integer> InvSocMap = new LinkedHashMap <Inverter, Integer>();

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
