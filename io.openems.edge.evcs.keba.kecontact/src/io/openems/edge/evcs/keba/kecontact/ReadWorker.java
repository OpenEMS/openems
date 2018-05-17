package io.openems.edge.evcs.keba.kecontact;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import io.openems.edge.common.worker.AbstractWorker;

public class ReadWorker extends AbstractWorker {

	private final static int REPORT_1_SECONDS = 6 * 60 * 60; // 6 hours
	private final static int REPORT_2_SECONDS = 60 * 60; // 1 hour
	private final static int REPORT_3_SECONDS = 10 * 60; // 10 minutes

	private final KebaKeContact parent;

	private LocalDateTime nextReport1 = LocalDateTime.MIN;
	private LocalDateTime nextReport2 = LocalDateTime.MIN;
	private LocalDateTime nextReport3 = LocalDateTime.MIN;

	public ReadWorker(KebaKeContact parent) {
		this.parent = parent;
	}

	@Override
	protected void activate(String name) {
		super.activate(name);
	}

	@Override
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void forever() {
		// REPORT 1
		if (this.nextReport1.isBefore(LocalDateTime.now())) {
			this.nextReport1 = LocalDateTime.now().plusSeconds(REPORT_1_SECONDS);
			this.parent.send("report 1");
		}
		// REPORT 2
		if (this.nextReport2.isBefore(LocalDateTime.now())) {
			this.nextReport2 = LocalDateTime.now().plusSeconds(REPORT_2_SECONDS);
			parent.send("report 2");
		}
		// REPORT 3
		if (this.nextReport3.isBefore(LocalDateTime.now())) {
			this.nextReport3 = LocalDateTime.now().plusSeconds(REPORT_3_SECONDS);
			parent.send("report 3");
		}
	}

	@Override
	protected int getCycleTime() {
		// get minimum required time till next report
		LocalDateTime now = LocalDateTime.now();
		if (this.nextReport1.isBefore(now) || this.nextReport2.isBefore(now) || this.nextReport3.isBefore(now)) {
			return 0;
		}
		long tillReport1 = ChronoUnit.MILLIS.between(now, this.nextReport1);
		long tillReport2 = ChronoUnit.MILLIS.between(now, this.nextReport2);
		long tillReport3 = ChronoUnit.MILLIS.between(now, this.nextReport3);
		long min = Math.min(Math.min(tillReport1, tillReport2), tillReport3);
		if (min < 0) {
			return 0;
		} else if (min > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		} else {
			return (int) min;
		}
	}

	@Override
	public void triggerForceRun() {
		// reset times for next report query
		this.nextReport1 = LocalDateTime.MIN;
		this.nextReport2 = LocalDateTime.MIN;
		this.nextReport3 = LocalDateTime.MIN;

		super.triggerForceRun();
	}

}
