package io.openems.edge.controller.io.heatingelement;

import static io.openems.edge.controller.io.heatingelement.Utils.getRequiredLevelFromPower;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.controller.io.heatingelement.enums.Level;

/**
 * Boundary tests for {@link Utils#getRequiredLevelFromPower(long, List)}, in
 * particular for the asymmetric Step powers of a two-heating-element PARALLEL
 * {@link io.openems.edge.controller.io.heatingelement.enums.Distribution} (A =
 * 2000 W, B = 1000 W): cumulated Step powers 2000, 3000, 5000, 6000, 8000,
 * 9000.
 */
class UtilsGetRequiredLevelFromPowerTest {

	private static List<HeatingElementStep> parallelSteps() throws OpenemsNamedException {
		// PARALLEL: A-L1, B-L1, A-L2, B-L2, A-L3, B-L3 -> 2000, 1000, 2000, 1000,
		// 2000, 1000
		return List.of(//
				step("io0/InputOutput0", 2000), step("io0/InputOutput3", 1000), //
				step("io0/InputOutput1", 2000), step("io0/InputOutput4", 1000), //
				step("io0/InputOutput2", 2000), step("io0/InputOutput5", 1000));
	}

	private static HeatingElementStep step(String channel, int nominalPower) throws OpenemsNamedException {
		return new HeatingElementStep(ChannelAddress.fromString(channel), nominalPower);
	}

	private static Stream<Arguments> parallelBoundaryCases() {
		return Stream.of(//
				Arguments.of(0L, Level.LEVEL_0), //
				Arguments.of(1_999L, Level.LEVEL_0), //
				Arguments.of(2_000L, Level.LEVEL_1), // first PARALLEL threshold (A-L1)
				Arguments.of(2_999L, Level.LEVEL_1), //
				Arguments.of(3_000L, Level.LEVEL_2), // second PARALLEL threshold (A-L1 + B-L1)
				Arguments.of(4_999L, Level.LEVEL_2), //
				Arguments.of(5_000L, Level.LEVEL_3), // third PARALLEL threshold (+ A-L2)
				Arguments.of(5_999L, Level.LEVEL_3), //
				Arguments.of(6_000L, Level.LEVEL_4), // fourth PARALLEL threshold (+ B-L2)
				Arguments.of(7_999L, Level.LEVEL_4), //
				Arguments.of(8_000L, Level.LEVEL_5), // fifth PARALLEL threshold (+ A-L3)
				Arguments.of(8_999L, Level.LEVEL_5), //
				Arguments.of(9_000L, Level.LEVEL_6), // sixth PARALLEL threshold (+ B-L3)
				Arguments.of(100_000L, Level.LEVEL_6) // excess above the maximum caps at the highest Level
		);
	}

	@ParameterizedTest(name = "[{index}] excessPower={0} -> {1}")
	@MethodSource("parallelBoundaryCases")
	void getRequiredLevelFromPowerAtParallelThresholds(long excessPower, Level expected) throws Exception {
		assertEquals(expected, getRequiredLevelFromPower(excessPower, parallelSteps()));
	}

	private static Stream<Arguments> sequentialBoundaryCases() {
		return Stream.of(//
				Arguments.of(0L, Level.LEVEL_0), //
				Arguments.of(1_999L, Level.LEVEL_0), //
				Arguments.of(2_000L, Level.LEVEL_1), //
				Arguments.of(3_999L, Level.LEVEL_1), //
				Arguments.of(4_000L, Level.LEVEL_2), //
				Arguments.of(5_999L, Level.LEVEL_2), //
				Arguments.of(6_000L, Level.LEVEL_3), //
				Arguments.of(6_999L, Level.LEVEL_3), //
				Arguments.of(7_000L, Level.LEVEL_4), //
				Arguments.of(7_999L, Level.LEVEL_4), //
				Arguments.of(8_000L, Level.LEVEL_5), //
				Arguments.of(8_999L, Level.LEVEL_5), //
				Arguments.of(9_000L, Level.LEVEL_6));
	}

	private static List<HeatingElementStep> sequentialSteps() throws OpenemsNamedException {
		// SEQUENTIAL: A-L1, A-L2, A-L3, B-L1, B-L2, B-L3 -> 2000, 2000, 2000, 1000,
		// 1000, 1000
		return List.of(//
				step("io0/InputOutput0", 2000), step("io0/InputOutput1", 2000), step("io0/InputOutput2", 2000), //
				step("io0/InputOutput3", 1000), step("io0/InputOutput4", 1000), step("io0/InputOutput5", 1000));
	}

	@ParameterizedTest(name = "[{index}] excessPower={0} -> {1}")
	@MethodSource("sequentialBoundaryCases")
	void getRequiredLevelFromPowerAtSequentialThresholds(long excessPower, Level expected) throws Exception {
		assertEquals(expected, getRequiredLevelFromPower(excessPower, sequentialSteps()));
	}
}
