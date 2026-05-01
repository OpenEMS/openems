package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.api.ConfigProperties;
import io.openems.edge.controller.ess.fixstateofcharge.api.EndCondition;

/**
 * Unit tests for {@link ReferenceCycleUtils}.
 */
class ReferenceCycleUtilsTest {

	private static final int CAPACITY_WH = 10_000;
	private static final int MAX_APPARENT_POWER = 10_000;
	private static final int TARGET_SOC = 30;
	private static final Clock CLOCK = new TimeLeapClock(Instant.parse("2023-01-01T08:00:00.00Z"), ZoneOffset.UTC);

	static Stream<Arguments> data() {
		return Stream.of(
				// Test 1: SoC = 70% -> refTarget = 100 (discharge test)
				// Note: calculateRequiredTime adds +1 to remainingSoC to avoid edge cases
				// Time to 100: (30+1)% @ 5000W = 3100Wh = 11160000Ws / 5000W = 2232s (37min
				// 12s)
				// Pause: 30 min = 1800s
				// Time from 100 to 30: (70+1)% @ 3333W = 7100Wh = 25560000Ws / 3333W = 7668s
				// (2h 7min 48s)
				// Total: 2232 + 1800 + 7668 = 11700s (3h 15min)
				Arguments.of(70, 100, 11700L, "3h 15min"),

				// Test 2: SoC = 60% -> refTarget = 0 (charge test)
				// Time to 0: (60+1)% @ 5000W (0.5C) = 6100Wh = 21960000Ws / 5000W = 4392s (1h
				// 13min 12s)
				// Pause: 30 min = 1800s
				// Time from 0 to 30: (30+1)% @ 3333W = 3100Wh = 11160000Ws / 3333W = 3348s
				// (55min 48s)
				// Total: 4392 + 1800 + 3348 = 9540s (2h 39min)
				Arguments.of(60, 0, 9540L, "2h 39min"));
	}

	@ParameterizedTest(name = "soc={0}%, refTarget={1}, expectedTime={3}")
	@MethodSource("data")
	void testCalculateRequiredTimeWithReferenceCycle(int socStart, int expectedRefTarget, long expectedTotalSeconds,
			String expectedTimeHumanReadable) throws InvalidValueException {
		var controller = new DummyFixStateOfChargeController().withCapacity(CAPACITY_WH);
		var config = new ConfigProperties(true, TARGET_SOC, false, null, 0, false, 0, false,
				EndCondition.CAPACITY_CHANGED);
		var context = new Context(controller, config,  MAX_APPARENT_POWER, socStart, TARGET_SOC, null, CLOCK);

		// Calculate using the utility method
		var actualSeconds = ReferenceCycleUtils.calculateRequiredTimeWithReferenceCycle(context);

		// Verify the result
		var actualTimeFormatted = formatSeconds(actualSeconds);
		var expectedTimeFormatted = formatSeconds(expectedTotalSeconds);
		assertEquals(expectedTotalSeconds, actualSeconds,
				String.format("Total required time should be %s (%d seconds), but was %s (%d seconds)",
						expectedTimeFormatted, expectedTotalSeconds, actualTimeFormatted, actualSeconds));

		// Also verify the reference target logic is correct by manually checking
		var refPowerW = ReferenceCycleUtils.calculateMaxReferencePower(MAX_APPARENT_POWER, CAPACITY_WH);
		var powerToTargetW = context.getTimeEstimationPowerW(CAPACITY_WH);

		var secondsToRef = AbstractFixStateOfCharge.calculateRequiredTime(socStart, expectedRefTarget, CAPACITY_WH,
				refPowerW, CLOCK);
		var pauseSeconds = ReferenceCycleUtils.REFERENCE_CYCLE_PAUSE_MS / 1000;
		var secondsRefToTarget = AbstractFixStateOfCharge.calculateRequiredTime(expectedRefTarget, TARGET_SOC,
				CAPACITY_WH, powerToTargetW, CLOCK);

		var manualCalculation = secondsToRef + pauseSeconds + secondsRefToTarget;

		assertEquals(manualCalculation, actualSeconds, "Manual calculation should match utility method result");
	}

	/**
	 * Formats seconds into a human-readable time string (e.g., "3h 15min 30s").
	 *
	 * @param totalSeconds total seconds to format
	 * @return formatted time string
	 */
	private static String formatSeconds(long totalSeconds) {
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		StringBuilder sb = new StringBuilder();
		if (hours > 0) {
			sb.append(hours).append("h ");
		}
		if (minutes > 0) {
			sb.append(minutes).append("min ");
		}
		if (seconds > 0 || sb.isEmpty()) {
			sb.append(seconds).append("s");
		}
		return sb.toString().trim();
	}
}
