package io.openems.edge.evcs.ocpp.server;

public enum SampleValueMeasurand {
	
	/**
	 * Instantaneous current flow from EV
	 */
	CURRENT_EXPORT("Current.Export"), 
	
	/**
	 * Instantaneous current flow to EV
	 */
	CURRENT_IMPORT("Current.IMPORT"), 
	
	/**
	 * Maximum current offered to EV
	 */
	CURRENT_OFFERED("Current.Offered"),  
	
	/**
	 * Numerical value read from the "active electrical energy" (Wh or kWh) register of the (most authoritative) electrical meter measuring energy exported (to the grid).
	 */
	ENERGY_ACTIVE_EXPORT_REGISTER("Energy.Active.Export.Register"),  
	
	/**
	 * Numerical value read from the "active electrical energy" (Wh or kWh) register of the (most authoritative) electrical meter measuring energy imported (from the grid supply).
	 */
	ENERGY_ACTIVE_IMPORT_REGISTER("Energy.Active.Import.Register"), 
	
	/**
	 * Numerical value read from the "reactive electrical energy" (VARh or kVARh) register of the (most authoritative) electrical meter measuring energy exported (to the grid).
	 */
	ENERGY_REACTIVE_EXPORT_REGISTER("Energy.Reactive.Export.Register"), 
	
	/**
	 * Numerical value read from the "reactive electrical energy" (VARh or kVARh) register of the (most authoritative) electrical meter measuring energy imported (from the grid supply).
	 */
	ENERGY_REACTIVE_IMPORT_REGISTER("Energy.Reactive.Import.Register"), 
	
	/**
	 * Absolute amount of "active electrical energy" (Wh or kWh) exported (to the grid) during an associated time "interval", 
	 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	ENERGY_ACTIVE_EXPORT_INTERVAL("Energy.Active.Export.Interval"), 
	
	/**
	 * Absolute amount of "active electrical energy" (Wh or kWh) imported (from the grid supply) during an associated time "interval", 
	 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	ENERGY_ACTIVE_IMPORT_INTERVAL("Energy.Active.Import.Interval"),
	
	/**
	 * Absolute amount of "reactive electrical energy" (VARh or kVARh) exported (to the grid) during an associated time "interval", 
	 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	ENERGY_REACTIVE_EXPORT_INTERVAL("Energy.Reactive.Export.Interval"),
	
	/**
	 * Absolute amount of "reactive electrical energy" (VARh or kVARh) imported (from the grid supply) during an associated time "interval", 
	 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	ENERGY_REACTIVE_IMPORT_INTERVAL("Energy.Reactive.Import.Interval"),
	
	/**
	 * Instantaneous reading of powerline frequency. NOTE: OCPP 1.6 does not have a UnitOfMeasure for frequency, the UnitOfMeasure for any SampledValue with measurand: Frequency is Hertz.
	 */
	FREQUENCY("Frequency"),
	
	/**
	 * Instantaneous active power exported by EV. (W or kW)
	 */
	POWER_ACTIVE_EXPORT("Power.Active.Export"),
	
	/**
	 * Instantaneous active power imported by EV. (W or kW)
	 */
	POWER_ACTIVE_IMPORT("Power.Active.Import"),
	
	/**
	 * Instantaneous power factor of total energy flow
	 */
	POWER_FACTOR("Power.Factor"),
	
	/**
	 * Maximum power offered to EV
	 */
	POWER_OFFERED("Power.Offered"),
	
	/**
	 * Instantaneous reactive power exported by EV. (var or kvar)
	 */
	POWER_REACTIVE_EXPORT("Power.Reactive.Export"),
	
	/**
	 * Instantaneous reactive power imported by EV. (var or kvar)
	 */
	POWER_REACTIVE_IMPORT("Power.Reactive.Import"),
	
	/**
	 * Fan speed in RPM
	 */
	RPM("RPM"),
	
	/**
	 * State of charge of charging vehicle in percentage
	 */
	SOC("SoC"),
	
	/**
	 * Temperature reading inside Charge Point.
	 */
	TEMPERATURE("Temperature"),
	
	/**
	 * Instantaneous AC RMS supply voltage
	 */
	VOLTAGE("Voltage");
	
	String ocppValue;
	
	private SampleValueMeasurand(String ocppValue) {
		this.ocppValue = ocppValue;
	}
	
	public String getOcppValue() {
		return this.ocppValue;
	}	
}
