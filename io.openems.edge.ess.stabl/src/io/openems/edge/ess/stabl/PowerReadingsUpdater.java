package io.openems.edge.ess.stabl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.IntegerReadChannel;

/**
 * Processes raw Modbus power and voltage readings and pushes them into the
 * OpenEMS channel model.
 *
 * <p>
 * Wires up {@code onChange} listeners in {@link #registerListeners()} and
 * recalculates allowed-charge/-discharge limits ({@link #updateAllowedPower()})
 * and per-phase active power ({@link #updatePower()}) whenever the underlying
 * channel values change.
 * </p>
 */
class PowerReadingsUpdater {

	private final Logger log = LoggerFactory.getLogger(PowerReadingsUpdater.class);
	private final EssStablImpl parent;

	PowerReadingsUpdater(EssStablImpl parent) {
		this.parent = parent;
	}

	void registerListeners() {
		getGridVoltageChannelIds().forEach(//
				channelId -> this.parent.channel(channelId).onChange((o, n) -> {
					this.updateAllowedPower();
				}));
		getAssymetricPowerChannelIds().forEach(//
				channelId -> this.parent.channel(channelId).onChange((o, n) -> {
					this.updatePower();
				}));
		// Also re-trigger when current limits arrive (they may arrive after voltage stabilises)
		List.of(EssStabl.ChannelId.CURRENT_LIMIT_CHARGE_SYSTEM, EssStabl.ChannelId.CURRENT_LIMIT_DISCHARGE_SYSTEM)
				.forEach(channelId -> this.parent.channel(channelId).onChange((o, n) -> {
					this.updateAllowedPower();
				}));
	}

	void updateAllowedPower() {
		var sumVolt = this.calculateSumVoltage();

		if (sumVolt == 0) {
			this.log.debug("No valid voltage readings available, skipping power limit update");
			return;
		}

		var chargeCurrent = this.parent.getCurrentLimitChargeSystem();
		if (chargeCurrent.isDefined()) {
			this.parent.getAllowedChargePowerChannel().setNextValue(-sumVolt * this.parent.getCurrentLimitChargeSystem().get());
		}

		var dischargeCurrent = this.parent.getCurrentLimitDischargeSystem();
		if (dischargeCurrent.isDefined()) {
			this.parent._setAllowedDischargePower(sumVolt * this.parent.getCurrentLimitDischargeSystem().get());
		}
	}

	synchronized void updatePower() {
		Map<EssStabl.ChannelId, Consumer<Integer>> powerSetters = Map.of(//
				EssStabl.ChannelId.STABL_ACTIVE_POWER_L1, this.parent::_setActivePowerL1, //
				EssStabl.ChannelId.STABL_ACTIVE_POWER_L2, this.parent::_setActivePowerL2, //
				EssStabl.ChannelId.STABL_ACTIVE_POWER_L3, this.parent::_setActivePowerL3//
		);

		int totalRawPower = 0;
		boolean anyValidReadings = false;

		for (var entry : powerSetters.entrySet()) {
			Integer rawValue = this.getValueFromChannel(entry.getKey());
			if (rawValue != null) {
				int adjustedPower = convertRegisterValueToSignedPower(rawValue);
				int scaledPower = adjustedPower * EssStableConstants.POWER_MULTIPLIER;

				entry.getValue().accept(scaledPower);
				totalRawPower += adjustedPower;
				anyValidReadings = true;

				this.log.debug("Updated power for {}: raw={}, adjusted={}, scaled={}W", entry.getKey(), rawValue,
						adjustedPower, scaledPower);
			} else {
				this.log.debug("No value available for channel {}", entry.getKey());
			}
		}

		if (anyValidReadings) {
			int totalScaledPower = totalRawPower * EssStableConstants.POWER_MULTIPLIER;
			this.parent._setActivePower(totalScaledPower);
			this.log.debug("Updated total active power: {}W (sum of phases)", totalScaledPower);
		} else {
			this.log.debug("No valid power readings available, total power unchanged");
		}
	}

	private int calculateSumVoltage() {
		return getGridVoltageChannelIds().stream().map(this::getValueFromChannel).filter(Objects::nonNull)
				.mapToInt(Integer::intValue).sum();
	}

	private Integer getValueFromChannel(io.openems.edge.common.channel.ChannelId channelId) {
		IntegerReadChannel readChannel = this.parent.channel(channelId);
		return readChannel.getNextValue().orElse(null);
	}

	private static int convertRegisterValueToSignedPower(int unsignedValue) {
		final int halfMaxValue = EssStableConstants.MAX_REGISTER_VALUE / 2;
		return (unsignedValue > halfMaxValue) ? -1 * (EssStableConstants.MAX_REGISTER_VALUE - unsignedValue + 1)
				: unsignedValue;
	}

	private static List<EssStabl.ChannelId> getGridVoltageChannelIds() {
		return List.of(//
				EssStabl.ChannelId.GRID_VOLTAGE_L1, //
				EssStabl.ChannelId.GRID_VOLTAGE_L2, //
				EssStabl.ChannelId.GRID_VOLTAGE_L3 //
		);
	}

	private static List<EssStabl.ChannelId> getAssymetricPowerChannelIds() {
		return List.of(//
				EssStabl.ChannelId.STABL_ACTIVE_POWER_L1, //
				EssStabl.ChannelId.STABL_ACTIVE_POWER_L2, //
				EssStabl.ChannelId.STABL_ACTIVE_POWER_L3 //
		);
	}

}
