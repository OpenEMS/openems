package io.openems.edge.ess.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to calculate the overall State-of-Charge.
 */
public class CalculateSoc {

	private static record Ess(int soc, Integer capacity) {
	}

	private final List<Ess> esss = new ArrayList<>();

	/**
	 * Adds a {@link SymmetricEss}.
	 *
	 * @param ess the {@link SymmetricEss}
	 */
	public synchronized void add(SymmetricEss ess) {
		var soc = ess.getSoc().get();
		var capacity = ess.getCapacity().get();
		if (soc == null) {
			return;
		}
		this.esss.add(new Ess(soc, capacity));
	}

	/**
	 * Adds multiple {@link SymmetricEss}s.
	 *
	 * @param esss the {@link SymmetricEss}s
	 * @return myself
	 */
	public synchronized CalculateSoc add(List<SymmetricEss> esss) {
		esss.forEach(ess -> this.add(ess));
		return this;
	}

	/**
	 * Calculates the overall State-of-Charge.
	 *
	 * @return the SoC value or null
	 */
	public synchronized Integer calculate() {
		var result = calculateWeightedAverage(this.esss);
		if (result != null) {
			return result;
		}
		return calculateAverage(this.esss);
	}

	/**
	 * Calculate the SoC as average weighted by capacity.
	 * 
	 * @param esss the list of {@link Ess}
	 * @return the SoC or null if list is empty or any value is missing
	 */
	private static Integer calculateWeightedAverage(List<Ess> esss) {
		if (esss.isEmpty()) {
			return null;
		}
		if (esss.stream().anyMatch(e -> e.capacity() == null)) {
			return null;
		}

		var socCapacity = esss.stream().mapToInt(e -> e.soc() * e.capacity()).sum();
		var totalCapacity = esss.stream().mapToInt(Ess::capacity).sum();

		return Math.round(socCapacity / (float) totalCapacity);
	}

	/**
	 * Calculate the SoC as average.
	 * 
	 * @param esss the list of {@link Ess}
	 * @return the SoC or null if list is empty
	 */
	private static Integer calculateAverage(List<Ess> esss) {
		var soc = esss.stream() //
				.mapToInt(Ess::soc) //
				.average();

		if (soc.isEmpty()) {
			return null;
		}

		return (int) Math.round(soc.getAsDouble());
	}
}
