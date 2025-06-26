package io.openems.edge.goodwe.charger.mppt.twostring;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

public class TestStatic {

	@Test
	public void testCaculateEnergyFromTwoStrings() {
		Optional<Object> string1 = Optional.of(40_000L);
		Optional<Object> string2 = Optional.of(90_000L);
		Optional<Object> string3 = Optional.empty();
		Optional<Object> string4 = Optional.of(0);

		var result = GoodWeChargerMpptTwoStringImpl.caculateEnergyFromTwoStrings(string1, string2);
		assertEquals((long) result, 130_000L);

		var result2 = GoodWeChargerMpptTwoStringImpl.caculateEnergyFromTwoStrings(string3, string4);
		assertEquals((long) result2, 0);

		var result3 = GoodWeChargerMpptTwoStringImpl.caculateEnergyFromTwoStrings(string1, string3);
		assertEquals((long) result3, 40_000L);
	}

}
