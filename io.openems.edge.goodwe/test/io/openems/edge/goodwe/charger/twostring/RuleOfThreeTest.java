package io.openems.edge.goodwe.charger.twostring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Test;

@SuppressWarnings("deprecation")
public class RuleOfThreeTest {

	@Test
	public void calculateByRuleOfThreeTest() {
		final var totalValue = Optional.of(2000); // e.g. W
		final var divisor = Optional.of(32); // e.g. A

		/*
		 * Assume charger1
		 */
		final var value1 = Optional.of(17);

		Integer expectedResult1 = 1_063;
		Integer result = GoodWeChargerTwoString.calculateByRuleOfThree(totalValue, divisor, value1).orElse(0);
		assertEquals(expectedResult1, result);

		/*
		 * Assume charger2
		 */
		final var value2 = Optional.of(15);

		/*
		 * The expected value may still differ by one watt if both decimal endings
		 * result in .5.
		 *
		 * If both current values aren't the same, it would be possible to Round
		 * AwayFromZero or DownToZero if the charger is producing more or less than the
		 * other.
		 */
		Integer expectedResult2 = totalValue.get() - expectedResult1 + 1; //
		Integer result2 = GoodWeChargerTwoString.calculateByRuleOfThree(totalValue, divisor, value2).orElse(0);

		assertEquals(expectedResult2, result2);

	}

	@Test
	public void calculateByRuleOfThreeWithZeroDivisorTest() {
		final var totalValue = Optional.of(10000);
		final var divisor = Optional.of(0);
		final var value = Optional.of(10);

		var resultIsPresent = GoodWeChargerTwoString.calculateByRuleOfThree(totalValue, divisor, value).isPresent();

		assertFalse(resultIsPresent);
	}

	@Test
	public void calculateByRuleOfThreeWithGreaterResultTest() {
		final var totalValue = Optional.of(1000);
		final var divisor = Optional.of(20);
		final var value = Optional.of(40);

		Integer expectedResult = 2000;
		Integer result = GoodWeChargerTwoString.calculateByRuleOfThree(totalValue, divisor, value).orElse(0);

		assertEquals(expectedResult, result);
	}

	@Test
	public void calculateByRuleOfThreeWithEmptyValuesTest() {
		final Optional<Integer> totalValue = Optional.empty();
		final Optional<Integer> divisor = Optional.empty();
		final Optional<Integer> value = Optional.empty();

		Integer expectedResult = 0;
		Integer result = GoodWeChargerTwoString.calculateByRuleOfThree(totalValue, divisor, value).orElse(0);

		assertEquals(expectedResult, result);
	}
}
