package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;

public interface MeasuringEvcs extends Evcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current to EV (import).
		 * 
		 * <p>
		 * Instantaneous current flow to EV
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_TO_EV(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)
				.text("Instantaneous current flow to EV")),

		/**
		 * Current to grid (export).
		 * 
		 * <p>
		 * Instantaneous current flow from EV
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_TO_GRID(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)
				.text("Instantaneous current flow from EV")),

		/**
		 * Current offered.
		 * 
		 * <p>
		 * Maximum current offered to EV
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_OFFERED(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_ONLY)
				.text("Current offered")),

		/**
		 * Active energy to grid (export).
		 * 
		 * <p>
		 * Numerical value read from the "active electrical energy" (Wh or kWh) register
		 * of the (most authoritative) electrical meter measuring energy exported (to
		 * the grid).
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ENERGY_ACTIVE_TO_GRID_REGISTER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS)
				.accessMode(AccessMode.READ_ONLY).text("Energy.Active.Export.Register")),

		// Import is in ENERGY_SESSION in Evcs

		/**
		 * Reactive energy to grid (export).
		 * 
		 * <p>
		 * Numerical value read from the "reactive electrical energy" (VARh or kVARh)
		 * register of the (most authoritative) electrical meter measuring energy
		 * exported (to the grid).
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: VARh
		 * </ul>
		 */
		ENERGY_REACTIVE_TO_GRID_REGISTER(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)
				.accessMode(AccessMode.READ_ONLY).text("Energy.Reactive.Export.Register")),

		/**
		 * Reactive energy to EV (import).
		 * 
		 * <p>
		 * Numerical value read from the "reactive electrical energy" (VARh or kVARh)
		 * register of the (most authoritative) electrical meter measuring energy
		 * imported (from the grid supply).
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: VARh
		 * </ul>
		 */
		ENERGY_REACTIVE_TO_EV_REGISTER(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)
				.accessMode(AccessMode.READ_ONLY).text("Energy.Reactive.Import.Register")),

		/**
		 * Active energy to grid (export) in an interval.
		 * 
		 * <p>
		 * Absolute amount of "active electrical energy" (Wh or kWh) exported (to the
		 * grid) during an associated time "interval", specified by a Metervalues
		 * ReadingContext, and applicable interval duration configuration values (in
		 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ENERGY_ACTIVE_TO_GRID_INTERVAL(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS)
				.accessMode(AccessMode.READ_ONLY).text("Energy.Active.Export.Interval")),

		/**
		 * Active energy to EV (import) in an interval.
		 * 
		 * <p>
		 * Absolute amount of "active electrical energy" (Wh or kWh) imported (from the
		 * grid supply) during an associated time "interval", specified by a Metervalues
		 * ReadingContext, and applicable interval duration configuration values (in
		 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ENERGY_ACTIVE_TO_EV_INTERVAL(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)
				.text("Energy.Active.Import.Interval")),

		/**
		 * Reactive energy to grid (export) in an interval.
		 * 
		 * <p>
		 * Absolute amount of "reactive electrical energy" (VARh or kVARh) exported (to
		 * the grid) during an associated time "interval", specified by a Metervalues
		 * ReadingContext, and applicable interval duration configuration values (in
		 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: VARh
		 * </ul>
		 */
		ENERGY_REACTIVE_TO_GRID_INTERVAL(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)
				.accessMode(AccessMode.READ_ONLY).text("Energy.Reactive.Export.Interval")),

		/**
		 * Reactive energy to EV (import) in an interval.
		 * 
		 * <p>
		 * Absolute amount of "reactive electrical energy" (VARh or kVARh) imported
		 * (from the grid supply) during an associated time "interval", specified by a
		 * Metervalues ReadingContext, and applicable interval duration configuration
		 * values (in seconds) for "ClockAlignedDataInterval" and
		 * "MeterValueSampleInterval".
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: VARh
		 * </ul>
		 */
		ENERGY_REACTIVE_TO_EV_INTERVAL(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)
				.accessMode(AccessMode.READ_ONLY).text("Energy.Reactive.Import.Interval")),

		/**
		 * Frequency.
		 * 
		 * <p>
		 * Instantaneous reading of powerline frequency. NOTE: OCPP 1.6 does not have a
		 * UnitOfMeasure for frequency, the UnitOfMeasure for any SampledValue with
		 * measurand: Frequency is Hertz.
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: Hz
		 * </ul>
		 */
		FREQUENCY(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY).text("Frequency")),

		/**
		 * Active power to grid (export)
		 * 
		 * <p>
		 * Instantaneous active power exported by EV. (W or kW)
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		POWER_ACTIVE_TO_GRID(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)
				.text("Power.Active.Export")),

		// Import is in CHARGE_POWER in Evcs

		/**
		 * Power factor.
		 * 
		 * <p>
		 * Instantaneous power factor of total energy flow
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * </ul>
		 */
		POWER_FACTOR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).text("Power.Factor")),

		/**
		 * Power offered.
		 * 
		 * <p>
		 * Maximum power offered to EV
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		POWER_OFFERED(
				Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY).text("Power.Offered")),

		/**
		 * Reactive power to grid (export).
		 * 
		 * <p>
		 * Instantaneous reactive power exported by EV. (var or kvar)
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: VAR
		 * </ul>
		 */
		POWER_REACTIVE_TO_GRID(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)
				.accessMode(AccessMode.READ_ONLY).text("Power.Reactive.Export")),

		/**
		 * Reactive power to EV (import).
		 * 
		 * <p>
		 * Instantaneous reactive power imported by EV. (var or kvar)
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: VAR
		 * </ul>
		 */
		POWER_REACTIVE_TO_EV(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)
				.accessMode(AccessMode.READ_ONLY).text("Power.Reactive.Import")),

		/**
		 * Fan speed.
		 * 
		 * <p>
		 * Fan speed in RPM
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * </ul>
		 */
		RPM(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).text("Fan speed")),

		/**
		 * Temperature.
		 * 
		 * <p>
		 * Temperature reading inside Charge Point.
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: C
		 * </ul>
		 */
		TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)
				.text("Temperature")),

		/**
		 * Voltage.
		 * 
		 * <p>
		 * Instantaneous AC RMS supply voltage
		 * 
		 * <ul>
		 * <li>Interface: OcppEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: V
		 * </ul>
		 */
		VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY).text("Voltage"));

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
	 * Current to EV (import).
	 * 
	 * <p>
	 * Instantaneous current flow to EV.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getCurrentToEV() {
		return this.channel(ChannelId.CURRENT_TO_EV);
	}

	/**
	 * Current to grid (export).
	 * 
	 * <p>
	 * Instantaneous current flow from EV.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getCurrentToGrid() {
		return this.channel(ChannelId.CURRENT_TO_GRID);
	}

	/**
	 * Current offered.
	 * 
	 * <p>
	 * Maximum current offered to EV.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getCurrentOffered() {
		return this.channel(ChannelId.CURRENT_OFFERED);
	}

	/**
	 * Active energy to grid (export).
	 * 
	 * <p>
	 * Numerical value read from the "active electrical energy" (Wh) register of the
	 * (most authoritative) electrical meter measuring energy exported (to the
	 * grid).
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getActiveEnergyToGrid() {
		return this.channel(ChannelId.ENERGY_ACTIVE_TO_GRID_REGISTER);
	}

	/**
	 * Reactive energy to grid (export).
	 * 
	 * <p>
	 * Numerical value read from the "reactive electrical energy" (VARh) register of
	 * the (most authoritative) electrical meter measuring energy exported (to the
	 * grid).
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getReactiveEnergyToGrid() {
		return this.channel(ChannelId.ENERGY_REACTIVE_TO_GRID_REGISTER);
	}

	/**
	 * Reactive energy to EV (import).
	 * 
	 * <p>
	 * Numerical value read from the "reactive electrical energy" (VARh) register of
	 * the (most authoritative) electrical meter measuring energy imported (from the
	 * grid supply).
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getReactiveEnergyToEV() {
		return this.channel(ChannelId.ENERGY_REACTIVE_TO_EV_REGISTER);
	}

	/**
	 * Active energy to grid (export) in an interval.
	 * 
	 * <p>
	 * Absolute amount of "active electrical energy" (Wh) exported (to the grid)
	 * during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getActiveEnergyToGridInInterval() {
		return this.channel(ChannelId.ENERGY_ACTIVE_TO_GRID_INTERVAL);
	}

	/**
	 * Active energy to EV (import) in an interval.
	 * 
	 * <p>
	 * Absolute amount of "active electrical energy" (Wh) imported (from the grid
	 * supply) during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getActiveEnergyToEvInInterval() {
		return this.channel(ChannelId.ENERGY_ACTIVE_TO_EV_INTERVAL);
	}

	/**
	 * Reactive energy to grid (export) in an interval.
	 * 
	 * <p>
	 * Absolute amount of "reactive electrical energy" (VARh) exported (to the grid)
	 * during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getReactiveEnergyToGridInInterval() {
		return this.channel(ChannelId.ENERGY_REACTIVE_TO_GRID_INTERVAL);
	}

	/**
	 * Reactive energy to EV (import) in an interval.
	 * 
	 * <p>
	 * Absolute amount of "reactive electrical energy" (VARh) imported (from the
	 * grid supply) during an associated time "interval", specified by a Metervalues
	 * ReadingContext, and applicable interval duration configuration values (in
	 * seconds) for "ClockAlignedDataInterval" and "MeterValueSampleInterval".
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getReactiveEnergyToEvInInterval() {
		return this.channel(ChannelId.ENERGY_REACTIVE_TO_EV_INTERVAL);
	}

	/**
	 * Frequency.
	 * 
	 * <p>
	 * Instantaneous reading of powerline frequency. NOTE: OCPP 1.6 does not have a
	 * UnitOfMeasure for frequency, the UnitOfMeasure for any SampledValue with
	 * measurand: Frequency is Hertz.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getFrequency() {
		return this.channel(ChannelId.FREQUENCY);
	}

	/**
	 * Active power to grid (export)
	 * 
	 * <p>
	 * Instantaneous active power exported by EV. (W).
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getActivePowerToGrid() {
		return this.channel(ChannelId.POWER_ACTIVE_TO_GRID);
	}

	/**
	 * Power factor.
	 * 
	 * <p>
	 * Instantaneous power factor of total energy flow.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getPowerFactor() {
		return this.channel(ChannelId.POWER_FACTOR);
	}

	/**
	 * Power offered.
	 * 
	 * <p>
	 * Maximum power offered to EV.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getPowerOffered() {
		return this.channel(ChannelId.POWER_OFFERED);
	}

	/**
	 * Reactive power to grid (export).
	 * 
	 * <p>
	 * Instantaneous reactive power exported by EV. (var).
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getReactivePowerToGrid() {
		return this.channel(ChannelId.POWER_REACTIVE_TO_GRID);
	}

	/**
	 * Reactive power to EV (import).
	 * 
	 * <p>
	 * Instantaneous reactive power imported by EV. (var).
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getReactivePowerToEV() {
		return this.channel(ChannelId.POWER_REACTIVE_TO_EV);
	}

	/**
	 * Fan speed.
	 * 
	 * <p>
	 * Fan speed in RPM.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getFanSpeed() {
		return this.channel(ChannelId.RPM);
	}

	/**
	 * Temperature.
	 * 
	 * <p>
	 * Temperature reading inside Charge Point.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getTemperature() {
		return this.channel(ChannelId.TEMPERATURE);
	}

	/**
	 * Voltage.
	 * 
	 * <p>
	 * Instantaneous AC RMS supply voltage.
	 * 
	 * @return IntegerReadChannel
	 */
	public default IntegerReadChannel getVoltage() {
		return this.channel(ChannelId.VOLTAGE);
	}
}
