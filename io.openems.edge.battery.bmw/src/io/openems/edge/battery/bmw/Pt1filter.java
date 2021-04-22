package io.openems.edge.battery.bmw;

/**
 * PT1 filter
 * 
 */
public class Pt1filter {

	private final double filterTimeConstant_s;
	
	private double yOld = 0.0;
	private double cycleTime_s = 0.0;

	/**
	 * Creates a PT1 filter.
	 * 
	 * @param filterTimeConstant_s	filter time constant in seconds (zero = disable filter)
	 * @param cycleTime_s			cycle time of calling applyPt1Filter in seconds
	 */
	public Pt1filter(double filterTimeConstant_s, double cycleTime_s) {
		this.filterTimeConstant_s = filterTimeConstant_s;
		this.cycleTime_s = cycleTime_s;
	}

	/**
	 * Sets cycle time of PT1 filter.
	 * 
	 * @param cycleTime_s			cycle time of calling applyPt1Filter in seconds
	 */
	public void setCycleTime_s(double cycleTime_s) {
		this.cycleTime_s = cycleTime_s;
	}

	/**
	 * Apply the PT1 filter
	 * 
	 * @param x 	the input value
	 * @return the filtered value
	 */
	public double applyPt1Filter(double x) {
		// cycle time have not to be zero
		if (cycleTime_s == 0.0) {
			return(0.0);
		}
		// disable PT1-Filter if time constant is zero
		if (filterTimeConstant_s == 0.0) {
			yOld = x;
			return(x);
		}
		// apply filter
		double y = (x + filterTimeConstant_s/cycleTime_s * yOld) / (1 + filterTimeConstant_s/cycleTime_s);
		yOld = y;
		return(y);
	}
}