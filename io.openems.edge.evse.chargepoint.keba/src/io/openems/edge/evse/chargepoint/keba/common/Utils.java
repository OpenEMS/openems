package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.common.types.Tuple;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchState;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetEnable;
import io.openems.edge.evse.chargepoint.keba.modbus.EvseChargePointKebaModbus;
import io.openems.edge.evse.chargepoint.keba.udp.EvseChargePointKebaUdp;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public class Utils {

	private final EvseChargePointKeba parent;

	private final CalculateEnergyFromPower calculateEnergyL1;
	private final CalculateEnergyFromPower calculateEnergyL2;
	private final CalculateEnergyFromPower calculateEnergyL3;

	public Utils(EvseChargePointKeba keba) {
		this.parent = keba;

		// Prepare Energy-Calculation for L1/L2/L3
		this.calculateEnergyL1 = new CalculateEnergyFromPower(keba,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
		this.calculateEnergyL2 = new CalculateEnergyFromPower(keba,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
		this.calculateEnergyL3 = new CalculateEnergyFromPower(keba,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

		// Set ReactivePower defaults
		keba._setReactivePower(0);
		keba._setReactivePowerL1(0);
		keba._setReactivePowerL2(0);
		keba._setReactivePowerL3(0);
	}

	private Tuple<Instant, Integer> previousCurrent = null;

	/**
	 * Applies a {@link ChargePointActions}.
	 * 
	 * @param config  the {@link CommonConfig}
	 * @param actions the {@link ChargePointActions}
	 */
	public void handleChargePointActions(CommonConfig config, ChargePointActions actions) {
		if (config.readOnly()) {
			return;
		}
		this.applyPhaseSwitch(actions.phaseSwitch());
		this.applySetPoint(actions.getApplySetPointInMilliAmpere().value());
	}

	private void applyPhaseSwitch(PhaseSwitch phaseSwitch) {
		if (phaseSwitch == null) {
			return;
		}

		final var keba = this.parent;
		try {
			// Set correct PhaseSwitchSource
			final var setPhaseSwitchSource = keba.getSetPhaseSwitchSourceChannel();
			if (keba instanceof EvseChargePointKebaModbus
					&& keba.getPhaseSwitchSource() != PhaseSwitchSource.VIA_MODBUS) {
				setPhaseSwitchSource.setNextWriteValue(PhaseSwitchSource.VIA_MODBUS);
			} else if (keba instanceof EvseChargePointKebaUdp
					&& keba.getPhaseSwitchSource() != PhaseSwitchSource.VIA_UDP) {
				setPhaseSwitchSource.setNextWriteValue(PhaseSwitchSource.VIA_UDP);
			}

			// Apply Phase Switch
			final var setPhaseSwitchState = keba.getSetPhaseSwitchStateChannel();
			setPhaseSwitchState.setNextWriteValue(switch (phaseSwitch) {
			case TO_SINGLE_PHASE -> PhaseSwitchState.SINGLE;
			case TO_THREE_PHASE -> PhaseSwitchState.THREE;
			});
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	private void applySetPoint(int setPointInMilliAmpere) {
		final var keba = this.parent;

		// Apply Charge Current
		final var now = Instant.now();
		if (this.previousCurrent != null && Duration.between(this.previousCurrent.a(), now).getSeconds() < 5) {
			return;
		}
		this.previousCurrent = Tuple.of(now, setPointInMilliAmpere);

		try {
			var setEnable = keba.getSetEnableChannel();
			if (setPointInMilliAmpere == 0) {
				setEnable.setNextWriteValue(SetEnable.DISABLE);
			} else {
				setEnable.setNextWriteValue(SetEnable.ENABLE);
				keba.getSetChargingCurrentChannel().setNextWriteValue(setPointInMilliAmpere);
			}

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the {@link MeterType}.
	 * 
	 * @param config the {@link CommonConfig}
	 * @return the value
	 */
	public static MeterType getMeterType(CommonConfig config) {
		if (config.readOnly()) {
			return MeterType.CONSUMPTION_METERED;
		} else {
			return MeterType.MANAGED_CONSUMPTION_METERED;
		}
	}

	/**
	 * Gets the {@link ChargeParams}.
	 * 
	 * @param config the {@link CommonConfig}
	 * @return the value
	 */
	public ChargePointAbilities getChargePointAbilities(CommonConfig config) {
		if (config == null || config.readOnly()) {
			return null;
		}
		final var phases = this.getWiring(config);
		if (config == null || config.readOnly() || phases == null) {
			return null;
		}

		final var keba = this.parent;
		return ChargePointAbilities.create() //
				// TODO apply actual hardware limit from protocol and/or config
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(phases, 6000, 32000)) //
				.setIsReadyForCharging(keba.getIsReadyForCharging()) //
				.setPhaseSwitch(this.getPhaseSwitchAbility(config)) //
				.build();
	}

	private PhaseSwitch getPhaseSwitchAbility(CommonConfig config) {
		final var keba = this.parent;

		// Set Phase-Switching Ability
		final var phaseSwitchState = keba.getPhaseSwitchState().actual;
		final var phaseSwitchSource = keba.getPhaseSwitchSource();
		if (!config.p30hasS10PhaseSwitching() || config.wiring() == SINGLE_PHASE
				|| phaseSwitchState == null || phaseSwitchSource == PhaseSwitchSource.UNDEFINED) {
			// Phase-Switching not available or still waiting for all required channels
			return null;
		}

		// TODO When switching between the parameters, a cool down time of 5 minutes is
		// required

		// Query and Set correct PhaseSwitchSource
		final var requiredPhaseSwitchSource = keba.getRequiredPhaseSwitchSource();
		if (phaseSwitchSource != requiredPhaseSwitchSource) {
			final var setPhaseSwitchSource = keba.getSetPhaseSwitchSourceChannel();
			try {
				// TODO during manual test it was required to switch to "0" before switching to
				// "3" or "4". Tested successfully via Modbus/TCP with QModMaster
				setPhaseSwitchSource.setNextWriteValue(requiredPhaseSwitchSource);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
			return null;
		}

		return switch (phaseSwitchState) {
		case SINGLE_PHASE -> PhaseSwitch.TO_THREE_PHASE;
		case THREE_PHASE -> PhaseSwitch.TO_SINGLE_PHASE;
		};
	}

	private SingleOrThreePhase getWiring(CommonConfig config) {
		final var keba = this.parent;

		// Handle P30 with S10
		if (config.p30hasS10PhaseSwitching()) {
			return keba.getPhaseSwitchState().actual;
		}

		return config.wiring();
	}

	/**
	 * Called on {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_PROCESS_IMAGE}.
	 */
	public void onBeforeProcessImage() {
		final var keba = this.parent;

		// Updates the IS_READY_FOR_CHARGING state
		var state = evaluateIsReadyForCharging(//
				keba.getCableStateChannel().getNextValue().asEnum(), //
				keba.getChargingStateChannel().getNextValue().asEnum());
		setValue(keba, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, state);

		// Calculate Energy values for L1/L2/L3
		this.calculateEnergyL1.update(keba.getActivePowerL1Channel().getNextValue().get());
		this.calculateEnergyL2.update(keba.getActivePowerL2Channel().getNextValue().get());
		this.calculateEnergyL3.update(keba.getActivePowerL3Channel().getNextValue().get());
	}

	private static boolean evaluateIsReadyForCharging(CableState cableState, ChargingState chargingState) {
		return switch (cableState) {
		case PLUGGED_AND_LOCKED, PLUGGED_EV_NOT_LOCKED //
			-> switch (chargingState) {
			case CHARGING, INTERRUPTED, NOT_READY_FOR_CHARGING, READY_FOR_CHARGING //
				-> true;
			case ERROR, STARTING, UNDEFINED //
				-> false;
			};

		case PLUGGED_ON_WALLBOX, PLUGGED_ON_WALLBOX_AND_LOCKED, UNDEFINED, UNPLUGGED //
			-> false;
		};
	}

	/**
	 * Called by {@link OpenemsComponent#debugLog()}.
	 * 
	 * @param config the {@link CommonConfig}
	 * @return the debugLog string
	 */
	public String debugLog(CommonConfig config) {
		final var keba = this.parent;

		var b = new StringBuilder() //
				.append("L:").append(keba.getActivePower().asString());
		if (!config.readOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(keba.channel(EvseChargePointKeba.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString()) //
					.append("|SetEnable:") //
					.append(keba.channel(EvseChargePointKeba.ChannelId.DEBUG_SET_ENABLE).value().asString());
		}
		return b.toString();
	}
}
