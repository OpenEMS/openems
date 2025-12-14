package io.openems.edge.evse.chargepoint.alpitronic;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint.Ability;
import io.openems.edge.evse.chargepoint.alpitronic.common.Alpitronic;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public class AlpitronicsUtils {

	private final Logger log = LoggerFactory.getLogger(EvseAlpitronicImpl.class);
	private final EvseAlpitronic parent;

	private final CalculateEnergyFromPower calculateEnergyL1;
	private final CalculateEnergyFromPower calculateEnergyL2;
	private final CalculateEnergyFromPower calculateEnergyL3;

	public AlpitronicsUtils(EvseAlpitronic alpitronic) {
		this.parent = alpitronic;

		// Prepare Energy-Calculation for L1/L2/L3
		this.calculateEnergyL1 = new CalculateEnergyFromPower(this.parent,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
		this.calculateEnergyL2 = new CalculateEnergyFromPower(this.parent,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
		this.calculateEnergyL3 = new CalculateEnergyFromPower(this.parent,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	}

	protected ChargePointAbilities getChargePointAbilities(Config config) {
		if (config == null) {
			return null;
		}
		if (config.readOnly()) {
			return ChargePointAbilities.create()//
					.build();
		}

		return ChargePointAbilities.create() //
				.setApplySetPoint(new Ability.Watt(SingleOrThreePhase.THREE_PHASE, //
						this.parent.getMinChargePower(), //
						this.parent.getMaxChargePower()))
				.setIsReadyForCharging(this.parent.getIsReadyForCharging()) //
				// Phase Switch not available
				.setPhaseSwitch(null)//
				.build();
	}

	/**
	 * Applies a {@link ChargePointActions}.
	 * 
	 * @param config  the {@link Config}
	 * @param actions the {@link ChargePointActions}
	 */
	public void applyChargePointActions(Config config, ChargePointActions actions) {
		if (config.readOnly()) {
			return;
		}

		this.applySetPoint(actions.getApplySetPointInWatt().value());
	}

	private void applySetPoint(int value) {
		var chargePower = Math.max(value, this.parent.getMinChargePower());
		try {
			this.parent.setApplyChargePowerLimit(chargePower);
		} catch (OpenemsNamedException e) {
			// todo add debuglog of error
		}
	}

	protected void onBeforeProcessImage(Config config) {
		try {
			// Calculate Energy values for L1/L2/L3
			this.calculateEnergyL1.update(this.parent.getActivePowerL1Channel().getNextValue().get());
			this.calculateEnergyL2.update(this.parent.getActivePowerL2Channel().getNextValue().get());
			this.calculateEnergyL3.update(this.parent.getActivePowerL3Channel().getNextValue().get());

			if (!config.readOnly()) {
				var state = evaluateIsReadyForCharging(this.parent.getAvailableStateChannel().getNextValue().asEnum());
				setValue(this.parent, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, state);
			}
		} catch (Exception e) {
			this.log.info("Could not evaluateIsReadyForCharging");
		}
	}

	private static boolean evaluateIsReadyForCharging(AvailableState state) {
		return switch (state) {
		case AVAILABLE, //
				CHARGING, //
				RESERVED ->
			true;

		case PREPARING_EV_READY, //
				PREPARING_TAG_ID_READY, //
				SUSPENDED_EV_SE, //
				SUSPENDED_EV, //
				FAULTED, //
				FINISHING, //
				UNDEFINED, //
				UNAVAILABLE, //
				UNAVAILABLE_CONNECTION_OBJECT, //
				UNAVAILABLE_FW_UPDATE ->
			false;
		};
	}

	/**
	 * Called by {@link OpenemsComponent#debugLog()}.
	 * 
	 * @param config the {@link Config}
	 * @return the debugLog string
	 */
	public String debugLog(Config config) {
		final var alpitronic = this.parent;

		var b = new StringBuilder() //
				.append("L:").append(alpitronic.getActivePower().asString());
		if (!config.readOnly()) {
			b //
					.append("|ApplyChargePowerLimit:") //
					.append(alpitronic.channel(Alpitronic.ChannelId.APPLY_CHARGE_POWER_LIMIT).value().asString())
					.append(alpitronic.channel(Alpitronic.ChannelId.EV_SOC).value().asString());
		}
		return b.toString();
	}

}
