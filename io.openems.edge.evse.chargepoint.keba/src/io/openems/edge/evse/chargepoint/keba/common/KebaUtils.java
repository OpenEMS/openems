package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public final class KebaUtils {

	private final Keba parent;

	private final CalculateEnergyFromPower calculateEnergyL1;
	private final CalculateEnergyFromPower calculateEnergyL2;
	private final CalculateEnergyFromPower calculateEnergyL3;

	public KebaUtils(Keba keba) {
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

	/**
	 * Called on {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_PROCESS_IMAGE}.
	 */
	public void onBeforeProcessImage() {
		final var keba = this.parent;

		// Calculate Energy values for L1/L2/L3
		this.calculateEnergyL1.update(keba.getActivePowerL1Channel().getNextValue().get());
		this.calculateEnergyL2.update(keba.getActivePowerL2Channel().getNextValue().get());
		this.calculateEnergyL3.update(keba.getActivePowerL3Channel().getNextValue().get());
	}

	/**
	 * Called by {@link OpenemsComponent#debugLog()}.
	 * 
	 * @return the debugLog string
	 */
	public String debugLog() {
		final var keba = this.parent;

		var b = new StringBuilder() //
				.append("L:").append(keba.getActivePower().asString());
		if (!keba.isReadOnly()) {
			b //
					.append("|SetCurrent:") //
					.append(keba.channel(Keba.ChannelId.DEBUG_SET_CHARGING_CURRENT).value().asString()) //
					.append("|SetEnable:") //
					.append(keba.channel(Keba.ChannelId.DEBUG_SET_ENABLE).value().asString());
		}
		return b.toString();
	}
}
