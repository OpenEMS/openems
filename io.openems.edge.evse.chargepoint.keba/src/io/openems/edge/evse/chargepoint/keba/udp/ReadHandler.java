package io.openems.edge.evse.chargepoint.keba.udp;

import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.chargepoint.keba.common.AbstractUdpReadHandler;

public class ReadHandler extends AbstractUdpReadHandler<EvseKebaUdpImpl> {

	public ReadHandler(EvseKebaUdpImpl parent) {
		super(parent);
	}

	@Override
	protected void handleReport3(ActivePowerPerPhase appp) {
		final var keba = this.parent;

		// Round power per phase and apply rotated phases
		PhaseRotation.setPhaseRotatedVoltageChannels(keba, appp.voltageL1(), appp.voltageL2(), appp.voltageL3());
		PhaseRotation.setPhaseRotatedCurrentChannels(keba, appp.currentL1(), appp.currentL2(), appp.currentL3());
		PhaseRotation.setPhaseRotatedActivePowerChannels(keba, appp.activePowerL1(), appp.activePowerL2(),
				appp.activePowerL3());
	}
}
