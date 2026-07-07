package io.openems.edge.io.shelly.common.component;

import java.util.Arrays;
import java.util.Optional;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

public interface ShellyEnergyMeter extends ElectricityMeter, TimedataProvider {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Apparent Power.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE}
		 * </ul>
		 */
		APPARENT_POWER(new IntegerDoc()//
				.unit(Unit.VOLT_AMPERE)), //

		/**
		 * Apparent Power L1.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE}
		 * </ul>
		 */
		APPARENT_POWER_L1(new IntegerDoc()//
				.unit(Unit.VOLT_AMPERE)), //

		/**
		 * Apparent Power L2.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE}
		 * </ul>
		 */
		APPARENT_POWER_L2(new IntegerDoc()//
				.unit(Unit.VOLT_AMPERE)), //

		/**
		 * Apparent Power L3.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT_AMPERE}
		 * </ul>
		 */
		APPARENT_POWER_L3(new IntegerDoc()//
				.unit(Unit.VOLT_AMPERE)), //

		;

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
	 * Channels with errors that can be returned by Shelly Pro 3EM. See <a href=
	 * "https://shelly-api-docs.shelly.cloud/gen2/ComponentsAndServices/EM/#status">shelly
	 * documentation</a> for a list of possible errors.
	 */
	public enum ErrorChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Phase Sequence Error.
		 *
		 * <p>
		 * Represents an error indicating if the sequence of zero-crossing events is
		 * Phase A followed by Phase C followed by Phase B. The regular succession of
		 * these zero-crossing events is Phase A followed by Phase B followed by Phase
		 * C.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: State
		 * </ul>
		 */
		PHASE_SEQUENCE_ERROR(Doc.of(Level.WARNING)//
				.text("Incorrect phase sequence. Expected A-B-C but found A-C-B."), "phase_sequence"),

		/**
		 * Power Meter Failure.
		 *
		 * <p>
		 * Represents a failure in the power meter, potentially leading to inaccurate or
		 * missing measurements.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: State
		 * </ul>
		 */
		POWER_METER_FAILURE(Doc.of(Level.FAULT)//
				.text("Power meter failure; unable to record or measure power accurately."), "power_meter_failure"),

		/**
		 * No Load Error.
		 *
		 * <p>
		 * Indicates that the power meter is in a no-load condition and is not
		 * accumulating the registered energies, therefore, the measured values can be
		 * discarded.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlug
		 * <li>Type: State
		 * </ul>
		 */
		NO_LOAD(Doc.of(Level.FAULT)//
				.text("No load condition detected; the power meter is not accumulating energy."), "no_load"),

		L1_OUT_OF_RANGE_ACTIVE_POWER(Doc.of(Level.WARNING).text("L1 Active Power is out of range."),
				"a_errors(out_of_range:active_power)"),
		L1_OUT_OF_RANGE_APPARENT_POWER(Doc.of(Level.WARNING).text("L1 Apparent Power is out of range."),
				"a_errors(out_of_range:apparent_power)"),
		L1_OUT_OF_RANGE_VOLTAGE(Doc.of(Level.WARNING).text("L1 Voltage is out of range."),
				"a_errors(out_of_range:voltage)"),
		L1_OUT_OF_RANGE_CURRENT(Doc.of(Level.WARNING).text("L1 Current is out of range."),
				"a_errors(out_of_range:current)"),

		L2_OUT_OF_RANGE_ACTIVE_POWER(Doc.of(Level.WARNING).text("L2 Active Power is out of range."),
				"b_errors(out_of_range:active_power)"),
		L2_OUT_OF_RANGE_APPARENT_POWER(Doc.of(Level.WARNING).text("L2 Apparent Power is out of range."),
				"b_errors(out_of_range:apparent_power)"),
		L2_OUT_OF_RANGE_VOLTAGE(Doc.of(Level.WARNING).text("L2 Voltage is out of range."),
				"b_errors(out_of_range:voltage)"),
		L2_OUT_OF_RANGE_CURRENT(Doc.of(Level.WARNING).text("L2 Current is out of range."),
				"b_errors(out_of_range:current)"),

		L3_OUT_OF_RANGE_ACTIVE_POWER(Doc.of(Level.WARNING).text("L3 Active Power is out of range."),
				"c_errors(out_of_range:active_power)"),
		L3_OUT_OF_RANGE_APPARENT_POWER(Doc.of(Level.WARNING).text("L3 Apparent Power is out of range."),
				"c_errors(out_of_range:apparent_power)"),
		L3_OUT_OF_RANGE_VOLTAGE(Doc.of(Level.WARNING).text("L3 Voltage is out of range."),
				"c_errors(out_of_range:voltage)"),
		L3_OUT_OF_RANGE_CURRENT(Doc.of(Level.WARNING).text("L3 Current is out of range."),
				"c_errors(out_of_range:current)");

		private final Doc doc;
		private final String shellyErrorCode;

		private ErrorChannelId(Doc doc, String shellyErrorCode) {
			this.doc = doc;
			this.shellyErrorCode = shellyErrorCode;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

		public String getShellyErrorCode() {
			return this.shellyErrorCode;
		}

		/**
		 * Returns error channel by shelly error code.
		 *
		 * @param shellyErrorCode Error code returned from shelly
		 * @return ErrorChannelId
		 */
		public static Optional<ErrorChannelId> byShellyErrorCode(String shellyErrorCode) {
			return Arrays.stream(values()).filter(x -> shellyErrorCode.equals(x.getShellyErrorCode())).findFirst();
		}

		/**
		 * Returns all ErrorChannelId's with provided shelly error codes.
		 *
		 * @return ErrorChannelId array
		 */
		public static ErrorChannelId[] valuesWithShellyErrorCode() {
			return Arrays.stream(values()).filter(x -> x.getShellyErrorCode() != null).toArray(ErrorChannelId[]::new);
		}
	}

}
