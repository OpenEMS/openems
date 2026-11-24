package io.openems.edge.controller.io.heatingelement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.controller.io.heatingelement.enums.Distribution;
import io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements;

/**
 * Verifies that {@link StepBuilder#buildSteps(Config)} builds the ordered list
 * of relay-output {@link HeatingElementStep}s from a {@link Config}.
 */
class StepBuilderTest {

	private static final int POWER_A = 2000;
	private static final int POWER_B = 1000;

	private static MyConfig.Builder configBuilder() {
		return MyConfig.create() //
				.setId("ctrl0") //
				.setOutputChannelPhaseL1("io0/InputOutput0") //
				.setOutputChannelPhaseL2("io0/InputOutput1") //
				.setOutputChannelPhaseL3("io0/InputOutput2") //
				.setPowerOfPhase(POWER_A) //
				.setPowerPerPhaseHeatingElementB(POWER_B) //
				.setOutputChannelPhaseL1HeatingElementB("io0/InputOutput3") //
				.setOutputChannelPhaseL2HeatingElementB("io0/InputOutput4") //
				.setOutputChannelPhaseL3HeatingElementB("io0/InputOutput5");
	}

	@Test
	void buildStepsForOneHeatingElementReturnsThePhaseStepsUnchanged() throws Exception {
		var config = configBuilder() //
				.setNumberOfHeatingElements(NumberOfHeatingElements.ONE) //
				.build();

		var steps = StepBuilder.buildSteps(config);

		assertEquals(List.of(//
				new HeatingElementStep(ChannelAddress.fromString("io0/InputOutput0"), POWER_A), //
				new HeatingElementStep(ChannelAddress.fromString("io0/InputOutput1"), POWER_A), //
				new HeatingElementStep(ChannelAddress.fromString("io0/InputOutput2"), POWER_A)), //
				steps);
	}

	private static HeatingElementStep stepA(String channel) throws OpenemsNamedException {
		return new HeatingElementStep(ChannelAddress.fromString(channel), POWER_A);
	}

	private static HeatingElementStep stepB(String channel) throws OpenemsNamedException {
		return new HeatingElementStep(ChannelAddress.fromString(channel), POWER_B);
	}

	private static Stream<Arguments> twoHeatingElementsDistributionCases() throws OpenemsNamedException {
		return Stream.of(//
				// SEQUENTIAL: A-L1 -> A-L2 -> A-L3 -> B-L1 -> B-L2 -> B-L3
				Arguments.of(Distribution.SEQUENTIAL, List.of(//
						stepA("io0/InputOutput0"), stepA("io0/InputOutput1"), stepA("io0/InputOutput2"), //
						stepB("io0/InputOutput3"), stepB("io0/InputOutput4"), stepB("io0/InputOutput5"))), //
				// PARALLEL: A-L1 -> B-L1 -> A-L2 -> B-L2 -> A-L3 -> B-L3
				Arguments.of(Distribution.PARALLEL, List.of(//
						stepA("io0/InputOutput0"), stepB("io0/InputOutput3"), stepA("io0/InputOutput1"), //
						stepB("io0/InputOutput4"), stepA("io0/InputOutput2"), stepB("io0/InputOutput5"))));
	}

	@ParameterizedTest(name = "[{index}] distribution={0}")
	@MethodSource("twoHeatingElementsDistributionCases")
	void buildStepsForTwoHeatingElementsInterleavesStepsAccordingToDistribution(Distribution distribution,
			List<HeatingElementStep> expectedSteps) throws Exception {
		var config = configBuilder() //
				.setNumberOfHeatingElements(NumberOfHeatingElements.TWO) //
				.setDistribution(distribution) //
				.build();

		var steps = StepBuilder.buildSteps(config);

		assertEquals(expectedSteps, steps);
	}
}
