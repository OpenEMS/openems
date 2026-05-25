package io.openems.edge.energy.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FitnessTest {

	@Nested
	@DisplayName("compareTo()")
	class CompareToTest {

		@Test
		void shouldPreferLowerHardConstraintViolations() {
			final var better = new Fitness(0, 0, 0, 0, 0, 0, 0);
			final var worse = new Fitness(1, 0, 0, 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerGridBuyCost() {
			final var better = new Fitness(1, 0, 0, 0, 0, 0, 0);
			final var worse = new Fitness(1, 1, 0, 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerGridBuyEnergy() {
			final var better = new Fitness(1, 1, 0, 0, 0, 0, 0);
			final var worse = new Fitness(1, 1, 1, 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferHigherGridSellRevenue() {
			final var better = new Fitness(1, 1, 1, 1, 0, 0, 0);
			final var worse = new Fitness(1, 1, 1, 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferHigherGridSellEnergy() {
			final var better = new Fitness(1, 1, 1, 1, 1, 0, 0);
			final var worse = new Fitness(1, 1, 1, 1, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerModePreferencePenalty() {
			final var better = new Fitness(1, 1, 1, 1, 1, 0, 0);
			final var worse = new Fitness(1, 1, 1, 1, 1, 1, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerSoftConstraintViolations() {
			final var better = new Fitness(1, 1, 1, 1, 1, 1, 0);
			final var worse = new Fitness(1, 1, 1, 1, 1, 1, 1);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldReturnZeroWhenAllFieldsAreEqual() {
			final var a = new Fitness(1, 1, 1, 1, 1, 1, 1);
			final var b = new Fitness(1, 1, 1, 1, 1, 1, 1);

			assertEquals(0, a.compareTo(b));
		}

		private static void assertBetterThan(Fitness better, Fitness worse) {
			assertTrue(better.compareTo(worse) < 0);
		}
	}

	@Nested
	class BuilderTest {

		@Test
		void shouldBuildFitnessWithCorrectValues() {
			final var result = Fitness.builder()//
					.addHardConstraintViolation(2)//
					.addHardConstraintViolation()//
					.addGridBuyCostScore(10)//
					.addGridBuyEnergyWh(5)//
					.addGridSellRevenueScore(3)//
					.addGridSellEnergyWh(4)//
					.withModePreferencePenalty(1.5)//
					.addSoftConstraintViolation(3)//
					.addSoftConstraintViolation()//
					.build();

			final var expected = new Fitness(3, 10, 5, 3, 4, 1.5, 4);

			assertEquals(expected, result);
		}
	}
}
