package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.evse.api.SingleThreePhase.SINGLE_PHASE;
import static io.openems.edge.evse.api.SingleThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.chargepoint.Profile.ApplySetPoint.Ability.MILLI_AMPERE;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.common.types.Tuple;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChargeParams;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.evse.chargepoint.keba.common.enums.ChargingState;
import io.openems.edge.evse.chargepoint.keba.common.enums.SetEnable;
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
	 * Applies a Charging Current.
	 * 
	 * @param actions the {@link ChargePointActions}
	 */
	public void handleApplyCharge(ChargePointActions actions) {
		// TODO this apply method should use a StateMachine. Consider having the
		// StateMachine inside EVSE Single-Controller

		// TODO Phase Switch Three-to-Single is always possible without interruption
		// TODO Allow Phase Switch always if no car is connected
		// final var p = EvseChargePointKebaImpl.this;

		final var keba = this.parent;
		final var current = actions.getApplySetPointInMilliAmpere().value();
		final var now = Instant.now();
		if (this.previousCurrent != null && Duration.between(this.previousCurrent.a(), now).getSeconds() < 5) {
			return;
		}
		this.previousCurrent = Tuple.of(now, current);

		try {
			var setEnable = keba.getSetEnableChannel();
			if (current == 0) {
				setEnable.setNextWriteValue(SetEnable.DISABLE);
			} else {
				setEnable.setNextWriteValue(SetEnable.ENABLE);
				keba.getSetChargingCurrentChannel().setNextWriteValue(current);
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
	public ChargeParams getChargeParams(CommonConfig config) {
		if (config == null || config.readOnly()) {
			return null;
		}
		final var phases = this.getWiring(config);
		if (config == null || config.readOnly() || phases == null) {
			return null;
		}

		var singlePhaseLimit = new Limit(SINGLE_PHASE, 6000, 32000);
		var threePhaseLimit = new Limit(THREE_PHASE, 6000, 32000);

		var limit = switch (phases) {
		case SINGLE_PHASE -> singlePhaseLimit;
		case THREE_PHASE -> threePhaseLimit;
		};

		var abilities = ChargePointAbilities.create() //
				.applySetPointIn(MILLI_AMPERE) //
				.build();

		final var keba = this.parent;
		return new ChargeParams(keba.getIsReadyForCharging(), limit, abilities);
	}

	private SingleThreePhase getWiring(CommonConfig config) {
		final var keba = this.parent;

		// Handle P30 with S10
		switch (config.p30s10PhaseSwitching()) {
		case NOT_AVAILABLE -> doNothing();
		case FORCE_SINGLE_PHASE, FORCE_THREE_PHASE -> {
			var phaseSwitchState = keba.getPhaseSwitchState().actual;
			if (phaseSwitchState == null) {
				return null;
			}
			return phaseSwitchState;
		}
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
}
