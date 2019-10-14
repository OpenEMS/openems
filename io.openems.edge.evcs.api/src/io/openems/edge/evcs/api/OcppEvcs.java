package io.openems.edge.evcs.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.evcs.api.Evcs.ChannelId;

public interface OcppEvcs extends Evcs{

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		/**
		 * Session Id.
		 * 
		 * Id is set if there is a new Session between - the EVCS implemented by this Component and the Server.
		 * If this value is empty, no communication was established
		 */
		CHARGING_SESSION_ID(Doc.of(OpenemsType.STRING).text("Identifies a current Session set by the server")), //
		
		/**
		 * Ocpp id.
		 * 
		 * Id that is defined in every EVCS which implements OCPP
		 */
		OCPP_ID(Doc.of(OpenemsType.STRING).text("OCPP Id of the Charging Station")), //
		
		/**
		 * Current to EV (import).
		 *  
		 * Instantaneous current flow to EV
		 */
		CURRENT_TO_EV(Doc.of(OpenemsType.INTEGER).text("Instantaneous current flow to EV")), 
		
		/**
		 * Current to grid (export).
		 *  
		 * Instantaneous current flow from EV
		 */
		CURRENT_TO_GRID(Doc.of(OpenemsType.INTEGER).text("Instantaneous current flow from EV")), 
				
		/**
		 * Current offered.
		 * 
		 * Maximum current offered to EV
		 */
		CURRENT_OFFERED(Doc.of(OpenemsType.INTEGER).text("Current offered")),  
		
		/**
		 * Active energy to grid (export).
		 * 
		 * Numerical value read from the "active electrical energy" (Wh or kWh) register of the (most authoritative) electrical meter measuring energy exported (to the grid).
		 */
		ENERGY_ACTIVE_TO_GRID_REGISTER(Doc.of(OpenemsType.INTEGER).text("Energy.Active.Export.Register")),  
		
		// Import is in ENERGY_SESSION in Evcs
		
		/**
		 * Reactive energy to grid (export).
		 * 
		 * Numerical value read from the "reactive electrical energy" (VARh or kVARh) register of the (most authoritative) electrical meter measuring energy exported (to the grid).
		 */
		ENERGY_REACTIVE_TO_GRID_REGISTER(Doc.of(OpenemsType.INTEGER).text("Energy.Reactive.Export.Register")), 
		
		/**
		 * Reactive energy to EV (import).
		 * 
		 * Numerical value read from the "reactive electrical energy" (VARh or kVARh) register of the (most authoritative) electrical meter measuring energy imported (from the grid supply).
		 */
		ENERGY_REACTIVE_TO_EV_REGISTER(Doc.of(OpenemsType.INTEGER).text("Energy.Reactive.Import.Register")), 
		
		/**
		 * Active energy to grid (export) in an interval.
		 * 
		 * Absolute amount of "active electrical energy" (Wh or kWh) exported (to the grid) during an associated time "interval", 
		 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
		 */
		ENERGY_ACTIVE_TO_GRID_INTERVAL(Doc.of(OpenemsType.INTEGER).text("Energy.Active.Export.Interval")), 
		
		/**
		 * Active energy to EV (import) in an interval.
		 * 
		 * Absolute amount of "active electrical energy" (Wh or kWh) imported (from the grid supply) during an associated time "interval", 
		 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
		 */
		ENERGY_ACTIVE_TO_EV_INTERVAL(Doc.of(OpenemsType.INTEGER).text("Energy.Active.Import.Interval")),
		
		/**
		 * Reactive energy to grid (export) in an interval.
		 * 
		 * Absolute amount of "reactive electrical energy" (VARh or kVARh) exported (to the grid) during an associated time "interval", 
		 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
		 */
		ENERGY_REACTIVE_TO_GRID_INTERVAL(Doc.of(OpenemsType.INTEGER).text("Energy.Reactive.Export.Interval")),
		
		/**
		 * Reactive energy to EV (import) in an interval.
		 * 
		 * Absolute amount of "reactive electrical energy" (VARh or kVARh) imported (from the grid supply) during an associated time "interval", 
		 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
		 */
		ENERGY_REACTIVE_TO_EV_INTERVAL(Doc.of(OpenemsType.INTEGER).text("Energy.Reactive.Import.Interval")),
		
