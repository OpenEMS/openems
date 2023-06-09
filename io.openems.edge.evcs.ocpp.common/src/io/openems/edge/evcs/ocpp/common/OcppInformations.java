package io.openems.edge.evcs.ocpp.common;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.SocEvcs;

public enum OcppInformations {

	/**
	 * Instantaneous current flow from EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_EXPORT("Current.Export", MeasuringEvcs.ChannelId.CURRENT_TO_GRID),

	/**
	 * Instantaneous current flow to EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_IMPORT("Current.Import", MeasuringEvcs.ChannelId.CURRENT_TO_EV),

	/**
	 * Maximum current offered to EV in mA.
	 */
	CORE_METER_VALUES_CURRENT_OFFERED("Current.Offered", MeasuringEvcs.ChannelId.CURRENT_OFFERED),

	/**
	 * Numerical value read from the "active electrical energy" (Wh) register of the
	 * (most authoritative) electrical meter measuring the total energy exported (to
	 * the grid).
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_EXPORT_REGISTER("Energy.Active.Export.Register",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_GRID),

	/**
	 * Numerical value read from the "active electrical energy" (Wh) register of the
	 * (most authoritative) electrical meter measuring the total energy imported
	 * (from the grid supply).
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_REGISTER("Energy.Active.Import.Register",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_EV),

	/**
	 * Numerical value read from the "reactive electrical energy" (VARh) register of
	 * the (most authoritative) electrical meter measuring energy exported (to the
	 * grid).
	 */
	CORE_METER_VALUES_ENERGY_REACTIVE_EXPORT_REGISTER("Energy.Reactive.Export.Register",
			MeasuringEvcs.ChannelId.ENERGY_REACTIVE_TO_GRID),

	/**
	 * Numerical value read from the "reactive electrical energy" (VARh) register of
	 * the (most authoritative) electrical meter measuring energy imported (from the
	 * grid supply).
	 */
	CORE_METER_VALUES_ENERGY_REACTIVE_IMPORT_REGISTER("Energy.Reactive.Import.Register",
			MeasuringEvcs.ChannelId.ENERGY_REACTIVE_TO_EV),

	/**
	 * Absolute amount of "active electrical energy" (Wh) exported (to the grid)
	 * during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_EXPORT_INTERVAL("Energy.Active.Export.Interval",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_GRID_INTERVAL),

	/**
	 * Absolute amount of "active electrical energy" (Wh) imported (from the grid
	 * supply) during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_INTERVAL("Energy.Active.Import.Interval",
			MeasuringEvcs.ChannelId.ENERGY_ACTIVE_TO_EV_INTERVAL),

	/**
	 * Absolute amount of "reactive electrical energy" (VARh) exported (to the grid)
	 * during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	CORE_METER_VALUES_ENERGY_REACTIVE_EXPORT_INTERVAL("Energy.Reactive.Export.Interval",
			MeasuringEvcs.ChannelId.ENERGY_REACTIVE_TO_GRID_INTERVAL),

	/**
	 * Absolute amount of "reactive electrical energy" (VARh) imported (from the
	 * grid supply) during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	CORE_METER_VALUES_ENERGY_REACTIVE_IMPORT_INTERVAL("Energy.Reactive.Import.Interval",
			MeasuringEvcs.ChannelId.ENERGY_REACTIVE_TO_EV_INTERVAL),

	/**
	 * Instantaneous reading of powerline frequency. NOTE: OCPP 1.6 does not have a
	 * UnitOfMeasure for frequency, the UnitOfMeasure for any SampledValue with
	 * measurand: Frequency is Hertz.
	 */
	CORE_METER_VALUES_FREQUENCY("Frequency", MeasuringEvcs.ChannelId.FREQUENCY),

	/**
	 * Instantaneous active power exported by EV. (W)
	 */
	CORE_METER_VALUES_POWER_ACTIVE_EXPORT("Power.Active.Export", MeasuringEvcs.ChannelId.POWER_ACTIVE_TO_GRID),

	/**
	 * Instantaneous active power imported by EV. (W)
	 */
	CORE_METER_VALUES_POWER_ACTIVE_IMPORT("Power.Active.Import", Evcs.ChannelId.CHARGE_POWER),

	/**
	 * Instantaneous power factor of total energy flow.
	 */
	CORE_METER_VALUES_POWER_FACTOR("Power.Factor", MeasuringEvcs.ChannelId.POWER_FACTOR),

	/**
	 * Maximum power offered to EV.
	 */
	CORE_METER_VALUES_POWER_OFFERED("Power.Offered", MeasuringEvcs.ChannelId.POWER_OFFERED),

	// TODO: should be combined to REACTIVE_POWER in SymmetricMeter
	/**
	 * Instantaneous reactive power exported by EV. (var)
	 */
	CORE_METER_VALUES_POWER_REACTIVE_EXPORT("Power.Reactive.Export", MeasuringEvcs.ChannelId.POWER_REACTIVE_TO_GRID),

	/**
	 * Instantaneous reactive power imported by EV. (var)
	 */
	CORE_METER_VALUES_POWER_REACTIVE_IMPORT("Power.Reactive.Import", MeasuringEvcs.ChannelId.POWER_REACTIVE_TO_EV),

	/**
	 * Fan speed in RPM.
	 */
	CORE_METER_VALUES_RPM("RPM", MeasuringEvcs.ChannelId.RPM),

	/**
	 * State of charge of charging vehicle in percentage.
	 */
	CORE_METER_VALUES_SOC("SoC", SocEvcs.ChannelId.SOC),

	/**
	 * Temperature reading inside Charge Point.
	 */
	CORE_METER_VALUES_TEMPERATURE("Temperature", MeasuringEvcs.ChannelId.TEMPERATURE),

	/**
	 * Instantaneous AC RMS supply voltage.
	 */
	CORE_METER_VALUES_VOLTAGE("Voltage", MeasuringEvcs.ChannelId.VOLTAGE);

	private final String ocppValue;
	private final ChannelId channelId;

	private OcppInformations(String ocppValue, ChannelId channelId) {
		this.ocppValue = ocppValue;
		this.channelId = channelId;
	}

	public String getOcppValue() {
		return this.ocppValue;
	}

	public ChannelId getChannelId() {
		return this.channelId;
	}
}
