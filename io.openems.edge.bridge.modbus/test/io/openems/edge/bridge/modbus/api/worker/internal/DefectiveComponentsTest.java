package io.openems.edge.bridge.modbus.api.worker.internal;

import static io.openems.edge.bridge.modbus.api.worker.internal.CycleTasksManagerTest.LOG_HANDLER;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

public class DefectiveComponentsTest {

	private static final String CMP = "foo";

	@Test
	public void testIsDueForNextTry() {
		final var clock = createDummyClock();
		var sut = new DefectiveComponents(clock, LOG_HANDLER);

		assertNull(sut.isDueForNextTry(CMP));
		sut.add(CMP);
		assertFalse(sut.isDueForNextTry(CMP));
		clock.leap(30_001, ChronoUnit.MILLIS);
		assertTrue(sut.isDueForNextTry(CMP));
	}

	@Test
	public void testAddRemove() {
		final var clock = createDummyClock();
		var sut = new DefectiveComponents(clock, LOG_HANDLER);

		sut.add(CMP);
		clock.leap(30_001, ChronoUnit.MILLIS);
		assertTrue(sut.isDueForNextTry(CMP));
		sut.remove(CMP);
		assertNull(sut.isDueForNextTry(CMP));
	}

	@Test
	public void testIsKnownw() {
		var sut = new DefectiveComponents(LOG_HANDLER);

		sut.add(CMP);
		assertTrue(sut.isKnown(CMP));
		sut.remove(CMP);
		assertFalse(sut.isKnown(CMP));
	}

}
