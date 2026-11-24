package io.openems.edge.common.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RampFilterTest {

	@Test
	void resetToZeroStartsNextRampAtZero() {
		var sut = new RampFilter();
		sut.resetTo(0f);

		assertEquals(10, sut.getFilteredValueAsInteger(100f, 10f));
	}

	@Test
	void resetToValueStartsNextRampAtThatValue() {
		var sut = new RampFilter();
		sut.resetTo(500f);

		assertEquals(510, sut.getFilteredValueAsInteger(1000f, 10f));
	}

	@Test
	void resetToNullImmediatelyUsesNextTarget() {
		var sut = new RampFilter(500f);
		sut.resetTo(null);

		assertEquals(1000, sut.getFilteredValueAsInteger(1000f, 10f));
	}
}
