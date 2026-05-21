package io.openems.edge.energy.api.simulation.periods;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PeriodDataTest {

	@Nested
	@DisplayName("hasConsistentValuePresence()")
	public class HasConsistentValuePresenceTest {

		@Test
		public void shouldReturnTrue_whenBothHaveAllFieldsPresent() {
			final var a = allOptionalFieldsPresent();
			final var b = allOptionalFieldsPresentVariant();

			final boolean result = a.hasConsistentValuePresence(b);

			assertTrue(result);
		}

		@Test
		public void shouldReturnTrue_whenBothEmpty() {
			final var a = noOptionalFieldsPresent();
			final var b = noOptionalFieldsPresent();

			final boolean result = a.hasConsistentValuePresence(b);

			assertTrue(result);
		}

		@Test
		public void shouldReturnFalse_whenOneMismatch() {
			final var a = allOptionalFieldsPresent();
			final var b = missingOneOptionalField();

			final boolean result = a.hasConsistentValuePresence(b);

			assertFalse(result);
		}

		@Test
		public void shouldReturnFalse_whenMultipleMismatches() {
			final var a = allOptionalFieldsPresent();
			final var b = noOptionalFieldsPresent();

			final boolean result = a.hasConsistentValuePresence(b);

			assertFalse(result);
		}
	}

	private static PeriodData allOptionalFieldsPresent() {
		return PeriodData.builder()//
				.withProduction(1000)//
				.withConsumption(new PeriodData.Prediction(600, 800))//
				.withGridBuyPrice(new PeriodData.Price(100.0, 1.0, 100.0))//
				.withGridSellPrice(new PeriodData.Price(25.0, 1.0, 25.0))//
				.build();
	}

	private static PeriodData allOptionalFieldsPresentVariant() {
		return PeriodData.builder()//
				.withProduction(2000)//
				.withConsumption(new PeriodData.Prediction(500, 700))//
				.withGridBuyPrice(new PeriodData.Price(80.0, 0.0, 80.0))//
				.withGridSellPrice(new PeriodData.Price(20.0, 0.0, 20.0))//
				.build();
	}

	private static PeriodData noOptionalFieldsPresent() {
		return PeriodData.builder()//
				.build();
	}

	private static PeriodData missingOneOptionalField() {
		return PeriodData.builder()//
				.withProduction(2000)//
				.withGridBuyPrice(new PeriodData.Price(80.0, 0.0, 80.0))//
				.withGridSellPrice(new PeriodData.Price(20.0, 0.0, 20.0))//
				.build();
	}
}
