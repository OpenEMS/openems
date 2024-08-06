package io.openems.edge.mqtt.component.enums;

public enum TimeIntervalSeconds {
	CYCLE(0, 0), //
	SECONDS(1, 0), //
	MINUTES(60, 0), //
	QUARTER_HOUR(60 * 15, 1), //
	HOUR(60 * 60, 1), //
	DAY(60 * 60 * 24, 1); //

	public final int interval;
	public final int qos;

	TimeIntervalSeconds(int interval, int qos) {
		this.interval = interval;
		this.qos = qos;
	}
}
