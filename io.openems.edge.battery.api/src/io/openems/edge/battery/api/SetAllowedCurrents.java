package io.openems.edge.battery.api;

import io.openems.edge.common.channel.IntegerReadChannel;

public class SetAllowedCurrents {

	private int lastMaxChargeCurrentFromBmsMilliAmpere = 0;
	private int lastMaxDischargeCurrentFromBmsMilliAmpere = 0;
	
	private Battery battery;
	private CellCharacteristic cellCharacteristic;
	private Settings settings;
	private IntegerReadChannel maxChargeCurrentChannel;
	private IntegerReadChannel maxDischargeCurrentChannel;
	
	public SetAllowedCurrents(//
			Battery battery, //
			CellCharacteristic cellCharacteristic, //
			Settings settings, //
			IntegerReadChannel maxChargeCurrentChannel, //
			IntegerReadChannel maxDischargeCurrentChannel // 
	) {
		super();
		this.battery = battery;
		this.cellCharacteristic = cellCharacteristic;
		this.settings = settings;
		this.maxChargeCurrentChannel = maxChargeCurrentChannel;
		this.maxDischargeCurrentChannel = maxDischargeCurrentChannel;
	}
	
	/**
	 * Calculates the allowed currents.
	 */
	public void act() {

		int maxChargeCurrentFromBmsMilliAmpere = this.maxChargeCurrentChannel.value().orElse(0);

		// limit increasing
		if (maxChargeCurrentFromBmsMilliAmpere > this.lastMaxChargeCurrentFromBmsMilliAmpere + this.settings.getMaxIncreaseMilliAmpere()) {
			maxChargeCurrentFromBmsMilliAmpere = this.lastMaxChargeCurrentFromBmsMilliAmpere + this.settings.getMaxIncreaseMilliAmpere();
		}
		this.lastMaxChargeCurrentFromBmsMilliAmpere = maxChargeCurrentFromBmsMilliAmpere;

		int maxDischargeCurrentFromBmsMilliAmpere = this.maxDischargeCurrentChannel.value().orElse(0);

		// limit increasing
		if (maxDischargeCurrentFromBmsMilliAmpere > this.lastMaxDischargeCurrentFromBmsMilliAmpere + this.settings.getMaxIncreaseMilliAmpere()) {
			maxDischargeCurrentFromBmsMilliAmpere = this.lastMaxDischargeCurrentFromBmsMilliAmpere + this.settings.getMaxIncreaseMilliAmpere();
		}
		this.lastMaxDischargeCurrentFromBmsMilliAmpere = maxDischargeCurrentFromBmsMilliAmpere;

		setMaxAllowedCurrents(this.battery, this.cellCharacteristic, this.settings, maxChargeCurrentFromBmsMilliAmpere / 1000,
				maxDischargeCurrentFromBmsMilliAmpere / 1000);
		
	}
	
	/**
	 * Sets the MaxAllowedCurrents.
	 * @param battery the {@link Battery}
	 * @param cellCharacteristic the {@link CellCharacteristic}
	 * @param settings the {@link Settings}
	 * @param maxChargeCurrentFromBms int
	 * @param maxDischargeCurrentFromBms int
	 */
	public static void setMaxAllowedCurrents(//
			Battery battery, //
			CellCharacteristic cellCharacteristic, //
			Settings settings, //
			int maxChargeCurrentFromBms, //
			int maxDischargeCurrentFromBms //
	) {

		int maxChargeCurrent = maxChargeCurrentFromBms;
		int maxDischargeCurrent = maxDischargeCurrentFromBms;

		if (!areApiValuesPresent(battery)) {
			maxChargeCurrent = 0;
			maxDischargeCurrent = 0;
		} else {
			if (isChargingAlready(battery)) {
				if (isFurtherChargingNecessary(battery, cellCharacteristic,settings)) {
					maxDischargeCurrent = calculateForceDischargeCurrent(battery, settings);
				}
			} else {
				if (isVoltageBelowFinalDischargingVoltage(cellCharacteristic, battery)) {
					if (isVoltageHigherThanForceChargeVoltage(cellCharacteristic, battery)) {
						maxDischargeCurrent = 0;
					} else {
						maxDischargeCurrent = calculateForceDischargeCurrent(battery, settings);
					}
				}
			}

			if (isDischargingAlready(battery)) {
				if (isFurtherDischargingNecessary(cellCharacteristic, battery)) {
					maxChargeCurrent = calculateForceChargeCurrent(battery, settings);
				}
			} else {
				if (isVoltageAboveFinalChargingVoltage(cellCharacteristic, battery)) {
					if (isVoltageLowerThanForceDischargeVoltage(cellCharacteristic, battery)) {
						maxChargeCurrent = 0;
					} else {
						maxChargeCurrent = calculateForceChargeCurrent(battery, settings);
					}
				}
			}
		}

		setChannelsForCharge(maxChargeCurrent, battery);
		setChannelsForDischarge(maxDischargeCurrent, battery);
	}

