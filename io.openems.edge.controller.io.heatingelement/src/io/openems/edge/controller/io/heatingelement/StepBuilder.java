package io.openems.edge.controller.io.heatingelement;

import java.util.List;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.controller.io.heatingelement.enums.Distribution;
import io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements;

/**
 * Builds the ordered list of relay-output {@link HeatingElementStep}s from a
 * {@link Config}.
 */
public class StepBuilder {

	private StepBuilder() {
	}

	/**
	 * Builds the ordered list of relay-output {@link HeatingElementStep}s from the
	 * given {@link Config}.
	 *
	 * <p>
	 * For {@link NumberOfHeatingElements#ONE} the three Steps of Heating Element A
	 * are returned unchanged. For {@link NumberOfHeatingElements#TWO} the three
	 * Steps of Heating Element B are added, interleaved with the Steps of Heating
	 * Element A according to the configured {@link Distribution}.
	 *
	 * @param config the {@link Config}
	 * @return the ordered list of Steps
	 * @throws OpenemsNamedException on invalid Channel-Address
	 */
	public static List<HeatingElementStep> buildSteps(Config config) throws OpenemsNamedException {
		var stepAL1 = new HeatingElementStep(ChannelAddress.fromString(config.outputChannelPhaseL1()), //
				config.powerPerPhase());
		var stepAL2 = new HeatingElementStep(ChannelAddress.fromString(config.outputChannelPhaseL2()), //
				config.powerPerPhase());
		var stepAL3 = new HeatingElementStep(ChannelAddress.fromString(config.outputChannelPhaseL3()), //
				config.powerPerPhase());

		if (config.numberOfHeatingElements() == NumberOfHeatingElements.ONE) {
			return List.of(stepAL1, stepAL2, stepAL3);
		}

		var stepBL1 = new HeatingElementStep(ChannelAddress.fromString(config.outputChannelPhaseL1HeatingElementB()), //
				config.powerPerPhaseHeatingElementB());
		var stepBL2 = new HeatingElementStep(ChannelAddress.fromString(config.outputChannelPhaseL2HeatingElementB()), //
				config.powerPerPhaseHeatingElementB());
		var stepBL3 = new HeatingElementStep(ChannelAddress.fromString(config.outputChannelPhaseL3HeatingElementB()), //
				config.powerPerPhaseHeatingElementB());

		return switch (config.distribution()) {
		case SEQUENTIAL -> //
			List.of(stepAL1, stepAL2, stepAL3, stepBL1, stepBL2, stepBL3);
		case PARALLEL -> //
			List.of(stepAL1, stepBL1, stepAL2, stepBL2, stepAL3, stepBL3);
		};
	}
}
