package io.openems.edge.controller.io.heatingelement;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.type.Phase.SinglePhase;

/**
 * Represents one generic relay output step of the heating element.
 *
 * <p>
 * Level N means the first N steps of the ordered step list are switched ON.
 * Unlike {@link PhaseDef} - which is tied to a {@link SinglePhase} for
 * calibration and average-power calculation of the legacy single-heating
 * -element meter - a step only knows the output channel it needs to switch and
 * the nominal power it contributes when switched on. This allows future
 * configurations to combine steps freely (e.g. across two heating elements),
 * without depending on the Phase abstraction.
 *
 * @param outputChannel the {@link ChannelAddress} of the relay output
 *                      controlled by this step
 * @param nominalPower  the nominal power of this step in [W]
 */
public record HeatingElementStep(ChannelAddress outputChannel, int nominalPower) {

}
