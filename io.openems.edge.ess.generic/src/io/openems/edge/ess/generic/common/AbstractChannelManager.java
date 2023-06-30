package io.openems.edge.ess.generic.common;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.SymmetricEss;

/**
 * Helper wrapping class to handle everything related to Channels; in particular
 * calculating the Ess-Channels based on the Channels of the Battery and
 * Battery-Inverter. Takes care of registering and unregistering listeners.
 */
public class AbstractChannelManager<ESS extends SymmetricEss, BATTERY extends Battery, BATTERY_INVERTER extends SymmetricBatteryInverter>
		extends AbstractChannelListenerManager {

	private final ESS parent;
	private final AbstractAllowedChargeDischargeHandler<ESS> allowedChargeDischargeHandler;

	public AbstractChannelManager(ESS parent,
			AbstractAllowedChargeDischargeHandler<ESS> allowedChargeDischargeHandler) {
		this.parent = parent;
		this.allowedChargeDischargeHandler = allowedChargeDischargeHandler;
	}

	/**
	 * Called on Component activate().
	 *
	 * @param clockProvider   the {@link ClockProvider}
	 * @param battery         the {@link Battery}
	 * @param batteryInverter the {@link ManagedSymmetricBatteryInverter}
	 */
	public void activate(ClockProvider clockProvider, Battery battery,
			ManagedSymmetricBatteryInverter batteryInverter) {
		this.addBatteryListener(clockProvider, battery);
		this.addBatteryInverterListener(batteryInverter);
	}

	private void addBatteryInverterListener(ManagedSymmetricBatteryInverter batteryInverter) {
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

		if (batteryInverter instanceof HybridManagedSymmetricBatteryInverter) {
			this.<Long>addCopyListener(batteryInverter, //
					HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY, //
					HybridEss.ChannelId.DC_CHARGE_ENERGY);
			this.<Long>addCopyListener(batteryInverter, //
					HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY, //
					HybridEss.ChannelId.DC_DISCHARGE_ENERGY);
			this.<Long>addCopyListener(batteryInverter, //
					HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, //
					HybridEss.ChannelId.DC_DISCHARGE_POWER);

		} else {
			this.<Long>addCopyListener(batteryInverter, //
					SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY, //
					HybridEss.ChannelId.DC_CHARGE_ENERGY);
			this.<Long>addCopyListener(batteryInverter, //
					SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
					HybridEss.ChannelId.DC_DISCHARGE_ENERGY);
			this.<Long>addCopyListener(batteryInverter, //
					SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, //
					HybridEss.ChannelId.DC_DISCHARGE_POWER);
		}
	}

	private void addBatteryListener(ClockProvider clockProvider, Battery battery) {
		/*
		 * Battery
		 */
		this.addOnSetNextValueListener(battery, Battery.ChannelId.DISCHARGE_MIN_VOLTAGE,
				ignored -> this.allowedChargeDischargeHandler.accept(clockProvider, battery));
		this.addOnSetNextValueListener(battery, Battery.ChannelId.DISCHARGE_MAX_CURRENT,
				ignored -> this.allowedChargeDischargeHandler.accept(clockProvider, battery));
		this.addOnSetNextValueListener(battery, Battery.ChannelId.CHARGE_MAX_VOLTAGE,
				ignored -> this.allowedChargeDischargeHandler.accept(clockProvider, battery));
		this.addOnSetNextValueListener(battery, Battery.ChannelId.CHARGE_MAX_CURRENT,
				ignored -> this.allowedChargeDischargeHandler.accept(clockProvider, battery));
		this.addCopyListener(battery, //
				Battery.ChannelId.CAPACITY, //
				SymmetricEss.ChannelId.CAPACITY);
		this.addCopyListener(battery, //
				Battery.ChannelId.SOC, //
				SymmetricEss.ChannelId.SOC);
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
	protected <T> void addCopyListener(OpenemsComponent sourceComponent, ChannelId sourceChannelId,
			ChannelId targetChannelId) {
		this.<T>addOnSetNextValueListener(sourceComponent, sourceChannelId, value -> {
			Channel<T> targetChannel = this.parent.channel(targetChannelId);
			targetChannel.setNextValue(value);
		});
	}
}