		/**
		 * Frequency.
		 * 
		 * Instantaneous reading of powerline frequency. NOTE: OCPP 1.6 does not have a UnitOfMeasure for frequency, the UnitOfMeasure for any SampledValue with measurand: Frequency is Hertz.
		 */
		FREQUENCY(Doc.of(OpenemsType.INTEGER).text("Frequency")),
		
		/**
		 * Active power to grid (export)
		 * 
		 * Instantaneous active power exported by EV. (W or kW)
		 */
		POWER_ACTIVE_TO_GRID(Doc.of(OpenemsType.INTEGER).text("Power.Active.Export")),

		// Import is in CHARGE_POWER in Evcs
		
		/**
		 * Power factor.
		 * 
		 * Instantaneous power factor of total energy flow
		 */
		POWER_FACTOR(Doc.of(OpenemsType.INTEGER).text("Power.Factor")),
		
		/**
		 * Power offered.
		 * 
		 * Maximum power offered to EV
		 */
		POWER_OFFERED(Doc.of(OpenemsType.INTEGER).text("Power.Offered")),
		
		/**
		 * Reactive power to grid (export).
		 * 
		 * Instantaneous reactive power exported by EV. (var or kvar)
		 */
		POWER_REACTIVE_TO_GRID(Doc.of(OpenemsType.INTEGER).text("Power.Reactive.Export")),
		
		/**
		 * Reactive power to EV (import).
		 * 
		 * Instantaneous reactive power imported by EV. (var or kvar)
		 */
		POWER_REACTIVE_TO_EV(Doc.of(OpenemsType.INTEGER).text("Power.Reactive.Import")),
		
		/**
		 * Fan speed.
		 * 
		 * Fan speed in RPM
		 */
		RPM(Doc.of(OpenemsType.INTEGER).text("RPM")),
		
		/**
		 * State of charge.
		 * 
		 * State of charge of charging vehicle in percentage
		 */
		SOC(Doc.of(OpenemsType.INTEGER).text("SoC")),
		
		/**
		 * Temperature.
		 * 
		 * Temperature reading inside Charge Point.
		 */
		TEMPERATURE(Doc.of(OpenemsType.INTEGER).text("Temperature")),
		
		/**
		 * Voltage.
		 * 
		 * Instantaneous AC RMS supply voltage
		 */
		VOLTAGE(Doc.of(OpenemsType.INTEGER).text("Voltage"));
		
		
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	/**
	 * Session Id.
	 * 
	 * Id is set if there is a new Session between - the EVCS implemented by this Component and the Server.
	 * If this value is empty, no communication was established
	 */
	public default StringReadChannel getChargingSessionId() {
		return this.channel(ChannelId.CHARGING_SESSION_ID);
	}

	/**
	 * Ocpp id.
	 * 
	 * Id that is defined in every EVCS which implements OCPP
	 */
	public default StringReadChannel getOcppId() {
		return this.channel(ChannelId.OCPP_ID);
	}
	
	/**
	 * Current to EV (import).
	 *  
	 * Instantaneous current flow to EV
	 */
	public default IntegerReadChannel getCurrentToEV() {
		return this.channel(ChannelId.CURRENT_TO_EV);
	}
	
	/**
	 * Current to grid (export).
	 *  
	 * Instantaneous current flow from EV
	 */
	public default IntegerReadChannel getCurrentToGrid() {
		return this.channel(ChannelId.CURRENT_TO_GRID);
	}
	
	/**
	 * Current offered.
	 * 
	 * Maximum current offered to EV
	 */
	public default IntegerReadChannel getCurrentOffered() {
		return this.channel(ChannelId.CURRENT_OFFERED);
	}
	
	/**
	 * Active energy to grid (export).
	 * 
	 * Numerical value read from the "active electrical energy" (Wh or kWh) register of the (most authoritative) electrical meter measuring energy exported (to the grid).
	 */
	public default IntegerReadChannel getActiveEnergyToGrid() {
		return this.channel(ChannelId.ENERGY_ACTIVE_TO_GRID_REGISTER);
	}
	
	/**
	 * Reactive energy to grid (export).
	 * 
	 * Numerical value read from the "reactive electrical energy" (VARh or kVARh) register of the (most authoritative) electrical meter measuring energy exported (to the grid).
	 */
	public default IntegerReadChannel getReactiveEnergyToGrid() {
		return this.channel(ChannelId.ENERGY_REACTIVE_TO_GRID_REGISTER);
	}
	
