package io.openems.edge.evcs.keba.kecontact;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;

import org.junit.Test;

public class ReadWorkerTest {

	private LocalDateTime now = LocalDateTime.now();

	@Test
	public void testAllReportsDue() {
		LocalDateTime lastReport1 = LocalDateTime.MIN;
		LocalDateTime lastReport2 = LocalDateTime.MIN;
		LocalDateTime lastReport3 = LocalDateTime.MIN;

		int result = ReadWorker.getCycleTimeLogic(lastReport1, lastReport2, lastReport3, this.now);
		assertEquals(0, result);
	}

	@Test
	public void testNoReportsDue() {
		LocalDateTime lastReport1 = this.now.minusSeconds(5);
		LocalDateTime lastReport2 = this.now.minusSeconds(5);
		LocalDateTime lastReport3 = this.now.minusSeconds(5);

		int result = ReadWorker.getCycleTimeLogic(lastReport1, lastReport2, lastReport3, this.now);
		assertEquals(5000, result);
	}

	@Test
	public void testOneReportDue() {
		LocalDateTime lastReport1 = this.now.minusHours(1);
		LocalDateTime lastReport2 = this.now.minusSeconds(1);
		LocalDateTime lastReport3 = this.now.minusSeconds(1);

		int result = ReadWorker.getCycleTimeLogic(lastReport1, lastReport2, lastReport3, this.now);
		assertEquals(0, result);
	}

	@Test
	public void testReportsInFuture() {
		LocalDateTime lastReport1 = this.now.plusSeconds(5);
		LocalDateTime lastReport2 = this.now.plusSeconds(5);
		LocalDateTime lastReport3 = this.now.plusSeconds(5);

		int result = ReadWorker.getCycleTimeLogic(lastReport1, lastReport2, lastReport3, this.now);
		assertEquals(15000, result);
	}

	@Test
	public void testReportsFarInFuture() {
		LocalDateTime lastReport1 = LocalDateTime.MAX;
		LocalDateTime lastReport2 = LocalDateTime.MAX;
		LocalDateTime lastReport3 = LocalDateTime.MAX;

		int result = ReadWorker.getCycleTimeLogic(lastReport1, lastReport2, lastReport3, this.now);
		assertEquals(0, result);
	}
	
	@Test
	public void testEdgeCaseReportNearNow() {
		LocalDateTime lastReport1 = this.now.minusSeconds(Report.REPORT1.getRequestSeconds() - 1);
		LocalDateTime lastReport2 = this.now.minusSeconds(Report.REPORT2.getRequestSeconds() - 1);
		LocalDateTime lastReport3 = this.now.minusSeconds(Report.REPORT3.getRequestSeconds() - 1);

		int result = ReadWorker.getCycleTimeLogic(lastReport1, lastReport2, lastReport3, this.now);
		assertEquals(1000, result);
	}
}