	protected static void setChannelsForDischarge(int maxDischargeCurrent, Battery battery) {
		battery._setDischargeMaxCurrent(maxDischargeCurrent);

		boolean forceChargeNecessary = maxDischargeCurrent < 0;
		battery._setForceChargeActive(forceChargeNecessary);
	}

	protected static void setChannelsForCharge(int maxChargeCurrent, Battery battery) {
		battery._setChargeMaxCurrent(maxChargeCurrent);

		boolean forceDischargeNecessary = maxChargeCurrent < 0;
		battery._setForceDischargeActive(forceDischargeNecessary);
	}

	protected static boolean isVoltageLowerThanForceDischargeVoltage(CellCharacteristic cellCharacteristic,
			Battery battery) {
		return battery.getMaxCellVoltage().get() < cellCharacteristic.getForceDischargeCellVoltage_mV();
	}

	protected static boolean isVoltageAboveFinalChargingVoltage(CellCharacteristic cellCharacteristic,
			Battery battery) {
		return battery.getMaxCellVoltage().get() > cellCharacteristic.getFinalCellChargeVoltage_mV();
	}

	protected static boolean isVoltageHigherThanForceChargeVoltage(CellCharacteristic cellCharacteristic,
			Battery battery) {
		return battery.getMinCellVoltage().get() > cellCharacteristic.getForceChargeCellVoltage_mV();
	}

	protected static boolean isVoltageBelowFinalDischargingVoltage(CellCharacteristic cellCharacteristic,
			Battery battery) {
		return battery.getMinCellVoltage().get() < cellCharacteristic.getFinalCellDischargeVoltage_mV();
	}

	protected static boolean isFurtherDischargingNecessary(CellCharacteristic cellCharacteristic, Battery battery) {
		if (!battery.getForceDischargeActive().isDefined()) {
			return false;
		}
		return (battery.getForceDischargeActive().get()
				&& battery.getMaxCellVoltage().get() > cellCharacteristic.getFinalCellChargeVoltage_mV());
	}

	protected static boolean isDischargingAlready(Battery battery) {
		return (battery.getForceDischargeActive().isDefined() && battery.getForceDischargeActive().get());
	}

	protected static int calculateForceDischargeCurrent(Battery battery, Settings settings) {
		return calculateForceCurrent(battery, settings);
	}

	protected static int calculateForceChargeCurrent(Battery battery, Settings settings) {
		return calculateForceCurrent(battery, settings);
	}

	protected static int calculateForceCurrent(Battery battery, Settings settings) {
		double capacity = battery.getCapacity().get();
		double voltage = battery.getVoltage().get();
		double power = capacity * settings.getPowerFactor();//POWER_FACTOR;
		double current = power / voltage;
		int value = -(int) Math.max(settings.getMinimumCurrentAmpere(), current);//MINIMUM_CURRENT_AMPERE, current);
		return value;
	}

	protected static boolean isFurtherChargingNecessary(Battery battery, CellCharacteristic cellCharacteristic, Settings settings) {
		if (!battery.getForceChargeActive().isDefined()) {
			return false;
		}
		return battery.getForceChargeActive().get()
				&& battery.getMinCellVoltage().get() < (cellCharacteristic.getFinalCellDischargeVoltage_mV() - settings.getToleranceMilliVolt());
	}

	protected static boolean isChargingAlready(Battery battery) {
		return (battery.getForceChargeActive().isDefined() && battery.getForceChargeActive().get());
	}

	protected static boolean areApiValuesPresent(Battery battery) {
		return battery.getCapacity().isDefined() && battery.getVoltage().isDefined()
				&& battery.getMinCellVoltage().isDefined() && battery.getMaxCellVoltage().isDefined();
	}
}
