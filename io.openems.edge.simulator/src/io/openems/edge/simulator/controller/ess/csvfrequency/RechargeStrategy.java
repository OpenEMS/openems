package io.openems.edge.simulator.controller.ess.csvfrequency;

/**
 * Recharge (IDM) Strategy.
 *
 * <p>
 * Manages battery SOC by periodically deciding to charge, discharge, or idle
 * based on SOC thresholds. Decisions are made at fixed intervals
 * (idmTransactionTime) and held until the next interval.
 *
 * <ul>
 * <li>SOC &lt; socMin -> charge (response = -1.0)
 * <li>SOC &gt; socMax -> discharge (response = 1.0)
 * <li>Otherwise -> idle (response = 0.0)
 * </ul>
 */
public class RechargeStrategy {

	private final long idmTransactionTime;
	private final double socMin;
	private final double socMax;

	private double response = 0.0;

	/**
	 * Creates a new Recharge strategy.
	 *
	 * @param idmTransactionTime interval in seconds at which SOC decisions are made
	 *                           (e.g. 900 = 15 minutes)
	 * @param socMin             minimum SOC threshold in percent; below this,
	 *                           charge (e.g. 20)
	 * @param socMax             maximum SOC threshold in percent; above this,
	 *                           discharge (e.g. 80)
	 */
	public RechargeStrategy(long idmTransactionTime, double socMin, double socMax) {
		this.idmTransactionTime = idmTransactionTime;
		this.socMin = socMin;
		this.socMax = socMax;
	}

	/**
	 * Calculate the recharge response.
	 *
	 * @param time running time counter in seconds since controller start
	 * @param soc  current state of charge in percent [0-100]
	 * @return response in per-unit: -1.0 = charge, 1.0 = discharge, 0.0 = idle
	 */
	public double next(long time, double soc) {
		if (time % this.idmTransactionTime == 0) {
			if (soc < this.socMin) {
				this.response = -1.0; // charge (negative = charging in OpenEMS)
			} else if (soc > this.socMax) {
				this.response = 1.0; // discharge (positive = discharging in OpenEMS)
			} else {
				this.response = 0.0; // idle
			}
		}

		return this.response;
	}
}
