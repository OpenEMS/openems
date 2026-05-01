package io.openems.edge.energy.api.simulation.periods;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class RawPeriodDataTest {

	@Nested
	@DisplayName("isCompatibleWith()")
	public class IsCompatibleWithTest {

		@Test
		public void shouldReturnTrue_whenCompatible() {
			final var base = allOptionalValuesPresent();
			final var candidate = allOptionalValuesPresentVariant();

			final boolean result = candidate.isCompatibleWith(base);

			assertTrue(result);
		}

		@Test
		public void shouldReturnFalse_whenOptionalValueMissing() {
			final var base = allOptionalValuesPresent();
			final var candidate = missingOneOptionalValue();

			final boolean result = candidate.isCompatibleWith(base);

			assertFalse(result);
		}

		@Test
		public void shouldReturnTrue_whenMoreOptionalValues() {
			final var base = noOptionalValuesPresent();
			final var candidate = allOptionalValuesPresent();

			final boolean result = candidate.isCompatibleWith(base);

			assertTrue(result);
		}
	}

	@Nested
	@DisplayName("hasNoOptionalValues()")
	public class HasNoOptionalValuesTest {

		@Test
		public void shouldReturnTrue_whenNoOptionalValues() {
			final var data = noOptionalValuesPresent();

			final boolean result = data.hasNoOptionalValues();

			assertTrue(result);
		}

		@Test
		public void shouldReturnFalse_whenOptionalValues() {
			final var data = missingOneOptionalValue();

			final boolean result = data.hasNoOptionalValues();

			assertFalse(result);
		}
	}

	private static RawPeriod.RawPeriodData allOptionalValuesPresent() {
		return new RawPeriod.RawPeriodData(1000, 500, 100.0, 25.0);
	}

	private static RawPeriod.RawPeriodData allOptionalValuesPresentVariant() {
		return new RawPeriod.RawPeriodData(1200, 600, 125.0, 30.0);
	}

	private static RawPeriod.RawPeriodData noOptionalValuesPresent() {
		return new RawPeriod.RawPeriodData(1000, null, null, null);
	}

	private static RawPeriod.RawPeriodData missingOneOptionalValue() {
		return new RawPeriod.RawPeriodData(1200, null, 125.0, 30.0);
	}
}
