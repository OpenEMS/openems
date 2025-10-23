package io.openems.edge.evcs.keba.udp;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static io.openems.edge.evcs.api.Phases.THREE_PHASE;
import static io.openems.edge.evcs.api.Status.CHARGING;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedActivePowerChannels;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedCurrentChannels;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedVoltageChannels;
import static java.lang.Math.round;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evse.chargepoint.keba.common.AbstractUdpReadHandler;
import io.openems.edge.evse.chargepoint.keba.common.EvcsKeba;
import io.openems.edge.evse.chargepoint.keba.common.KebaUdp;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;

public class ReadHandler extends AbstractUdpReadHandler<EvcsKebaUdpImpl> {

	public ReadHandler(EvcsKebaUdpImpl parent) {
		super(parent);
	}

	@Override
	protected void handleReport2(ChargingState chargingState, CableState cableState) {
		final var keba = this.parent;

		// Set Evcs status
		var status = switch (cableState) {
		case PLUGGED_ON_WALLBOX, PLUGGED_ON_WALLBOX_AND_LOCKED, PLUGGED_EV_NOT_LOCKED, UNDEFINED, UNPLUGGED //
			-> Status.NOT_READY_FOR_CHARGING;

		case PLUGGED_AND_LOCKED -> {
			/*
			 * Check if the maximum energy limit is reached, informs the user and sets the
			 * status
			 */
			var limit = keba.getSetEnergyLimit().get();
			var energy = keba.getEnergySession().get();
			yield getStatus(chargingState, limit, energy);
		}
		};

		setValue(keba, Evcs.ChannelId.STATUS, status);
		setValue(keba, KebaUdp.ChannelId.CHARGINGSTATION_STATE_ERROR, status == Status.ERROR);
	}

	@Override
	protected void handleReport3(ActivePowerPerPhase appp) {
		final var keba = this.parent;

		// Round power per phase and apply rotated phases
		setPhaseRotatedVoltageChannels(keba, appp.voltageL1(), appp.voltageL2(), appp.voltageL3());
		setPhaseRotatedCurrentChannels(keba, appp.currentL1(), appp.currentL2(), appp.currentL3());
		setPhaseRotatedActivePowerChannels(keba, appp.activePowerL1(), appp.activePowerL2(), appp.activePowerL3());

		final var phases = evaluatePhaseCountFromCurrent(appp.currentL1(), appp.currentL2(), appp.currentL3());
		keba._setPhases(phases);
		if (phases != null) {
			keba._setStatus(CHARGING);
		}

		/*
		 * Set FIXED_MAXIMUM_HARDWARE_POWER of Evcs - this is setting internally the
		 * dynamically calculated MAXIMUM_HARDWARE_POWER including the current used
		 * phases.
		 */
		Channel<Integer> maxDipSwitchLimitChannel = keba.channel(KebaUdp.ChannelId.DIP_SWITCH_MAX_HW);
		int maxDipSwitchPowerLimit = round(maxDipSwitchLimitChannel.value() //
				.orElse(Evcs.DEFAULT_MAXIMUM_HARDWARE_CURRENT) / 1000f) * Evcs.DEFAULT_VOLTAGE * THREE_PHASE.getValue();

		// Minimum of hardware setting and component configuration will be set.
		int maximumHardwareLimit = Math.min(maxDipSwitchPowerLimit, keba.getConfiguredMaximumHardwarePower());

		keba._setFixedMaximumHardwarePower(maximumHardwareLimit);

		/*
		 * Set FIXED_MINIMUM_HARDWARE_POWER of Evcs - this is setting internally the
		 * dynamically calculated MINIMUM_HARDWARE_POWER including the current used
		 * phases.
		 */
		keba._setFixedMinimumHardwarePower(keba.getConfiguredMinimumHardwarePower());
	}

	/**
	 * Calculates the Status based on the raw status and energylimit.
	 * 
	 * @param chargingState the {@link ChargingState}
	 * @param limit         the set energy limit
	 * @param energy        the current energy session
	 * @return the mapped Status
	 */
	public static Status getStatus(ChargingState chargingState, Integer limit, Integer energy) {
		if (limit != null && energy != null && energy >= limit && limit != 0) {
			return Status.ENERGY_LIMIT_REACHED;
		}
		return switch (chargingState) {
		case UNDEFINED -> Status.UNDEFINED;
		case STARTING -> Status.STARTING;
		case NOT_READY_FOR_CHARGING -> Status.NOT_READY_FOR_CHARGING;
		case INTERRUPTED, READY_FOR_CHARGING -> Status.READY_FOR_CHARGING;
		case CHARGING -> Status.CHARGING;
		case ERROR -> Status.ERROR;
		};
	}

	@Override
	protected CableState setCableState(CableState cableState) {
		super.setCableState(cableState);

		// Set deprecated PLUG channel
		this.parent.channel(EvcsKeba.ChannelId.PLUG).setNextValue(cableState);
		return cableState;
	}
}
