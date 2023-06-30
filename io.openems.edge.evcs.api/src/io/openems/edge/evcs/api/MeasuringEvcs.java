package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface MeasuringEvcs extends Evcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Current to grid (export).
		 *
		 * <p>
		 * Instantaneous current flow from EV
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Instantaneous current flow from EV")),

		/**
		 * Current to ev (import).
		 *
		 * <p>
		 * Instantaneous current flow to EV
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_TO_EV(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Instantaneous current flow to EV")),

		/**
		 * Current offered.
		 *
		 * <p>
		 * Maximum current offered to EV
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * </ul>
		 */
		CURRENT_OFFERED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Current offered")),

		/**
		 * Active energy to grid (export).
		 *
		 * <p>
		 * Numerical value read from the "active electrical energy" (Wh) register of the
		 * (most authoritative) electrical meter measuring the total energy exported (to
		 * the grid).
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: DOUBLE
		 * <li>Unit: Wh
		 * </ul>
		 */
		ENERGY_ACTIVE_TO_GRID(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Active energy to grid")),

		/**
		 * Active energy to ev (import).
		 *
		 * <p>
		 * Numerical value read from the "active electrical energy" (Wh) register of the
		 * (most authoritative) electrical meter measuring the total energy imported
		 * (from the grid supply).
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: DOUBLE
		 * <li>Unit: Wh
		 * </ul>
		 */
		ENERGY_ACTIVE_TO_EV(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Active energy to ev")),

		/**
		 * Reactive energy to grid (export).
		 *
		 * <p>
		 * Numerical value read from the "reactive electrical energy" (VARh or kVARh)
		 * register of the (most authoritative) electrical meter measuring energy
		 * exported (to the grid).
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: DOUBLE
		 * <li>Unit: VARh
		 * </ul>
		 */
		ENERGY_REACTIVE_TO_GRID(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Energy.Reactive.Export.Register")),

		/**
		 * Reactive energy to EV (import).
		 *
		 * <p>
		 * Numerical value read from the "reactive electrical energy" (VARh or kVARh)
		 * register of the (most authoritative) electrical meter measuring energy
		 * imported (from the grid supply).
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: DOUBLE
		 * <li>Unit: VARh
		 * </ul>
		 */
		ENERGY_REACTIVE_TO_EV(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Energy.Reactive.Import.Register")),

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
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: DOUBLE
		 * <li>Unit: Wh
		 * </ul>
		 */
		ENERGY_ACTIVE_TO_GRID_INTERVAL(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Energy.Active.Export.Interval")),

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
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: DOUBLE
		 * <li>Unit: Wh
		 * </ul>
		 */
		ENERGY_ACTIVE_TO_EV_INTERVAL(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
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
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: DOUBLE
		 * <li>Unit: VARh
		 * </ul>
		 */
		ENERGY_REACTIVE_TO_GRID_INTERVAL(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Energy.Reactive.Export.Interval")),

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
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: DOUBLE
		 * <li>Unit: VARh
		 * </ul>
		 */
		ENERGY_REACTIVE_TO_EV_INTERVAL(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Energy.Reactive.Import.Interval")),

		/**
		 * Frequency.
		 *
		 * <p>
		 * Instantaneous reading of powerline frequency. NOTE: OCPP 1.6 does not have a
		 * UnitOfMeasure for frequency, the UnitOfMeasure for any SampledValue with
		 * measurand: Frequency is Hertz.
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: String
		 * <li>Unit: Hz
		 * </ul>
		 */
		FREQUENCY(Doc.of(OpenemsType.STRING) //
				.unit(Unit.HERTZ) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Frequency")),

		/**
		 * Active power to grid (export)
		 *
		 * <p>
		 * Instantaneous active power exported by EV. (W or kW)
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		POWER_ACTIVE_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power.Active.Export")),

		/**
		 * Power factor.
		 *
		 * <p>
		 * Instantaneous power factor of total energy flow
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: String
		 * </ul>
		 */
		POWER_FACTOR(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power.Factor")),

		/**
		 * Power offered.
		 *
		 * <p>
		 * Maximum power offered to EV
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		POWER_OFFERED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power.Offered")),

		/**
		 * Reactive power to grid (export).
		 *
		 * <p>
		 * Instantaneous reactive power exported by EV. (var or kvar)
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: VAR
		 * </ul>
		 */
		POWER_REACTIVE_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power.Reactive.Export")),

		/**
		 * Reactive power to EV (import).
		 *
		 * <p>
		 * Instantaneous reactive power imported by EV. (var or kvar)
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: Integer
		 * <li>Unit: VAR
		 * </ul>
		 */
		POWER_REACTIVE_TO_EV(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power.Reactive.Import")),

		/**
		 * Fan speed.
		 *
		 * <p>
		 * Fan speed in RPM
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: String
		 * </ul>
		 */
		RPM(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Fan speed")),

		/**
		 * Voltage.
		 *
		 * <p>
		 * Instantaneous AC RMS supply voltage.
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: String
		 * </ul>
		 */
		VOLTAGE(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Voltage")),

		/**
		 * Temperature.
		 *
		 * <p>
		 * Temperature reading inside Charge Point.
		 *
		 * <ul>
		 * <li>Interface: MeasuringEvcs
		 * <li>Readable
		 * <li>Type: STRING
		 * <li>Unit: C
		 * </ul>
		 */
		TEMPERATURE(Doc.of(OpenemsType.STRING) //
				.unit(Unit.DEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Temperature"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
