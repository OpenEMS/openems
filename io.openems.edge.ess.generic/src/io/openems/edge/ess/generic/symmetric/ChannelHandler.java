package io.openems.edge.ess.generic.symmetric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
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
public class ChannelHandler {

	private static class Listener<T> {
		protected final OpenemsComponent component;
		protected final ChannelId channelId;
		protected final Consumer<Value<T>> callback;

		public Listener(OpenemsComponent component, ChannelId channelId, Consumer<Value<T>> callback) {
			super();
			this.component = component;
			this.channelId = channelId;
			this.callback = callback;
		}
	}

	private final GenericManagedSymmetricEss parent;

	private final List<Listener<?>> listeners = new ArrayList<>();

	public ChannelHandler(GenericManagedSymmetricEss parent) {
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
			final double efficiencyFactor = 0.9;

			Value<Integer> dischargeMinVoltage = battery.getDischargeMinVoltageChannel().getNextValue();
			Value<Integer> dischargeMaxCurrent = battery.getDischargeMaxCurrentChannel().getNextValue();
			Value<Integer> chargeMaxCurrent = battery.getChargeMaxCurrentChannel().getNextValue();
			Value<Integer> chargeMaxVoltage = battery.getChargeMaxVoltageChannel().getNextValue();

			if (dischargeMinVoltage.isDefined() && dischargeMaxCurrent.isDefined() && chargeMaxCurrent.isDefined()
					&& chargeMaxVoltage.isDefined()) {
				this.parent._setAllowedChargePower(//
						(int) (chargeMaxCurrent.get() * chargeMaxVoltage.get() * -1 * efficiencyFactor));
				this.parent._setAllowedDischargePower(//
						(int) (dischargeMaxCurrent.get() * dischargeMinVoltage.get() * efficiencyFactor));
			}
		};

		this.addListener(battery, Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, allowedChargeDischargeCallback);
		this.addListener(battery, Battery.ChannelId.DISCHARGE_MAX_CURRENT, allowedChargeDischargeCallback);
		this.addListener(battery, Battery.ChannelId.CHARGE_MAX_VOLTAGE, allowedChargeDischargeCallback);
		this.addListener(battery, Battery.ChannelId.CHARGE_MAX_CURRENT, allowedChargeDischargeCallback);
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
				SymmetricBatteryInverter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
				SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
		this.<Long>addCopyListener(batteryInverter, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
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
	 * Called on deactivate(). Remove all callbacks from Channels.
	 */
	public void deactivate() {
		for (Listener<?> listener : this.listeners) {
			Channel<?> channel = listener.component.channel(listener.channelId);
			channel.removeOnSetNextValueCallback(listener.callback);
		}
	}

	/**
	 * Adds a Listener. Also applies the callback once to make sure it applies
	 * already existing values.
	 * 
	 * @param <T>       the Channel value type
	 * @param component the Component - Battery or BatteryInverter
	 * @param channelId the ChannelId
	 * @param callback  the callback
	 */
	private <T> void addListener(OpenemsComponent component, ChannelId channelId, Consumer<Value<T>> callback) {
		this.listeners.add(new Listener<T>(component, channelId, callback));
		Channel<T> channel = component.channel(channelId);
		channel.onSetNextValue(callback);
		callback.accept(channel.getNextValue());
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
		this.<T>addListener(sourceComponent, sourceChannelId, (value) -> {
			Channel<T> targetChannel = this.parent.channel(targetChannelId);
			targetChannel.setNextValue(value);
		});
	}

}
