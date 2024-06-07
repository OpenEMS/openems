package io.openems.edge.common.filter;

public class Pt1filter {

	private final int filterTimeConstant;

	private double result;
	private int cycleTime;

	/**
	 * Creates a PT1 filter.
	 * 
	 * @param filterTimeConstant filter time constant in seconds.
	 * @param cycleTime          cycle time of calling applyPt1Filter in
	 *                           milliseconds
	 */
	public Pt1filter(int filterTimeConstant, int cycleTime) {
		this.filterTimeConstant = filterTimeConstant;
		this.cycleTime = cycleTime;
	}

	/**
	 * Sets cycle time of PT1 filter.
	 * 
	 * @param cycleTime cycle time of calling applyPt1Filter in seconds
	 */
	public void setCycleTime(int cycleTime) {
		this.cycleTime = cycleTime;
	}

	/**
	 * Apply the PT1 filter.
	 * 
	 * @param value the input value
	 * @return the filtered value
	 */
	public int applyPt1Filter(double value) {
		// disable PT1-Filter if time constant is zero
		if (this.filterTimeConstant == 0) {
			this.result = value;
			return (int) value;
		}
		// apply filter
		final var cycle = this.cycleTime / 1000.;
		this.result = (value + this.filterTimeConstant / cycle * this.result) / (1 + this.filterTimeConstant / cycle);
		return (int) this.result;
	}
}