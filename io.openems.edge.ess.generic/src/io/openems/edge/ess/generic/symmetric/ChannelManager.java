package io.openems.edge.ess.generic.symmetric;

import java.util.function.Consumer;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;

/**
 * Helper wrapping class to handle everything related to Channels; in particular
 * calculating the Ess-Channels based on the Channels of the Battery and
 * Battery-Inverter. Takes care of registering and unregistering listeners.
 */
public class ChannelManager extends AbstractChannelListenerManager {

	private final GenericManagedSymmetricEss parent;

	public ChannelManager(GenericManagedSymmetricEss parent) {
		this.parent = parent;
	}

	/**
	 * Called on Component activate().
	 * 
	 * @param battery         the {@link Battery}
	 * @param batteryInverter the {@link ManagedSymmetricBatteryInverter}
	 */
	public void activate(Battery battery, ManagedSymmetricBatteryInverter batteryInverter) {
		/*
		 * Battery
		 */
		final Consumer<Value<Integer>> allowedChargeDischargeCallback = (value) -> {
			// TODO: find proper efficiency factor to calculate AC Charge/Discharge limits
			// from DC
			final double efficiencyFactor = 0.95;

			Value<Integer> chargeMaxCurrent = battery.getChargeMaxCurrentChannel().getNextValue();
			Value<Integer> dischargeMaxCurrent = battery.getDischargeMaxCurrentChannel().getNextValue();
			Value<Integer> voltage = battery.getVoltageChannel().getNextValue();

			if (voltage.isDefined() && dischargeMaxCurrent.isDefined() && chargeMaxCurrent.isDefined()) {
				// efficiency factor is not considered in chargeMaxCurrent (DC Power > AC Power)
				this.parent._setAllowedChargePower(//
						(int) (chargeMaxCurrent.get() * voltage.get() * -1));
				this.parent._setAllowedDischargePower(//
						(int) (dischargeMaxCurrent.get() * voltage.get() * efficiencyFactor));
			} else {
				this.parent._setAllowedChargePower(0);
				this.parent._setAllowedDischargePower(0);
			}
		};

		this.addOnSetNextValueListener(battery, Battery.ChannelId.DISCHARGE_MIN_VOLTAGE,
				allowedChargeDischargeCallback);
		this.addOnSetNextValueListener(battery, Battery.ChannelId.DISCHARGE_MAX_CURRENT,
				allowedChargeDischargeCallback);
		this.addOnSetNextValueListener(battery, Battery.ChannelId.CHARGE_MAX_VOLTAGE, allowedChargeDischargeCallback);
		this.addOnSetNextValueListener(battery, Battery.ChannelId.CHARGE_MAX_CURRENT, allowedChargeDischargeCallback);
		this.addCopyListener(battery, //
				Battery.ChannelId.CAPACITY, //
				SymmetricEss.ChannelId.CAPACITY);
		this.addCopyListener(battery, //
				Battery.ChannelId.SOC, //
				SymmetricEss.ChannelId.SOC);

		/*
		 * Battery-Inverter
		 */
		this.<Long>addCopyListener(batteryInverter, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY, //
				SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
		this.<Long>addCopyListener(batteryInverter, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
				SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
		this.<Long>addCopyListener(batteryInverter, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, //
				SymmetricEss.ChannelId.ACTIVE_POWER);
		this.<Long>addCopyListener(batteryInverter, //
				SymmetricBatteryInverter.ChannelId.GRID_MODE, //
				SymmetricEss.ChannelId.GRID_MODE);
		this.<Long>addCopyListener(batteryInverter, //
				SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER, //
				SymmetricEss.ChannelId.MAX_APPARENT_POWER);
		this.<Long>addCopyListener(batteryInverter, //
				SymmetricBatteryInverter.ChannelId.REACTIVE_POWER, //
				SymmetricEss.ChannelId.REACTIVE_POWER);
	}

	/**
	 * Adds a Copy-Listener. It listens on setNextValue() and copies the value to
	 * the target channel.
	 * 
	 * @param <T>             the Channel-Type
	 * @param sourceComponent the source component - Battery or BatteryInverter
	 * @param sourceChannelId the source ChannelId
	 * @param targetChannelId the target ChannelId
	 */
	private <T> void addCopyListener(OpenemsComponent sourceComponent, ChannelId sourceChannelId,
			ChannelId targetChannelId) {
		this.<T>addOnSetNextValueListener(sourceComponent, sourceChannelId, (value) -> {
			Channel<T> targetChannel = this.parent.channel(targetChannelId);
			targetChannel.setNextValue(value);
		});
	}

}
