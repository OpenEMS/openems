package io.openems.edge.energy.optimizer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.jenetics.Genotype;
import io.jenetics.IntegerGene;

public class GenotypeCache {

	private final ConcurrentHashMap<Genotype<IntegerGene>, Double> map = new ConcurrentHashMap<Genotype<IntegerGene>, Double>();
	private final AtomicInteger cacheHits = new AtomicInteger();

	public GenotypeCache() {
	}

	/**
	 * Gets the number of cache hits.
	 * 
	 * @return the count
	 */
	public int getCacheHits() {
		return this.cacheHits.get();
	}

	/**
	 * Query the {@link GenotypeCache}.
	 * 
	 * @param gt the {@link Genotype}
	 * @return the cost
	 */
	public double query(Genotype<IntegerGene> gt) {
		var value = this.map.get(gt);
		if (value != null) {
			this.cacheHits.incrementAndGet();
			return value;
		}
		return Double.NaN;
	}

	/**
	 * Add a cost to the {@link GenotypeCache}.
	 * 
	 * @param gt   the {@link Genotype}
	 * @param cost the cost
	 */
	public void add(Genotype<IntegerGene> gt, double cost) {
		this.map.put(gt, cost);
	}

}