	/**
	 * Reactive energy to EV (import).
	 * 
	 * Numerical value read from the "reactive electrical energy" (VARh or kVARh) register of the (most authoritative) electrical meter measuring energy imported (from the grid supply).
	 */
	public default IntegerReadChannel getReactiveEnergyToEV() {
		return this.channel(ChannelId.ENERGY_REACTIVE_TO_EV_REGISTER);
	}
	
	/**
	 * Active energy to grid (export) in an interval.
	 * 
	 * Absolute amount of "active electrical energy" (Wh or kWh) exported (to the grid) during an associated time "interval", 
	 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	public default IntegerReadChannel getActiveEnergyToGridInInterval() {
		return this.channel(ChannelId.ENERGY_ACTIVE_TO_GRID_INTERVAL);
	}
	
	/**
	 * Active energy to EV (import) in an interval.
	 * 
	 * Absolute amount of "active electrical energy" (Wh or kWh) imported (from the grid supply) during an associated time "interval", 
	 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	public default IntegerReadChannel getActiveEnergyToEVInInterval() {
		return this.channel(ChannelId.ENERGY_ACTIVE_TO_EV_INTERVAL);
	}
	
	/**
	 * Reactive energy to grid (export) in an interval.
	 * 
	 * Absolute amount of "reactive electrical energy" (VARh or kVARh) exported (to the grid) during an associated time "interval", 
	 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	public default IntegerReadChannel getReactiveEnergyToGridInInterval() {
		return this.channel(ChannelId.ENERGY_REACTIVE_TO_GRID_INTERVAL);
	}
	
	/**
	 * Reactive energy to EV (import) in an interval.
	 * 
	 * Absolute amount of "reactive electrical energy" (VARh or kVARh) imported (from the grid supply) during an associated time "interval", 
	 * specified by a Metervalues ReadingContext, and applicable interval duration configuration values (in seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 */
	public default IntegerReadChannel getReactiveEnergyToEvInInterval() {
		return this.channel(ChannelId.ENERGY_REACTIVE_TO_EV_INTERVAL);
	}
	
	/**
	 * Frequency.
	 * 
	 * Instantaneous reading of powerline frequency. NOTE: OCPP 1.6 does not have a UnitOfMeasure for frequency, the UnitOfMeasure for any SampledValue with measurand: Frequency is Hertz.
	 */
	public default IntegerReadChannel getFrequency() {
		return this.channel(ChannelId.FREQUENCY);
	}
	
	/**
	 * Active power to grid (export)
	 * 
	 * Instantaneous active power exported by EV. (W or kW)
	 */
	public default IntegerReadChannel getActivePowerToGrid() {
		return this.channel(ChannelId.POWER_ACTIVE_TO_GRID);
	}

	/**
	 * Power factor.
	 * 
	 * Instantaneous power factor of total energy flow
	 */
	public default IntegerReadChannel getPowerFactor() {
		return this.channel(ChannelId.POWER_FACTOR);
	}
	
	/**
	 * Power offered.
	 * 
	 * Maximum power offered to EV
	 */
	public default IntegerReadChannel getPowerOffered() {
		return this.channel(ChannelId.POWER_OFFERED);
	}
	
	/**
	 * Reactive power to grid (export).
	 * 
	 * Instantaneous reactive power exported by EV. (var or kvar)
	 */
	public default IntegerReadChannel getReactivePowerToGrid() {
		return this.channel(ChannelId.POWER_REACTIVE_TO_GRID);
	}
	
	/**
	 * Reactive power to EV (import).
	 * 
	 * Instantaneous reactive power imported by EV. (var or kvar)
	 */
	public default IntegerReadChannel getReactivePowerToEV() {
		return this.channel(ChannelId.POWER_REACTIVE_TO_EV);
	}
	
	/**
	 * Fan speed.
	 * 
	 * Fan speed in RPM
	 */
	public default IntegerReadChannel getFanSpeed() {
		return this.channel(ChannelId.RPM);
	}
	
	/**
	 * State of charge.
	 * 
	 * State of charge of charging vehicle in percentage
	 */
	public default IntegerReadChannel getSoC() {
		return this.channel(ChannelId.SOC);
	}
	
	/**
	 * Temperature.
	 * 
	 * Temperature reading inside Charge Point.
	 */
	public default IntegerReadChannel getTemperature() {
		return this.channel(ChannelId.TEMPERATURE);
	}
	
	/**
	 * Voltage.
	 * 
	 * Instantaneous AC RMS supply voltage
	 */
	public default IntegerReadChannel getVoltage() {
		return this.channel(ChannelId.VOLTAGE);
	}	
}
