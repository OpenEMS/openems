package io.openems.edge.ess.core.power.data;

import java.util.Collections;
import java.util.List;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Inverter;

public class WeightsUtil {

	private static final int DEFAULT_WEIGHT = 50;
	private static final float SORT_FACTOR = 0.3f;

	/**
	 * Sets the weight of each Inverter according to the SoC of its ESS.
	 * 
	 * @param inverters a List of inverters
	 */
	public static void updateWeightsFromSoc(List<Inverter> inverters, List<ManagedSymmetricEss> esss) {
		for (Inverter inv : inverters) {
			int weight = DEFAULT_WEIGHT;
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
	 * Sorts the list of Inverters by their weights descending.
	 * 
	 * @param inverters a List of inverters
	 */
	public static void sortByWeights(List<Inverter> inverters) {
		Collections.sort(inverters, (e1, e2) -> {
			// first: sort by weight
			int weightCompare = Integer.compare(e2.getWeight(), e1.getWeight());
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
		for (int i = 1; i < inverters.size(); i++) {
			for (int j = 0; j < inverters.size() - i; j++) {
				int weight1 = inverters.get(j).getWeight();
				int weight2 = inverters.get(j + 1).getWeight();
				int threshold = (int) (Math.min(weight1, weight2) * SORT_FACTOR);
				if (weight1 < weight2 - threshold) {
					Collections.swap(inverters, j, j + 1);
				}
			}
		}
	}
}
