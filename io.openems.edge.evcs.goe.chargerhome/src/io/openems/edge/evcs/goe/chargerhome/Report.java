package io.openems.edge.evcs.goe.chargerhome;

public enum Report {
	REPORT1(200), REPORT2(50), REPORT3(10);

	private int requestSeconds;

	private Report(int requestSeconds) {
		this.requestSeconds = requestSeconds;
	}

	/**
	 * Get requested seconds.
	 *
	 * @return seconds
	 */
	public int getRequestSeconds() {
		return this.requestSeconds;
	}

	public void setRequestSeconds(int requestSeconds) {
		this.requestSeconds = requestSeconds;
	}
}
