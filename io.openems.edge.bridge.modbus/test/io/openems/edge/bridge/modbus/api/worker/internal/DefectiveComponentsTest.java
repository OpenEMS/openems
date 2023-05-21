package io.openems.edge.bridge.modbus.api.worker.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.edge.common.test.TimeLeapClock;

public class DefectiveComponentsTest {

	private final static String CMP = "foo";

	@Test
	public void testIsDueForNextTry() {
		var clock = new TimeLeapClock();
		var sut = new DefectiveComponents(clock);

		assertFalse(sut.isDueForNextTry(CMP));
		sut.add(CMP);
		assertFalse(sut.isDueForNextTry(CMP));
		clock.leap(30_001, ChronoUnit.MILLIS);
		assertTrue(sut.isDueForNextTry(CMP));
	}

	@Test
	public void testAddRemove() {
		var clock = new TimeLeapClock();
		var sut = new DefectiveComponents(clock);

		sut.add(CMP);
		clock.leap(30_001, ChronoUnit.MILLIS);
		assertTrue(sut.isDueForNextTry(CMP));
		sut.remove(CMP);
		assertFalse(sut.isDueForNextTry(CMP));
	}

	@Test
	public void testIsKnownw() {
		var sut = new DefectiveComponents();

		sut.add(CMP);
		assertTrue(sut.isKnown(CMP));
		sut.remove(CMP);
		assertFalse(sut.isKnown(CMP));
	}

}
