package io.openems.edge.common.sum;

import static io.openems.edge.common.test.TestUtils.testWithValue;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class DummySumTest {

	@Test
	public void test() throws OpenemsException {
		final var sut = new DummySum();

		testWithValue(sut, DummySum::withProductionAcActivePower, Sum::getProductionAcActivePower);
		testWithValue(sut, DummySum::withGridActivePower, Sum::getGridActivePower);
		testWithValue(sut, DummySum::withEssCapacity, Sum::getEssCapacity);
		testWithValue(sut, DummySum::withEssSoc, Sum::getEssSoc);
		testWithValue(sut, DummySum::withEssMinDischargePower, Sum::getEssMinDischargePower);
		testWithValue(sut, DummySum::withEssMaxDischargePower, Sum::getEssMaxDischargePower);
	}
}
