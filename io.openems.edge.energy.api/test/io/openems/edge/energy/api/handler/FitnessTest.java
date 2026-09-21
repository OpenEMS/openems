package io.openems.edge.energy.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FitnessTest {

	private static final Fitness.CustomConstraint CONSTRAINT_AFTER_HARD = new Fitness.CustomConstraint(//
			"constraint1", //
			Fitness.CustomConstraint.Position.AFTER_HARD_CONSTRAINT_VIOLATIONS, //
			Fitness.CustomConstraint.Direction.LOWER_IS_BETTER);

	private static final Fitness.CustomConstraint CONSTRAINT_AFTER_BUY = new Fitness.CustomConstraint(//
			"constraint2", //
			Fitness.CustomConstraint.Position.AFTER_GRID_BUY_COST_SCORE, //
			Fitness.CustomConstraint.Direction.HIGHER_IS_BETTER);

	@Nested
	@DisplayName("compareTo()")
	class CompareToTest {

		@Test
		void shouldPreferLowerHardConstraintViolations() {
			final var better = new Fitness(0, List.of(), 0, 0, List.of(), 0, 0, 0, 0);
			final var worse = new Fitness(1, List.of(), 0, 0, List.of(), 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerCustomConstraintAfterHardConstraintViolations() {
			final var better = new Fitness(1, List.of(new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_HARD, 0)), 0,
					0, List.of(), 0, 0, 0, 0);
			final var worse = new Fitness(1, List.of(new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_HARD, 1)), 0,
					0, List.of(), 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerGridBuyCost() {
			final var better = new Fitness(1, List.of(), 0, 0, List.of(), 0, 0, 0, 0);
			final var worse = new Fitness(1, List.of(), 1, 0, List.of(), 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerGridBuyEnergy() {
			final var better = new Fitness(1, List.of(), 1, 0, List.of(), 0, 0, 0, 0);
			final var worse = new Fitness(1, List.of(), 1, 1, List.of(), 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferHigherCustomConstraintAfterGridBuyCostScore() {
			final var better = new Fitness(1, List.of(), 1, 0,
					List.of(new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_BUY, 1)), 0, 0, 0, 0);
			final var worse = new Fitness(1, List.of(), 1, 0,
					List.of(new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_BUY, 0)), 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferHigherGridSellRevenue() {
			final var better = new Fitness(1, List.of(), 1, 1, List.of(), 1, 0, 0, 0);
			final var worse = new Fitness(1, List.of(), 1, 1, List.of(), 0, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferHigherGridSellEnergy() {
			final var better = new Fitness(1, List.of(), 1, 1, List.of(), 1, 1, 0, 0);
			final var worse = new Fitness(1, List.of(), 1, 1, List.of(), 1, 0, 0, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerModePreferencePenalty() {
			final var better = new Fitness(1, List.of(), 1, 1, List.of(), 1, 1, 0, 0);
			final var worse = new Fitness(1, List.of(), 1, 1, List.of(), 1, 1, 1, 0);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldPreferLowerSoftConstraintViolations() {
			final var better = new Fitness(1, List.of(), 1, 1, List.of(), 1, 1, 1, 0);
			final var worse = new Fitness(1, List.of(), 1, 1, List.of(), 1, 1, 1, 1);

			assertBetterThan(better, worse);
		}

		@Test
		void shouldReturnZero_whenAllFieldsAreEqual() {
			final var a = new Fitness(1, List.of(), 1, 1, List.of(), 1, 1, 1, 1);
			final var b = new Fitness(1, List.of(), 1, 1, List.of(), 1, 1, 1, 1);

			assertEquals(0, a.compareTo(b));
		}

		@Test
		void shouldThrowException_whenComparingDifferentCustomConstraintDefinitions() {
			final var otherConstraintAfterHard = new Fitness.CustomConstraint(//
					"constraint-other", //
					Fitness.CustomConstraint.Position.AFTER_HARD_CONSTRAINT_VIOLATIONS, //
					Fitness.CustomConstraint.Direction.LOWER_IS_BETTER);

			final var left = new Fitness(//
					1, //
					List.of(new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_HARD, 1.0)), //
					1, //
					1, //
					List.of(), //
					1, //
					1, //
					1, //
					1);

			final var right = new Fitness(//
					1, //
					List.of(new Fitness.AppliedCustomConstraint(otherConstraintAfterHard, 1.0)), //
					1, //
					1, //
					List.of(), //
					1, //
					1, //
					1, //
					1);

			assertThrows(IllegalArgumentException.class, () -> left.compareTo(right));
		}

		@Test
		void shouldThrowException_whenComparingDifferentCustomConstraintOrder() {
			final var secondAfterHard = new Fitness.CustomConstraint(//
					"constraint3", //
					Fitness.CustomConstraint.Position.AFTER_HARD_CONSTRAINT_VIOLATIONS, //
					Fitness.CustomConstraint.Direction.LOWER_IS_BETTER);

			final var left = new Fitness(//
					1, //
					List.of(//
							new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_HARD, 1.0), //
							new Fitness.AppliedCustomConstraint(secondAfterHard, 1.0)), //
					1, //
					1, //
					List.of(), //
					1, //
					1, //
					1, //
					1);

			final var right = new Fitness(//
					1, //
					List.of(//
							new Fitness.AppliedCustomConstraint(secondAfterHard, 1.0), //
							new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_HARD, 1.0)), //
					1, //
					1, //
					List.of(), //
					1, //
					1, //
					1, //
					1);

			assertThrows(IllegalArgumentException.class, () -> left.compareTo(right));
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
					.addCustomConstraint(CONSTRAINT_AFTER_HARD, 2.0)//
					.addCustomConstraint(CONSTRAINT_AFTER_HARD, 3.0)//
					.addGridBuyCostScore(10)//
					.addGridBuyEnergyWh(5)//
					.addCustomConstraint(CONSTRAINT_AFTER_BUY, 4.0)//
					.addGridSellRevenueScore(3)//
					.addGridSellEnergyWh(4)//
					.withModePreferencePenalty(1.5)//
					.addSoftConstraintViolation(3)//
					.addSoftConstraintViolation()//
					.build();

			final var expected = new Fitness(//
					3, //
					List.of(new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_HARD, 5.0)), //
					10, //
					5, //
					List.of(new Fitness.AppliedCustomConstraint(CONSTRAINT_AFTER_BUY, 4.0)), //
					3, //
					4, //
					1.5, //
					4);

			assertEquals(expected, result);
		}
	}
}