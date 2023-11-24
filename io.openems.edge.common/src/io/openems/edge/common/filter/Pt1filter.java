package io.openems.edge.common.filter;

public class Pt1filter {

	private final double filterTimeConstant;

	private double yOld = 0.0;
	private double cycleTime = 0.0;

	/**
	 * Creates a PT1 filter.
	 * 
	 * @param filterTimeConstant filter time constant in seconds.
	 * @param cycleTime          cycle time of calling applyPt1Filter in seconds
	 */
	public Pt1filter(double filterTimeConstant, double cycleTime) {
		this.filterTimeConstant = filterTimeConstant;
		this.cycleTime = cycleTime;
	}

	/**
	 * Sets cycle time of PT1 filter.
	 * 
	 * @param cycleTime cycle time of calling applyPt1Filter in seconds
	 */
	public void setCycleTime(double cycleTime) {
		this.cycleTime = cycleTime;
	}

	/**
	 * Apply the PT1 filter.
	 * 
	 * @param value the input value
	 * @return the filtered value
	 */
	public double applyPt1Filter(double value) {
		// cycle time has not to be zero
		if (this.cycleTime == 0.0) {
			return (0.0);
		}
		// disable PT1-Filter if time constant is zero
		if (this.filterTimeConstant == 0.0) {
			this.yOld = value;
			return (value);
		}
		// apply filter
		this.yOld = (value + this.filterTimeConstant / this.cycleTime * this.yOld)
				/ (1 + this.filterTimeConstant / this.cycleTime);
		return this.yOld;
	}
}