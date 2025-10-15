package io.openems.edge.predictor.profileclusteringmodel.services;

public record QueryWindow(int minWindowDays, int maxWindowDays) {

	public QueryWindow {
		if (minWindowDays <= 0 || maxWindowDays <= 0) {
			throw new IllegalArgumentException("minDays and maxDays must be greater than zero");
		}
		if (minWindowDays > maxWindowDays) {
			throw new IllegalArgumentException("minDays must be less than or equal to maxDays");
		}
	}

	public QueryWindow(int windowDays) {
		this(windowDays, windowDays);
	}

	/**
	 * Returns the minimum training window size in quarters (15-minute intervals).
	 *
	 * @return the minimum window size in quarters
	 */
	public int minQuarters() {
		return this.minWindowDays * 24 /* hours per day */ * 4 /* quarters per hour */;
	}

	/**
	 * Returns the maximum training window size in quarters (15-minute intervals).
	 *
	 * @return the minimum window size in quarters
	 */
	public int maxQuarters() {
		return this.maxWindowDays * 24 /* hours per day */ * 4 /* quarters per hour */;
	}
}
